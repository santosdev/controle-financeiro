package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(
        @NotBlank(message = "Link inválido")
        @Size(max = 100, message = "Link inválido")
        String token) {

    public TokenRequest {
        token = token == null ? null : token.trim();
    }
}
