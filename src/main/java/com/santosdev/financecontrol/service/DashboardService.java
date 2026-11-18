package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.CategoriaTotal;
import com.santosdev.financecontrol.dto.DashboardResponse;
import com.santosdev.financecontrol.dto.MesTotal;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Service
public class DashboardService {

    /** Limite de meses devolvidos na evolucao mensal (os mais recentes), para o grafico continuar legivel. */
    private static final int MAXIMO_MESES = 24;

    private final TransacaoRepository transacoes;

    public DashboardService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser depois da data final");
        }

        List<CategoriaTotal> receitas = porCategoria(TipoTransacao.RECEITA, inicio, fim);
        List<CategoriaTotal> despesas = porCategoria(TipoTransacao.DESPESA, inicio, fim);
        BigDecimal totalReceitas = soma(receitas);
        BigDecimal totalDespesas = soma(despesas);

        return new DashboardResponse(totalReceitas, totalDespesas, totalReceitas.subtract(totalDespesas),
                receitas, despesas, evolucaoMensal(inicio, fim));
    }

    private List<CategoriaTotal> porCategoria(TipoTransacao tipo, LocalDate inicio, LocalDate fim) {
        return transacoes.totaisPorCategoria(tipo, inicio, fim).stream()
                .map(l -> new CategoriaTotal((Long) l[0], (String) l[1], (BigDecimal) l[2]))
                .toList();
    }

    private BigDecimal soma(List<CategoriaTotal> lista) {
        return lista.stream().map(CategoriaTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<MesTotal> evolucaoMensal(LocalDate inicio, LocalDate fim) {
        // [0] = receitas, [1] = despesas
        TreeMap<YearMonth, BigDecimal[]> porMes = new TreeMap<>();
        for (Object[] l : transacoes.totaisPorMes(inicio, fim)) {
            YearMonth mes = YearMonth.of(((Number) l[0]).intValue(), ((Number) l[1]).intValue());
            BigDecimal[] valores = porMes.computeIfAbsent(mes, m -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int posicao = l[2] == TipoTransacao.RECEITA ? 0 : 1;
            valores[posicao] = (BigDecimal) l[3];
        }

        // Preenche os meses sem movimento, para o grafico nao "pular" meses.
        YearMonth primeiro = inicio != null ? YearMonth.from(inicio) : (porMes.isEmpty() ? null : porMes.firstKey());
        YearMonth ultimo = fim != null ? YearMonth.from(fim) : (porMes.isEmpty() ? null : porMes.lastKey());
        if (primeiro == null || ultimo == null || ultimo.isBefore(primeiro)) {
            return List.of();
        }
        if (primeiro.plusMonths(MAXIMO_MESES - 1L).isBefore(ultimo)) {
            primeiro = ultimo.minusMonths(MAXIMO_MESES - 1L);
        }

        List<MesTotal> resultado = new ArrayList<>();
        for (YearMonth mes = primeiro; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            BigDecimal[] valores = porMes.getOrDefault(mes, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            resultado.add(new MesTotal(mes.toString(), valores[0], valores[1]));
        }
        return resultado;
    }
}
