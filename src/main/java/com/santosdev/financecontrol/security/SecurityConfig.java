package com.santosdev.financecontrol.security;

import com.santosdev.financecontrol.service.VisitanteService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Politica de conteudo: so carrega scripts, estilos e imagens do proprio sistema. */
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http,
                                ObjectProvider<ClientRegistrationRepository> clientesOAuth,
                                GoogleLoginSuccessHandler loginGoogle,
                                VisitanteService visitantes) throws Exception {
        http
                // CSRF: o token vai num cookie legivel pelo JavaScript e volta no cabecalho X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()) // paginas estaticas; os dados ficam atras da API

                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, e) ->
                                responderErro(res, 401, "NAO_AUTENTICADO", "Você precisa entrar para continuar"))
                        .accessDeniedHandler((req, res, e) ->
                                responderErro(res, 403, "ACESSO_NEGADO", "Requisição não permitida. Atualize a página e tente de novo.")))

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        // Visitante nao tem senha para voltar depois: ao sair, a conta temporaria e apagada.
                        .addLogoutHandler((req, res, auth) -> {
                            if (auth != null && auth.getPrincipal() instanceof UsuarioLogado u && u.isVisitante()) {
                                visitantes.apagar(u.getId());
                            }
                        })
                        .deleteCookies("FINANCECONTROL_SESSION")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))

                .headers(cabecalhos -> cabecalhos
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()")));

        // "Continuar com o Google" so existe se as credenciais do Google foram informadas.
        if (clientesOAuth.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login.html")
                    .successHandler(loginGoogle)
                    .failureUrl("/login.html?erro=google"));
        }
        return http.build();
    }

    // As mensagens sao fixas e sem aspas, entao o JSON pode ser montado direto.
    private static void responderErro(HttpServletResponse resposta, int status, String codigo, String mensagem)
            throws java.io.IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"status\":" + status + ",\"codigo\":\"" + codigo + "\",\"erro\":\"" + mensagem + "\"}");
    }
}
