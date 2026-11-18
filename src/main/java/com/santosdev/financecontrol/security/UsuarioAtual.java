package com.santosdev.financecontrol.security;

import com.santosdev.financecontrol.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Quem esta logado nesta requisicao. Os servicos usam isto para filtrar tudo pelo dono dos dados. */
@Component
public class UsuarioAtual {

    public Long id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioLogado usuario) {
            return usuario.getId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Você precisa entrar para continuar");
    }
}
