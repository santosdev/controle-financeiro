package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitanteServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-18T12:00:00Z");

    private UsuarioRepository usuarios;
    private CategoriasPadraoService categoriasPadrao;
    private PerfilService perfil;
    private MutableClock relogio;
    private VisitanteService service;

    /** Relogio que o teste consegue avancar. */
    static class MutableClock extends Clock {
        Instant agora = AGORA;
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        categoriasPadrao = mock(CategoriasPadraoService.class);
        perfil = mock(PerfilService.class);
        relogio = new MutableClock();
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        service = new VisitanteService(usuarios, categoriasPadrao, perfil, true, relogio);
    }

    @Test
    void criaContaConfirmadaSemSenhaComCategoriasPadrao() {
        Usuario visitante = service.criar("1.2.3.4");

        assertThat(visitante.getProvedor()).isEqualTo(ProvedorLogin.VISITANTE);
        assertThat(visitante.isVisitante()).isTrue();
        assertThat(visitante.getSenhaHash()).isNull();
        assertThat(visitante.isEmailVerificado()).isTrue();
        assertThat(visitante.getNome()).isEqualTo("Visitante");
        assertThat(visitante.getEmail()).startsWith("visitante-").endsWith("@visitante.invalid");
        verify(categoriasPadrao).criarPara(visitante);
    }

    @Test
    void cadaVisitanteGanhaUmaContaDiferente() {
        assertThat(service.criar("1.2.3.4").getEmail()).isNotEqualTo(service.criar("1.2.3.4").getEmail());
    }

    @Test
    void limitaContasPorIpEmUmaHora() {
        for (int i = 0; i < VisitanteService.LIMITE_POR_IP; i++) {
            service.criar("1.2.3.4");
        }
        assertThatThrownBy(() -> service.criar("1.2.3.4"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("MUITOS_VISITANTES");
                });

        // outro endereco nao e afetado
        service.criar("5.6.7.8");

        // passada a janela, o mesmo endereco pode entrar de novo
        relogio.agora = AGORA.plus(VisitanteService.JANELA_DO_LIMITE).plusSeconds(1);
        service.criar("1.2.3.4");
    }

    @Test
    void recusaQuandoHaVisitantesDemais() {
        when(usuarios.countByProvedor(ProvedorLogin.VISITANTE)).thenReturn(VisitanteService.MAXIMO_DE_VISITANTES);

        assertThatThrownBy(() -> service.criar("1.2.3.4"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("VISITANTES_ESGOTADOS"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void desligadoNaoCriaConta() {
        VisitanteService desligado = new VisitanteService(usuarios, categoriasPadrao, perfil, false, relogio);

        assertThat(desligado.isHabilitado()).isFalse();
        assertThatThrownBy(() -> desligado.criar("1.2.3.4"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(usuarios, never()).save(any());
    }

    @Test
    void apagaSoVisitantesComMaisDe24Horas() {
        when(usuarios.idsCriadosAntesDe(ProvedorLogin.VISITANTE, AGORA.minus(Duration.ofHours(24))))
                .thenReturn(List.of(10L, 11L));

        service.apagarExpirados();

        verify(perfil).apagarTudoDoUsuario(10L);
        verify(perfil).apagarTudoDoUsuario(11L);
    }

    @Test
    void falhaAoApagarUmVisitanteNaoImpedeOsOutros() {
        when(usuarios.idsCriadosAntesDe(eq(ProvedorLogin.VISITANTE), any())).thenReturn(List.of(10L, 11L));
        doThrow(new RuntimeException("falhou")).when(perfil).apagarTudoDoUsuario(10L);

        service.apagarExpirados();

        verify(perfil).apagarTudoDoUsuario(11L);
    }

    @Test
    void sairApagaAContaDoVisitante() {
        service.apagar(42L);

        verify(perfil).apagarTudoDoUsuario(42L);
    }
}
