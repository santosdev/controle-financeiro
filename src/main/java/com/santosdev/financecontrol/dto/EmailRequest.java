package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        @Pattern(regexp = RegrasDeCampo.EMAIL, message = RegrasDeCampo.MENSAGEM_EMAIL)
        String email) {

    public EmailRequest {
        email = email == null ? null : email.trim();
    }
}
