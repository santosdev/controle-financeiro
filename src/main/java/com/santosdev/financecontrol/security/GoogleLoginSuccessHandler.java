package com.santosdev.financecontrol.security;

import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Depois que o Google confirma quem e a pessoa, acha ou cria a conta no FinanceControl
 * e troca a autenticacao do Google pela do sistema (mesmo tipo usado no login com senha).
 */
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final SessaoUsuario sessao;

    public GoogleLoginSuccessHandler(AuthService authService, SessaoUsuario sessao) {
        this.authService = authService;
        this.sessao = sessao;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication autenticacao) throws IOException {
        if (!(autenticacao.getPrincipal() instanceof OidcUser google)
                || !Boolean.TRUE.equals(google.getEmailVerified())
                || google.getEmail() == null) {
            response.sendRedirect("/login.html?erro=google-email");
            return;
        }

        Usuario usuario = authService.entrarComGoogle(google.getEmail(), google.getFullName());
        UsuarioLogado logado = new UsuarioLogado(usuario);
        sessao.iniciar(UsernamePasswordAuthenticationToken.authenticated(logado, null, logado.getAuthorities()),
                request, response);
        response.sendRedirect("/");
    }
}
