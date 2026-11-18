package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,
        @Size(max = 40, message = "O apelido deve ter no máximo 40 caracteres")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "O apelido não pode ter caracteres de controle")
        String apelido) {

    // Espacos nas pontas sao ignorados; apelido vazio significa "sem apelido".
    public PerfilRequest {
        nome = nome == null ? null : nome.trim();
        apelido = apelido == null ? null : apelido.trim();
    }
}
