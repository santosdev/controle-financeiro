package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.RegistroRequest;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.TipoToken;
import com.santosdev.financecontrol.model.TokenUsuario;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Regras de conta: cadastro, confirmacao de e-mail, bloqueio por tentativas, redefinicao de senha e Google. */
@Service
public class AuthService {

    static final int LIMITE_DE_FALHAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    static final Duration VALIDADE_CONFIRMACAO = Duration.ofHours(24);
    static final Duration VALIDADE_REDEFINICAO = Duration.ofHours(1);
    static final Duration INTERVALO_ENTRE_EMAILS = Duration.ofSeconds(60);

    private final UsuarioRepository usuarios;
    private final TokenService tokens;
    private final EmailService emails;
    private final PasswordEncoder encoder;
    private final CategoriasPadraoService categoriasPadrao;

    public AuthService(UsuarioRepository usuarios, TokenService tokens, EmailService emails,
                       PasswordEncoder encoder, CategoriasPadraoService categoriasPadrao) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.emails = emails;
        this.encoder = encoder;
        this.categoriasPadrao = categoriasPadrao;
    }

    public static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /* ---------------- Cadastro e confirmação de e-mail ---------------- */

    @Transactional
    public void registrar(RegistroRequest req) {
        String email = normalizar(req.email());
        String nome = req.nome().trim();
        Optional<Usuario> existente = usuarios.findByEmail(email);

        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.isEmailVerificado()) {
                String mensagem = usuario.getSenhaHash() == null
                        ? "Este e-mail já usa o login com o Google. Use o botão \"Continuar com o Google\"."
                        : "Este e-mail já está cadastrado. Entre na sua conta ou redefina a senha.";
                throw new ApiException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO", mensagem);
            }
            // Conta ainda nao confirmada: refazer o cadastro atualiza os dados e reenvia o e-mail.
            usuario.setNome(nome);
            usuario.setSenhaHash(encoder.encode(req.senha()));
            enviarConfirmacao(usuario);
            return;
        }

        Usuario novo = usuarios.save(new Usuario(nome, email, encoder.encode(req.senha()), ProvedorLogin.LOCAL, false));
        categoriasPadrao.criarPara(novo);
        enviarConfirmacao(novo);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void confirmarEmail(String valorDoToken) {
        TokenUsuario token = tokens.buscar(valorDoToken, TipoToken.VERIFICACAO_EMAIL)
                .orElseThrow(AuthService::linkInvalido);
        Usuario usuario = token.getUsuario();
        if (usuario.isEmailVerificado()) {
            return; // clicar duas vezes no mesmo link nao e erro
        }
        if (!token.estaValido(Instant.now())) {
            throw linkInvalido();
        }
        token.marcarComoUsado();
        usuario.setEmailVerificado(true);
    }

    /** Resposta sempre igual, exista o e-mail ou nao, para nao revelar quem tem conta. */
    @Transactional
    public void reenviarConfirmacao(String emailInformado) {
        usuarios.findByEmail(normalizar(emailInformado))
                .filter(u -> !u.isEmailVerificado())
                .filter(u -> !tokens.gerouRecentemente(u.getId(), TipoToken.VERIFICACAO_EMAIL, INTERVALO_ENTRE_EMAILS))
                .ifPresent(this::enviarConfirmacao);
    }

    private void enviarConfirmacao(Usuario usuario) {
        String valor = tokens.criar(usuario, TipoToken.VERIFICACAO_EMAIL, VALIDADE_CONFIRMACAO);
        emails.enviarConfirmacaoDeConta(usuario.getNome(), usuario.getEmail(), valor);
    }

    /* ---------------- Login e bloqueio por tentativas ---------------- */

    @Transactional(readOnly = true)
    public void verificarSeEstaBloqueado(String email) {
        usuarios.findByEmail(email).ifPresent(usuario -> {
            Instant agora = Instant.now();
            if (usuario.estaBloqueado(agora)) {
                long minutos = Math.max(1, Duration.between(agora, usuario.getBloqueadoAte()).toMinutes() + 1);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CONTA_BLOQUEADA",
                        "Muitas tentativas de login. Tente novamente em " + minutos
                                + " minuto(s) ou redefina sua senha.");
            }
        });
    }

    /** Transacao propria: precisa gravar a falha mesmo que a requisicao termine em erro. */
    @Transactional
    public void registrarFalhaDeLogin(String email) {
        usuarios.findByEmail(email).ifPresent(u -> u.registrarFalhaDeLogin(LIMITE_DE_FALHAS, BLOQUEIO));
    }

    /** Chamado depois que a senha foi conferida: zera as falhas e exige o e-mail confirmado. */
    @Transactional
    public Usuario concluirLogin(String email) {
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos"));
        usuario.zerarFalhasDeLogin();
        if (!usuario.isEmailVerificado()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NAO_VERIFICADO",
                    "Confirme seu e-mail para entrar. Enviamos um link para a sua caixa de entrada.");
        }
        return usuario;
    }

    /* ---------------- Esqueci minha senha ---------------- */

    /** Resposta sempre igual, exista o e-mail ou nao. */
    @Transactional
    public void solicitarRedefinicaoDeSenha(String emailInformado) {
        usuarios.findByEmail(normalizar(emailInformado))
                .filter(u -> !u.isVisitante()) // visitante nao tem e-mail real nem senha
                .filter(u -> !tokens.gerouRecentemente(u.getId(), TipoToken.REDEFINICAO_SENHA, INTERVALO_ENTRE_EMAILS))
                .ifPresent(usuario -> {
                    String valor = tokens.criar(usuario, TipoToken.REDEFINICAO_SENHA, VALIDADE_REDEFINICAO);
                    emails.enviarRedefinicaoDeSenha(usuario.getNome(), usuario.getEmail(), valor);
                });
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void redefinirSenha(String valorDoToken, String novaSenha) {
        TokenUsuario token = tokens.buscar(valorDoToken, TipoToken.REDEFINICAO_SENHA)
                .filter(t -> t.estaValido(Instant.now()))
                .orElseThrow(AuthService::linkInvalido);
        Usuario usuario = token.getUsuario();
        usuario.setSenhaHash(encoder.encode(novaSenha));
        usuario.setEmailVerificado(true); // quem recebeu o e-mail de redefinicao provou ser o dono do endereco
        usuario.zerarFalhasDeLogin();
        token.marcarComoUsado();
    }

    /* ---------------- Entrar com o Google ---------------- */

    /** Acha ou cria a conta de quem entrou pelo Google. O Google ja confirmou o e-mail. */
    @Transactional
    public Usuario entrarComGoogle(String emailDoGoogle, String nomeDoGoogle) {
        String email = normalizar(emailDoGoogle);
        Optional<Usuario> existente = usuarios.findByEmail(email);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            usuario.setEmailVerificado(true);
            usuario.zerarFalhasDeLogin();
            return usuario;
        }
        String nome = (nomeDoGoogle == null || nomeDoGoogle.isBlank()) ? email.substring(0, email.indexOf('@')) : nomeDoGoogle.trim();
        Usuario novo = usuarios.save(new Usuario(nome, email, null, ProvedorLogin.GOOGLE, true));
        categoriasPadrao.criarPara(novo);
        return novo;
    }

    private static ApiException linkInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST, "LINK_INVALIDO",
                "Este link é inválido, expirou ou já foi usado. Solicite um novo.");
    }
}
