package com.santosdev.financecontrol.dto;

/** Regras compartilhadas pelos formularios de conta. O frontend repete as mesmas regras para avisar antes de enviar. */
public final class RegrasDeCampo {

    /** Pelo menos uma letra e um numero. */
    public static final String SENHA = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String MENSAGEM_SENHA = "A senha deve ter de 8 a 64 caracteres, com pelo menos uma letra e um número";

    public static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    public static final String MENSAGEM_EMAIL = "Informe um e-mail válido";

    private RegrasDeCampo() {
    }
}
