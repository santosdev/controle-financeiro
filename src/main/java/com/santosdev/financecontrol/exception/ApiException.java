package com.santosdev.financecontrol.exception;

import org.springframework.http.HttpStatus;

/** Erro de negocio com status HTTP e um codigo curto que o frontend pode usar (ex.: EMAIL_NAO_VERIFICADO). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
