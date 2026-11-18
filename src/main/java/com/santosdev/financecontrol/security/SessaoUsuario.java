package com.santosdev.financecontrol.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Guarda o usuario autenticado na sessao (cookie HttpOnly), trocando o id da sessao para evitar fixacao de sessao. */
@Component
public class SessaoUsuario {

    private final SecurityContextRepository repositorio = new HttpSessionSecurityContextRepository();

    public void iniciar(Authentication autenticacao, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        repositorio.saveContext(contexto, request, response);
    }
}
