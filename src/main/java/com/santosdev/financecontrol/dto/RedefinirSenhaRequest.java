package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "Link inválido")
        @Size(max = 100, message = "Link inválido")
        String token,
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 64, message = RegrasDeCampo.MENSAGEM_SENHA)
        @Pattern(regexp = RegrasDeCampo.SENHA, message = RegrasDeCampo.MENSAGEM_SENHA)
        String novaSenha) {
}
