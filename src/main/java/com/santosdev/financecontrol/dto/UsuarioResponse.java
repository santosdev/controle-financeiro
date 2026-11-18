package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.model.Usuario;

/**
 * O que o frontend precisa saber de quem esta logado. Nunca inclui senha ou hash.
 * {@code fotoVersao} e nulo quando nao ha foto; quando existe, muda a cada troca (serve para atualizar o cache).
 */
public record UsuarioResponse(Long id, String nome, String apelido, String nomeExibicao, String email,
                              boolean temSenha, Long fotoVersao) {

    public static UsuarioResponse de(Usuario usuario, Long fotoVersao) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getApelido(),
                usuario.nomeDeExibicao(), usuario.getEmail(), usuario.getSenhaHash() != null, fotoVersao);
    }
}
