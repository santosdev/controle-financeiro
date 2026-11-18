package com.santosdev.financecontrol.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        List<CategoriaTotal> receitasPorCategoria,
        List<CategoriaTotal> despesasPorCategoria,
        List<MesTotal> evolucaoMensal) {
}
