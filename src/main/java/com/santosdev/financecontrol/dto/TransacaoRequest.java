package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.model.TipoTransacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres")
        String descricao,
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais e 10 dígitos inteiros")
        BigDecimal valor,
        @NotNull(message = "A data é obrigatória")
        LocalDate data,
        @NotNull(message = "O tipo é obrigatório (RECEITA ou DESPESA)")
        TipoTransacao tipo,
        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId) {
}
