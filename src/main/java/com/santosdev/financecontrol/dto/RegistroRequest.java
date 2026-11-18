package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,
        @NotBlank(message = "O e-mail é obrigatório")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        @Pattern(regexp = RegrasDeCampo.EMAIL, message = RegrasDeCampo.MENSAGEM_EMAIL)
        String email,
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 64, message = RegrasDeCampo.MENSAGEM_SENHA)
        @Pattern(regexp = RegrasDeCampo.SENHA, message = RegrasDeCampo.MENSAGEM_SENHA)
        String senha) {

    // Espacos nas pontas do nome e do e-mail sao ignorados (a senha nunca e alterada).
    public RegistroRequest {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim();
    }
}
