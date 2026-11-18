package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Size(max = 160, message = "E-mail muito longo")
        String email,
        @NotBlank(message = "A senha é obrigatória")
        @Size(max = 128, message = "Senha muito longa")
        String senha) {
}
