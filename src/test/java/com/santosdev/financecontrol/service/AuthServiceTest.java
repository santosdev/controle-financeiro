package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.RegistroRequest;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.TipoToken;
import com.santosdev.financecontrol.model.TokenUsuario;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private TokenService tokens;
    @Mock
    private EmailService emails;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private CategoriasPadraoService categoriasPadrao;

    @InjectMocks
    private AuthService service;

    private static Usuario usuario(boolean verificado, String senhaHash, ProvedorLogin provedor) {
        return new Usuario("Ana Souza", "ana@email.com", senhaHash, provedor, verificado);
    }

    private static void assertErro(Runnable acao, HttpStatus status, String codigo) {
        assertThatThrownBy(acao::run)
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    assertThat(e.getCodigo()).isEqualTo(codigo);
                });
    }

    /* ---------------- normalizar ---------------- */

    @Test
    void normalizarTiraEspacosEUsaMinusculas() {
        assertThat(AuthService.normalizar("  Ana@Email.COM ")).isEqualTo("ana@email.com");
    }

    /* ---------------- cadastro ---------------- */

    @Test
    void registrarCriaContaNaoVerificadaComCategoriasEEnviaEmail() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.empty());
        when(encoder.encode("Senha1234")).thenReturn("HASH");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokens.criar(any(), eq(TipoToken.VERIFICACAO_EMAIL), any())).thenReturn("token-do-link");

        service.registrar(new RegistroRequest("Ana Souza", "Ana@Email.com", "Senha1234"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("ana@email.com");
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("HASH");
        assertThat(salvo.getValue().isEmailVerificado()).isFalse();
        assertThat(salvo.getValue().getProvedor()).isEqualTo(ProvedorLogin.LOCAL);
        verify(categoriasPadrao).criarPara(salvo.getValue());
        verify(emails).enviarConfirmacaoDeConta("Ana Souza", "ana@email.com", "token-do-link");
    }

    @Test
    void registrarRecusaEmailJaCadastradoEConfirmado() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(true, "HASH", ProvedorLogin.LOCAL)));

        assertErro(() -> service.registrar(new RegistroRequest("Ana", "ana@email.com", "Senha1234")),
                HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO");

        verify(usuarios, never()).save(any());
        verifyNoInteractions(emails);
    }

    @Test
    void registrarAvisaQuandoOEmailJaUsaOGoogle() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(true, null, ProvedorLogin.GOOGLE)));

        assertThatThrownBy(() -> service.registrar(new RegistroRequest("Ana", "ana@email.com", "Senha1234")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("Google"));
    }

    @Test
    void registrarContaNaoConfirmadaAtualizaDadosEReenviaEmail() {
        Usuario pendente = usuario(false, "HASH-ANTIGO", ProvedorLogin.LOCAL);
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(pendente));
        when(encoder.encode("NovaSenha1")).thenReturn("HASH-NOVO");
        when(tokens.criar(any(), eq(TipoToken.VERIFICACAO_EMAIL), any())).thenReturn("novo-token");

        service.registrar(new RegistroRequest("Ana Nova", "ana@email.com", "NovaSenha1"));

        assertThat(pendente.getNome()).isEqualTo("Ana Nova");
        assertThat(pendente.getSenhaHash()).isEqualTo("HASH-NOVO");
        verify(usuarios, never()).save(any());
        verify(emails).enviarConfirmacaoDeConta("Ana Nova", "ana@email.com", "novo-token");
    }

    /* ---------------- confirmacao de e-mail ---------------- */

    @Test
    void confirmarEmailAtivaAContaEGastaOToken() {
        Usuario pendente = usuario(false, "HASH", ProvedorLogin.LOCAL);
        TokenUsuario token = new TokenUsuario(pendente, TipoToken.VERIFICACAO_EMAIL, "h", Instant.now().plusSeconds(600));
        when(tokens.buscar("valor", TipoToken.VERIFICACAO_EMAIL)).thenReturn(Optional.of(token));

        service.confirmarEmail("valor");

        assertThat(pendente.isEmailVerificado()).isTrue();
        assertThat(token.estaValido(Instant.now())).isFalse();
    }

    @Test
    void confirmarEmailRecusaTokenInexistente() {
        when(tokens.buscar("invalido", TipoToken.VERIFICACAO_EMAIL)).thenReturn(Optional.empty());

        assertErro(() -> service.confirmarEmail("invalido"), HttpStatus.BAD_REQUEST, "LINK_INVALIDO");
    }

    @Test
    void confirmarEmailRecusaTokenExpirado() {
        Usuario pendente = usuario(false, "HASH", ProvedorLogin.LOCAL);
        TokenUsuario expirado = new TokenUsuario(pendente, TipoToken.VERIFICACAO_EMAIL, "h", Instant.now().minusSeconds(10));
        when(tokens.buscar("valor", TipoToken.VERIFICACAO_EMAIL)).thenReturn(Optional.of(expirado));

        assertErro(() -> service.confirmarEmail("valor"), HttpStatus.BAD_REQUEST, "LINK_INVALIDO");
        assertThat(pendente.isEmailVerificado()).isFalse();
    }

    @Test
    void clicarDuasVezesNoLinkDeConfirmacaoNaoEErro() {
        Usuario jaConfirmado = usuario(true, "HASH", ProvedorLogin.LOCAL);
        TokenUsuario usado = new TokenUsuario(jaConfirmado, TipoToken.VERIFICACAO_EMAIL, "h", Instant.now().plusSeconds(600));
        usado.marcarComoUsado();
        when(tokens.buscar("valor", TipoToken.VERIFICACAO_EMAIL)).thenReturn(Optional.of(usado));

        service.confirmarEmail("valor");

        assertThat(jaConfirmado.isEmailVerificado()).isTrue();
    }

    /* ---------------- reenvio e redefinicao ---------------- */

    @Test
    void reenviarConfirmacaoNaoFazNadaParaEmailDesconhecido() {
        when(usuarios.findByEmail("x@email.com")).thenReturn(Optional.empty());

        service.reenviarConfirmacao("x@email.com");

        verifyNoInteractions(emails);
    }

    @Test
    void reenviarConfirmacaoNaoFazNadaParaContaJaConfirmada() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(true, "HASH", ProvedorLogin.LOCAL)));

        service.reenviarConfirmacao("ana@email.com");

        verifyNoInteractions(emails);
    }

    @Test
    void reenviarConfirmacaoRespeitaOIntervaloEntreEmails() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(false, "HASH", ProvedorLogin.LOCAL)));
        when(tokens.gerouRecentemente(any(), eq(TipoToken.VERIFICACAO_EMAIL), eq(AuthService.INTERVALO_ENTRE_EMAILS)))
                .thenReturn(true);

        service.reenviarConfirmacao("ana@email.com");

        verifyNoInteractions(emails);
    }

    @Test
    void reenviarConfirmacaoEnviaQuandoPermitido() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(false, "HASH", ProvedorLogin.LOCAL)));
        when(tokens.gerouRecentemente(any(), eq(TipoToken.VERIFICACAO_EMAIL), any())).thenReturn(false);
        when(tokens.criar(any(), eq(TipoToken.VERIFICACAO_EMAIL), any())).thenReturn("t");

        service.reenviarConfirmacao("ana@email.com");

        verify(emails).enviarConfirmacaoDeConta("Ana Souza", "ana@email.com", "t");
    }

    @Test
    void solicitarRedefinicaoNaoRevelaSeOEmailExiste() {
        when(usuarios.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        service.solicitarRedefinicaoDeSenha("naoexiste@email.com");

        verifyNoInteractions(emails);
    }

    @Test
    void contaDeVisitanteNaoRecebeEmailDeRedefinicao() {
        when(usuarios.findByEmail("visitante-1@visitante.invalid"))
                .thenReturn(Optional.of(usuario(true, null, ProvedorLogin.VISITANTE)));

        service.solicitarRedefinicaoDeSenha("visitante-1@visitante.invalid");

        verifyNoInteractions(emails);
        verify(tokens, never()).criar(any(), any(), any());
    }

    @Test
    void solicitarRedefinicaoEnviaEmailComLinkValidoPorUmaHora() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(true, "HASH", ProvedorLogin.LOCAL)));
        when(tokens.gerouRecentemente(any(), eq(TipoToken.REDEFINICAO_SENHA), any())).thenReturn(false);
        when(tokens.criar(any(), eq(TipoToken.REDEFINICAO_SENHA), eq(Duration.ofHours(1)))).thenReturn("tk");

        service.solicitarRedefinicaoDeSenha("ANA@email.com");

        verify(emails).enviarRedefinicaoDeSenha("Ana Souza", "ana@email.com", "tk");
    }

    @Test
    void redefinirSenhaTrocaASenhaDesbloqueiaEConfirmaOEmail() {
        Usuario conta = usuario(false, "HASH-ANTIGO", ProvedorLogin.LOCAL);
        for (int i = 0; i < 5; i++) {
            conta.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        }
        TokenUsuario token = new TokenUsuario(conta, TipoToken.REDEFINICAO_SENHA, "h", Instant.now().plusSeconds(600));
        when(tokens.buscar("valor", TipoToken.REDEFINICAO_SENHA)).thenReturn(Optional.of(token));
        when(encoder.encode("NovaSenha9")).thenReturn("HASH-NOVO");

        service.redefinirSenha("valor", "NovaSenha9");

        assertThat(conta.getSenhaHash()).isEqualTo("HASH-NOVO");
        assertThat(conta.isEmailVerificado()).isTrue();
        assertThat(conta.estaBloqueado(Instant.now())).isFalse();
        assertThat(token.estaValido(Instant.now())).isFalse();
    }

    @Test
    void redefinirSenhaRecusaTokenJaUsado() {
        Usuario conta = usuario(true, "HASH", ProvedorLogin.LOCAL);
        TokenUsuario usado = new TokenUsuario(conta, TipoToken.REDEFINICAO_SENHA, "h", Instant.now().plusSeconds(600));
        usado.marcarComoUsado();
        when(tokens.buscar("valor", TipoToken.REDEFINICAO_SENHA)).thenReturn(Optional.of(usado));

        assertErro(() -> service.redefinirSenha("valor", "NovaSenha9"), HttpStatus.BAD_REQUEST, "LINK_INVALIDO");

        verifyNoInteractions(encoder);
        assertThat(conta.getSenhaHash()).isEqualTo("HASH");
    }

    /* ---------------- login e bloqueio ---------------- */

    @Test
    void concluirLoginExigeEmailConfirmado() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(false, "HASH", ProvedorLogin.LOCAL)));

        assertErro(() -> service.concluirLogin("ana@email.com"), HttpStatus.FORBIDDEN, "EMAIL_NAO_VERIFICADO");
    }

    @Test
    void concluirLoginDevolveOUsuarioEZeraAsFalhas() {
        Usuario conta = usuario(true, "HASH", ProvedorLogin.LOCAL);
        conta.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(conta));

        assertThat(service.concluirLogin("ana@email.com")).isSameAs(conta);
    }

    @Test
    void verificarSeEstaBloqueadoRecusaContaBloqueada() {
        Usuario conta = usuario(true, "HASH", ProvedorLogin.LOCAL);
        for (int i = 0; i < AuthService.LIMITE_DE_FALHAS; i++) {
            conta.registrarFalhaDeLogin(AuthService.LIMITE_DE_FALHAS, AuthService.BLOQUEIO);
        }
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(conta));

        assertErro(() -> service.verificarSeEstaBloqueado("ana@email.com"),
                HttpStatus.TOO_MANY_REQUESTS, "CONTA_BLOQUEADA");
    }

    @Test
    void verificarSeEstaBloqueadoPermiteContaLivreEEmailDesconhecido() {
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario(true, "HASH", ProvedorLogin.LOCAL)));
        when(usuarios.findByEmail("outro@email.com")).thenReturn(Optional.empty());

        service.verificarSeEstaBloqueado("ana@email.com");
        service.verificarSeEstaBloqueado("outro@email.com");
    }

    @Test
    void cincoSenhasErradasBloqueiamOLogin() {
        Usuario conta = usuario(true, "HASH", ProvedorLogin.LOCAL);
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(conta));

        for (int i = 0; i < AuthService.LIMITE_DE_FALHAS; i++) {
            service.registrarFalhaDeLogin("ana@email.com");
        }

        assertThat(conta.estaBloqueado(Instant.now())).isTrue();
    }

    /* ---------------- Google ---------------- */

    @Test
    void entrarComGoogleCriaContaVerificadaComCategorias() {
        when(usuarios.findByEmail("nova@gmail.com")).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario criado = service.entrarComGoogle("Nova@Gmail.com", "  Nova Pessoa ");

        assertThat(criado.getNome()).isEqualTo("Nova Pessoa");
        assertThat(criado.getEmail()).isEqualTo("nova@gmail.com");
        assertThat(criado.getProvedor()).isEqualTo(ProvedorLogin.GOOGLE);
        assertThat(criado.isEmailVerificado()).isTrue();
        assertThat(criado.getSenhaHash()).isNull();
        verify(categoriasPadrao).criarPara(criado);
    }

    @Test
    void entrarComGoogleSemNomeUsaAParteAntesDoArroba() {
        when(usuarios.findByEmail("fulano@gmail.com")).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.entrarComGoogle("fulano@gmail.com", " ").getNome()).isEqualTo("fulano");
    }

    @Test
    void entrarComGoogleEmContaExistenteConfirmaOEmailSemCriarOutra() {
        Usuario existente = usuario(false, "HASH", ProvedorLogin.LOCAL);
        when(usuarios.findByEmail("ana@email.com")).thenReturn(Optional.of(existente));

        Usuario resultado = service.entrarComGoogle("ana@email.com", "Ana");

        assertThat(resultado).isSameAs(existente);
        assertThat(existente.isEmailVerificado()).isTrue();
        verify(usuarios, never()).save(any());
        verifyNoInteractions(categoriasPadrao);
    }
}
