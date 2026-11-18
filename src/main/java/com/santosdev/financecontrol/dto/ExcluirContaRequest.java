package com.santosdev.financecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A senha, para contas com senha; o e-mail da conta, para contas que so entram pelo Google. */
public record ExcluirContaRequest(
        @NotBlank(message = "Informe a confirmação")
        @Size(max = 160, message = "Confirmação inválida")
        String confirmacao) {
}
