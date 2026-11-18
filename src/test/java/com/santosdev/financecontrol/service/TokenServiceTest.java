package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.TipoToken;
import com.santosdev.financecontrol.model.TokenUsuario;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.TokenUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenUsuarioRepository repositorio;

    @InjectMocks
    private TokenService service;

    private final Usuario usuario = new Usuario("Ana", "ana@email.com", "hash", ProvedorLogin.LOCAL, false);

    @Test
    void guardaSoOHashEDevolveOValorOriginal() {
        String valor = service.criar(usuario, TipoToken.VERIFICACAO_EMAIL, Duration.ofHours(24));

        ArgumentCaptor<TokenUsuario> capturado = ArgumentCaptor.forClass(TokenUsuario.class);
        verify(repositorio).save(capturado.capture());
        assertThat(valor).isNotBlank().hasSizeGreaterThan(30);
        assertThat(TokenService.hash(valor)).hasSize(64);
        assertThat(capturado.getValue().getTipo()).isEqualTo(TipoToken.VERIFICACAO_EMAIL);
    }

    @Test
    void cadaTokenGeradoEDiferente() {
        String primeiro = service.criar(usuario, TipoToken.REDEFINICAO_SENHA, Duration.ofHours(1));
        String segundo = service.criar(usuario, TipoToken.REDEFINICAO_SENHA, Duration.ofHours(1));

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    void hashEEstavelEDiferenteDoValor() {
        assertThat(TokenService.hash("abc")).isEqualTo(TokenService.hash("abc"));
        assertThat(TokenService.hash("abc")).isNotEqualTo("abc");
        assertThat(TokenService.hash("abc")).isNotEqualTo(TokenService.hash("abd"));
    }

    @Test
    void buscarIgnoraValorVazioOuGrandeDemais() {
        assertThat(service.buscar(null, TipoToken.VERIFICACAO_EMAIL)).isEmpty();
        assertThat(service.buscar("   ", TipoToken.VERIFICACAO_EMAIL)).isEmpty();
        assertThat(service.buscar("x".repeat(101), TipoToken.VERIFICACAO_EMAIL)).isEmpty();

        verifyNoInteractions(repositorio);
    }

    @Test
    void buscarConsultaPeloHashENaoPeloValor() {
        when(repositorio.buscarPorHash(anyString(), any())).thenReturn(Optional.empty());

        service.buscar("meu-token", TipoToken.VERIFICACAO_EMAIL);

        verify(repositorio).buscarPorHash(TokenService.hash("meu-token"), TipoToken.VERIFICACAO_EMAIL);
        verify(repositorio, never()).buscarPorHash("meu-token", TipoToken.VERIFICACAO_EMAIL);
    }

    @Test
    void gerouRecentementeEVerdadeiroDentroDaJanela() {
        TokenUsuario recente = new TokenUsuario(usuario, TipoToken.VERIFICACAO_EMAIL, "h", Instant.now().plusSeconds(3600));
        when(repositorio.findFirstByUsuarioIdAndTipoOrderByCriadoEmDesc(1L, TipoToken.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(recente));

        assertThat(service.gerouRecentemente(1L, TipoToken.VERIFICACAO_EMAIL, Duration.ofSeconds(60))).isTrue();
    }

    @Test
    void gerouRecentementeEFalsoSemTokenAnterior() {
        when(repositorio.findFirstByUsuarioIdAndTipoOrderByCriadoEmDesc(1L, TipoToken.VERIFICACAO_EMAIL))
                .thenReturn(Optional.empty());

        assertThat(service.gerouRecentemente(1L, TipoToken.VERIFICACAO_EMAIL, Duration.ofSeconds(60))).isFalse();
    }
}
