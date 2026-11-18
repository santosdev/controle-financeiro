package com.santosdev.financecontrol.dto;

import java.math.BigDecimal;

/** Total movimentado em uma categoria dentro do período consultado. */
public record CategoriaTotal(Long categoriaId, String categoria, BigDecimal total) {
}
