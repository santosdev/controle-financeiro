package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.DashboardResponse;
import com.santosdev.financecontrol.dto.MesTotal;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long USUARIO_ID = 7L;

    @Mock
    private TransacaoRepository transacoes;
    @Mock
    private UsuarioAtual usuarioAtual;

    @InjectMocks
    private DashboardService service;

    @BeforeEach
    void usuarioLogado() {
        lenient().when(usuarioAtual.id()).thenReturn(USUARIO_ID);
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void somaReceitasEDespesasECalculaOSaldo() {
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.RECEITA, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Salário", v("3000.00")}, new Object[]{2L, "Freelance", v("500.50")}));
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.DESPESA, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{3L, "Moradia", v("1200.00")}));
        when(transacoes.totaisPorMes(USUARIO_ID, null, null)).thenReturn(List.of());

        DashboardResponse resumo = service.resumo(null, null);

        assertThat(resumo.totalReceitas()).isEqualByComparingTo("3500.50");
        assertThat(resumo.totalDespesas()).isEqualByComparingTo("1200.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2300.50");
        assertThat(resumo.receitasPorCategoria()).hasSize(2);
        assertThat(resumo.despesasPorCategoria()).extracting("categoria").containsExactly("Moradia");
    }

    @Test
    void semMovimentacoesTudoFicaZeradoEVazio() {
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.RECEITA, null, null)).thenReturn(List.of());
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.DESPESA, null, null)).thenReturn(List.of());
        when(transacoes.totaisPorMes(USUARIO_ID, null, null)).thenReturn(List.of());

        DashboardResponse resumo = service.resumo(null, null);

        assertThat(resumo.saldo()).isEqualByComparingTo("0");
        assertThat(resumo.evolucaoMensal()).isEmpty();
    }

    @Test
    void saldoPodeSerNegativo() {
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.RECEITA, null, null)).thenReturn(List.of());
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.DESPESA, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{3L, "Moradia", v("100.00")}));
        when(transacoes.totaisPorMes(USUARIO_ID, null, null)).thenReturn(List.of());

        assertThat(service.resumo(null, null).saldo()).isEqualByComparingTo("-100.00");
    }

    @Test
    void evolucaoMensalPreencheOsMesesSemMovimento() {
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.RECEITA, null, null)).thenReturn(List.of());
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.DESPESA, null, null)).thenReturn(List.of());
        when(transacoes.totaisPorMes(USUARIO_ID, null, null)).thenReturn(List.<Object[]>of(
                new Object[]{2026, 9, TipoTransacao.RECEITA, v("1000.00")},
                new Object[]{2026, 9, TipoTransacao.DESPESA, v("400.00")},
                new Object[]{2026, 11, TipoTransacao.DESPESA, v("50.00")}));

        List<MesTotal> meses = service.resumo(null, null).evolucaoMensal();

        assertThat(meses).extracting(MesTotal::mes).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(meses.get(0).receitas()).isEqualByComparingTo("1000.00");
        assertThat(meses.get(0).despesas()).isEqualByComparingTo("400.00");
        assertThat(meses.get(1).receitas()).isEqualByComparingTo("0");
        assertThat(meses.get(1).despesas()).isEqualByComparingTo("0");
        assertThat(meses.get(2).despesas()).isEqualByComparingTo("50.00");
    }

    @Test
    void evolucaoMensalDevolveNoMaximo24MesesOsMaisRecentes() {
        LocalDate inicio = LocalDate.of(2020, 1, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.RECEITA, inicio, fim)).thenReturn(List.of());
        when(transacoes.totaisPorCategoria(USUARIO_ID, TipoTransacao.DESPESA, inicio, fim)).thenReturn(List.of());
        when(transacoes.totaisPorMes(USUARIO_ID, inicio, fim)).thenReturn(List.of());

        List<MesTotal> meses = service.resumo(inicio, fim).evolucaoMensal();

        assertThat(meses).hasSize(24);
        assertThat(meses.get(0).mes()).isEqualTo("2024-12");
        assertThat(meses.get(23).mes()).isEqualTo("2026-11");
    }

    @Test
    void recusaPeriodoComInicioDepoisDoFim() {
        assertThatThrownBy(() -> service.resumo(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(transacoes);
    }
}
