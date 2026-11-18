package com.santosdev.financecontrol.controller;

import com.santosdev.financecontrol.dto.EmailRequest;
import com.santosdev.financecontrol.dto.LoginRequest;
import com.santosdev.financecontrol.dto.MensagemResponse;
import com.santosdev.financecontrol.dto.RedefinirSenhaRequest;
import com.santosdev.financecontrol.dto.RegistroRequest;
import com.santosdev.financecontrol.dto.SessaoResponse;
import com.santosdev.financecontrol.dto.TokenRequest;
import com.santosdev.financecontrol.dto.UsuarioResponse;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.security.SessaoUsuario;
import com.santosdev.financecontrol.security.UsuarioLogado;
import com.santosdev.financecontrol.service.AuthService;
import com.santosdev.financecontrol.service.PerfilService;
import com.santosdev.financecontrol.service.VisitanteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final PerfilService perfil;
    private final AuthenticationManager gerenteDeAutenticacao;
    private final SessaoUsuario sessao;
    private final ObjectProvider<ClientRegistrationRepository> clientesOAuth;
    private final VisitanteService visitantes;

    public AuthController(AuthService auth, PerfilService perfil, AuthenticationManager gerenteDeAutenticacao,
                          SessaoUsuario sessao, ObjectProvider<ClientRegistrationRepository> clientesOAuth,
                          VisitanteService visitantes) {
        this.visitantes = visitantes;
        this.auth = auth;
        this.perfil = perfil;
        this.gerenteDeAutenticacao = gerenteDeAutenticacao;
        this.sessao = sessao;
        this.clientesOAuth = clientesOAuth;
    }

    /** O frontend usa para decidir se mostra os botoes "Continuar com o Google" e "Entrar como visitante". */
    @GetMapping("/config")
    public Map<String, Boolean> configuracao() {
        return Map.of("google", clientesOAuth.getIfAvailable() != null,
                "visitante", visitantes.isHabilitado());
    }

    /** Cria uma conta temporaria (sem e-mail e sem senha) e ja entra com ela. */
    @PostMapping("/visitante")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse entrarComoVisitante(HttpServletRequest request, HttpServletResponse response) {
        Usuario visitante = visitantes.criar(request.getRemoteAddr());
        UsuarioLogado logado = new UsuarioLogado(visitante);
        sessao.iniciar(UsernamePasswordAuthenticationToken.authenticated(logado, null, logado.getAuthorities()),
                request, response);
        return perfil.resposta(visitante);
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse cadastrar(@Valid @RequestBody RegistroRequest req) {
        auth.registrar(req);
        return new MensagemResponse("Conta criada! Enviamos um link de confirmação para " + AuthService.normalizar(req.email())
                + ". Abra o e-mail para ativar a conta.");
    }

    @PostMapping("/verificar-email")
    public MensagemResponse verificarEmail(@Valid @RequestBody TokenRequest req) {
        auth.confirmarEmail(req.token());
        return new MensagemResponse("E-mail confirmado! Agora você já pode entrar.");
    }

    @PostMapping("/reenviar-confirmacao")
    public MensagemResponse reenviarConfirmacao(@Valid @RequestBody EmailRequest req) {
        auth.reenviarConfirmacao(req.email());
        return new MensagemResponse("Se existir uma conta pendente de confirmação para esse e-mail, enviamos um novo link.");
    }

    @PostMapping("/login")
    public UsuarioResponse entrar(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String email = AuthService.normalizar(req.email());
        auth.verificarSeEstaBloqueado(email);

        Authentication autenticacao;
        try {
            autenticacao = gerenteDeAutenticacao.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, req.senha()));
        } catch (AuthenticationException e) {
            auth.registrarFalhaDeLogin(email);
            // Mesma mensagem para "e-mail inexistente" e "senha errada".
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos");
        }

        Usuario usuario = auth.concluirLogin(email); // exige e-mail confirmado
        sessao.iniciar(autenticacao, request, response);
        return perfil.resposta(usuario);
    }

    /** Os dados vem do banco (e nao da sessao), para o apelido e a foto novos aparecerem na hora. */
    @GetMapping("/sessao")
    public SessaoResponse sessao(@AuthenticationPrincipal UsuarioLogado usuario) {
        if (usuario == null) {
            return SessaoResponse.anonima();
        }
        return perfil.buscar(usuario.getId())
                .map(dados -> new SessaoResponse(true, dados))
                .orElseGet(SessaoResponse::anonima);
    }

    @PostMapping("/esqueci-senha")
    public MensagemResponse esqueciSenha(@Valid @RequestBody EmailRequest req) {
        auth.solicitarRedefinicaoDeSenha(req.email());
        return new MensagemResponse("Se existir uma conta com esse e-mail, enviamos um link para redefinir a senha. O link vale por 1 hora.");
    }

    @PostMapping("/redefinir-senha")
    public MensagemResponse redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        auth.redefinirSenha(req.token(), req.novaSenha());
        return new MensagemResponse("Senha alterada com sucesso! Entre com a nova senha.");
    }
}
