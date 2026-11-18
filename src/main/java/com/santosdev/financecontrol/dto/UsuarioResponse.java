package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.security.UsuarioLogado;

/** O que o frontend precisa saber de quem esta logado. Nunca inclui senha ou hash. */
public record UsuarioResponse(Long id, String nome, String email) {

    public static UsuarioResponse de(UsuarioLogado usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
