package com.santosdev.financecontrol.model;

/**
 * Como a conta foi criada: com e-mail e senha (LOCAL), pelo "Entrar com o Google"
 * ou pelo "Entrar como visitante" (conta temporaria, sem e-mail real e sem senha).
 */
public enum ProvedorLogin {
    LOCAL,
    GOOGLE,
    VISITANTE
}
