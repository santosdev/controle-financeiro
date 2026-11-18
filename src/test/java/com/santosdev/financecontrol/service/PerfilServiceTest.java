package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.AlterarSenhaRequest;
import com.santosdev.financecontrol.dto.ExcluirContaRequest;
import com.santosdev.financecontrol.dto.PerfilRequest;
import com.santosdev.financecontrol.dto.UsuarioResponse;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.FotoUsuario;
import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.FotoUsuarioRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Long ID = 7L;

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private FotoUsuarioRepository fotos;
    @Mock
    private TransacaoRepository transacoes;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private UsuarioAtual usuarioAtual;

    @InjectMocks
    private PerfilService service;

    private Usuario conta;

    @BeforeEach
    void contaLogada() {
        conta = contaComId("Fábio Martins", "fabio@email.com", "HASH", ProvedorLogin.LOCAL);
        lenient().when(usuarioAtual.id()).thenReturn(ID);
        lenient().when(usuarios.findById(ID)).thenReturn(Optional.of(conta));
        lenient().when(fotos.buscarAtualizadaEm(any())).thenReturn(Optional.empty());
    }

    // O banco e quem define o id; nos testes damos o id na mao.
    private static Usuario contaComId(String nome, String email, String senhaHash, ProvedorLogin provedor) {
        Usuario usuario = new Usuario(nome, email, senhaHash, provedor, true);
        ReflectionTestUtils.setField(usuario, "id", ID);
        return usuario;
    }

    private static byte[] pngValido() throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB), "png", saida);
        return saida.toByteArray();
    }

    /* ---------------- dados ---------------- */

    @Test
    void semApelidoMostraOPrimeiroNome() {
        assertThat(service.obter().nomeExibicao()).isEqualTo("Fábio");
    }

    @Test
    void atualizarGuardaApelidoENome() {
        UsuarioResponse resposta = service.atualizar(new PerfilRequest("Fábio Santos Martins", "Fabão"));

        assertThat(conta.getNome()).isEqualTo("Fábio Santos Martins");
        assertThat(conta.getApelido()).isEqualTo("Fabão");
        assertThat(resposta.nomeExibicao()).isEqualTo("Fabão");
    }

    @Test
    void apelidoVazioRemoveOApelido() {
        conta.setApelido("Fabão");

        service.atualizar(new PerfilRequest("Fábio Martins", "   "));

        assertThat(conta.getApelido()).isNull();
        assertThat(conta.nomeDeExibicao()).isEqualTo("Fábio");
    }

    @Test
    void respostaIndicaSeHaFotoESeHaSenha() {
        when(fotos.buscarAtualizadaEm(ID)).thenReturn(Optional.of(Instant.ofEpochMilli(1234567L)));

        UsuarioResponse resposta = service.obter();

        assertThat(resposta.fotoVersao()).isEqualTo(1234567L);
        assertThat(resposta.temSenha()).isTrue();
    }

    @Test
    void contaSoDoGoogleNaoTemSenha() {
        Usuario google = contaComId("Ana", "ana@gmail.com", null, ProvedorLogin.GOOGLE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(google));

        assertThat(service.obter().temSenha()).isFalse();
    }

    @Test
    void buscarDeContaInexistenteDevolveVazio() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscar(99L)).isEmpty();
    }

    /* ---------------- foto ---------------- */

    @Test
    void trocarFotoGuardaImagemProcessadaParaOUsuario() throws IOException {
        service.trocarFoto(pngValido());

        ArgumentCaptor<FotoUsuario> salva = ArgumentCaptor.forClass(FotoUsuario.class);
        verify(fotos).save(salva.capture());
        assertThat(salva.getValue().getUsuarioId()).isEqualTo(ID);
        assertThat(salva.getValue().getDados()[0] & 0xFF).isEqualTo(0xFF); // JPEG
    }

    @Test
    void trocarFotoRecusaArquivoInvalidoSemGravar() {
        assertThatThrownBy(() -> service.trocarFoto("texto".getBytes()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("FOTO_INVALIDA"));

        verify(fotos, never()).save(any());
    }

    @Test
    void removerFotoApagaSoADoUsuarioLogado() {
        service.removerFoto();

        verify(fotos).deleteById(ID);
    }

    @Test
    void fotoDoGoogleNaoSobrescreveUmaFotoExistente() throws IOException {
        when(fotos.existsById(ID)).thenReturn(true);

        service.definirFotoSeNaoTiver(ID, pngValido());

        verify(fotos, never()).save(any());
    }

    @Test
    void fotoDoGoogleEGuardadaQuandoNaoHaFoto() throws IOException {
        when(fotos.existsById(ID)).thenReturn(false);

        service.definirFotoSeNaoTiver(ID, pngValido());

        verify(fotos).save(any(FotoUsuario.class));
    }

    /* ---------------- senha ---------------- */

    @Test
    void alterarSenhaComSenhaAtualCorreta() {
        when(encoder.matches("Antiga123", "HASH")).thenReturn(true);
        when(encoder.encode("Nova12345")).thenReturn("HASH-NOVO");

        service.alterarSenha(new AlterarSenhaRequest("Antiga123", "Nova12345"));

        assertThat(conta.getSenhaHash()).isEqualTo("HASH-NOVO");
    }

    @Test
    void alterarSenhaRecusaSenhaAtualErradaSemTrocar() {
        when(encoder.matches("Errada123", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.alterarSenha(new AlterarSenhaRequest("Errada123", "Nova12345")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("SENHA_ATUAL_INCORRETA");
                    // 400 (e nao 401): um 401 faria o frontend achar que a sessao expirou e deslogar.
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        assertThat(conta.getSenhaHash()).isEqualTo("HASH");
        verify(encoder, never()).encode(any());
    }

    @Test
    void cincoSenhasAtuaisErradasBloqueiamAConta() {
        when(encoder.matches(any(), any())).thenReturn(false);

        for (int i = 0; i < AuthService.LIMITE_DE_FALHAS; i++) {
            assertThatThrownBy(() -> service.alterarSenha(new AlterarSenhaRequest("Errada123", "Nova12345")))
                    .isInstanceOf(ApiException.class);
        }

        assertThat(conta.estaBloqueado(Instant.now())).isTrue();
        assertThatThrownBy(() -> service.alterarSenha(new AlterarSenhaRequest("Errada123", "Nova12345")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("CONTA_BLOQUEADA"));
    }

    @Test
    void contaDoGoogleNaoAlteraSenha() {
        Usuario google = contaComId("Ana", "ana@gmail.com", null, ProvedorLogin.GOOGLE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(google));

        assertThatThrownBy(() -> service.alterarSenha(new AlterarSenhaRequest("x", "Nova12345")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("SEM_SENHA"));

        verifyNoInteractions(encoder);
    }

    /* ---------------- excluir conta ---------------- */

    @Test
    void excluirContaApagaMovimentacoesDepoisCategoriasDepoisAConta() {
        when(encoder.matches("Senha123", "HASH")).thenReturn(true);

        service.excluirConta(new ExcluirContaRequest("Senha123"));

        InOrder ordem = inOrder(transacoes, categorias, usuarios);
        ordem.verify(transacoes).apagarDoUsuario(ID);
        ordem.verify(categorias).apagarDoUsuario(ID);
        ordem.verify(usuarios).deleteById(ID);
    }

    @Test
    void excluirContaComSenhaErradaNaoApagaNada() {
        when(encoder.matches("Errada123", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.excluirConta(new ExcluirContaRequest("Errada123")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("CONFIRMACAO_INCORRETA"));

        verifyNoInteractions(transacoes, categorias);
        verify(usuarios, never()).deleteById(any());
    }

    @Test
    void visitanteConfirmaAExclusaoDigitandoExcluir() {
        Usuario visitante = contaComId("Visitante", "visitante-1@visitante.invalid", null, ProvedorLogin.VISITANTE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(visitante));

        service.excluirConta(new ExcluirContaRequest(" excluir "));

        verify(usuarios).deleteById(ID);
    }

    @Test
    void visitanteQueNaoDigitaExcluirNaoApagaNada() {
        Usuario visitante = contaComId("Visitante", "visitante-1@visitante.invalid", null, ProvedorLogin.VISITANTE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(visitante));

        assertThatThrownBy(() -> service.excluirConta(new ExcluirContaRequest("visitante-1@visitante.invalid")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("CONFIRMACAO_INCORRETA"));
        verify(usuarios, never()).deleteById(any());
    }

    @Test
    void visitanteNaoTemSenhaParaAlterar() {
        Usuario visitante = contaComId("Visitante", "visitante-1@visitante.invalid", null, ProvedorLogin.VISITANTE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(visitante));

        assertThatThrownBy(() -> service.alterarSenha(new AlterarSenhaRequest("x", "NovaSenha1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("VISITANTE"));
        verifyNoInteractions(encoder);
    }

    @Test
    void respostaIndicaContaDeVisitante() {
        Usuario visitante = contaComId("Visitante", "visitante-1@visitante.invalid", null, ProvedorLogin.VISITANTE);

        assertThat(service.resposta(visitante).visitante()).isTrue();
        assertThat(service.resposta(conta).visitante()).isFalse();
    }

    @Test
    void contaDoGoogleConfirmaDigitandoOEmail() {
        Usuario google = contaComId("Ana", "ana@gmail.com", null, ProvedorLogin.GOOGLE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(google));

        service.excluirConta(new ExcluirContaRequest("  ANA@gmail.com "));

        verify(usuarios).deleteById(ID);
    }

    @Test
    void contaDoGoogleComEmailErradoNaoApagaNada() {
        Usuario google = contaComId("Ana", "ana@gmail.com", null, ProvedorLogin.GOOGLE);
        when(usuarios.findById(ID)).thenReturn(Optional.of(google));

        assertThatThrownBy(() -> service.excluirConta(new ExcluirContaRequest("outro@gmail.com")))
                .isInstanceOf(ApiException.class);

        verify(usuarios, never()).deleteById(any());
    }
}
