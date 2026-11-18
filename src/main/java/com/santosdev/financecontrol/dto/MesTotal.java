package com.santosdev.financecontrol.dto;

import java.math.BigDecimal;

/** Receitas e despesas de um mês. O campo "mes" segue o formato AAAA-MM (ex.: 2026-11). */
public record MesTotal(String mes, BigDecimal receitas, BigDecimal despesas) {
}
