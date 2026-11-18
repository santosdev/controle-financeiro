package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank(message = "Informe a senha atual")
        @Size(max = 64, message = "Senha atual inválida")
        String senhaAtual,
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 64, message = RegrasDeCampo.MENSAGEM_SENHA)
        @Pattern(regexp = RegrasDeCampo.SENHA, message = RegrasDeCampo.MENSAGEM_SENHA)
        String novaSenha) {
}
