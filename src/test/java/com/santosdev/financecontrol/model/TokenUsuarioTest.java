package com.santosdev.financecontrol.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUsuarioTest {

    private final Usuario usuario = new Usuario("Ana", "ana@email.com", "hash", ProvedorLogin.LOCAL, false);

    private TokenUsuario tokenQueExpiraEm(Duration validade) {
        return new TokenUsuario(usuario, TipoToken.VERIFICACAO_EMAIL, "hash-do-token", Instant.now().plus(validade));
    }

    @Test
    void tokenNovoEValido() {
        assertThat(tokenQueExpiraEm(Duration.ofHours(1)).estaValido(Instant.now())).isTrue();
    }

    @Test
    void tokenExpiradoNaoEValido() {
        TokenUsuario token = tokenQueExpiraEm(Duration.ofHours(1));

        assertThat(token.estaValido(Instant.now().plus(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void tokenJaUsadoNaoEValido() {
        TokenUsuario token = tokenQueExpiraEm(Duration.ofHours(1));

        token.marcarComoUsado();

        assertThat(token.estaValido(Instant.now())).isFalse();
    }
}
