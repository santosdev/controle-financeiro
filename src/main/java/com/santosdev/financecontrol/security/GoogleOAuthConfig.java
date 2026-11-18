package com.santosdev.financecontrol.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registra o Google como provedor de login, mas somente se GOOGLE_CLIENT_ID estiver definido.
 * Sem ele o sistema funciona normalmente, so que sem o botao "Continuar com o Google".
 */
@Configuration
@ConditionalOnExpression("!'${app.google.client-id:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret:}") String clientSecret) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
