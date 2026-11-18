package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaDTO(
        Long id,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres")
        String nome,
        @NotNull(message = "O tipo é obrigatório (RECEITA ou DESPESA)")
        TipoTransacao tipo) {

    public static CategoriaDTO de(Categoria c) {
        return new CategoriaDTO(c.getId(), c.getNome(), c.getTipo());
    }
}
