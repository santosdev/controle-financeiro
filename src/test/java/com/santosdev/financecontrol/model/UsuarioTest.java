package com.santosdev.financecontrol.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private Usuario novoUsuario() {
        return new Usuario("Ana", "ana@email.com", "hash", ProvedorLogin.LOCAL, true);
    }

    @Test
    void naoBloqueiaAntesDoLimiteDeFalhas() {
        Usuario usuario = novoUsuario();

        for (int i = 0; i < 4; i++) {
            usuario.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        }

        assertThat(usuario.estaBloqueado(Instant.now())).isFalse();
    }

    @Test
    void bloqueiaAoAtingirOLimiteDeFalhas() {
        Usuario usuario = novoUsuario();

        for (int i = 0; i < 5; i++) {
            usuario.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        }

        assertThat(usuario.estaBloqueado(Instant.now())).isTrue();
        assertThat(usuario.getBloqueadoAte()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
    }

    @Test
    void bloqueioExpiraDepoisDoTempo() {
        Usuario usuario = novoUsuario();
        for (int i = 0; i < 5; i++) {
            usuario.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        }

        Instant depoisDoBloqueio = Instant.now().plus(Duration.ofMinutes(16));

        assertThat(usuario.estaBloqueado(depoisDoBloqueio)).isFalse();
    }

    @Test
    void zerarFalhasRemoveOBloqueio() {
        Usuario usuario = novoUsuario();
        for (int i = 0; i < 5; i++) {
            usuario.registrarFalhaDeLogin(5, Duration.ofMinutes(15));
        }

        usuario.zerarFalhasDeLogin();

        assertThat(usuario.estaBloqueado(Instant.now())).isFalse();
        assertThat(usuario.getBloqueadoAte()).isNull();
    }

    @Test
    void contaNovaNaoComecaBloqueada() {
        assertThat(novoUsuario().estaBloqueado(Instant.now())).isFalse();
    }
}
