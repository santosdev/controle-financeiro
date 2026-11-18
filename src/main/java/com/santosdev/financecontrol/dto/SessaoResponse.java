package com.santosdev.financecontrol.dto;

/** Resposta de "tem alguém logado?". Sem login devolve autenticado=false (200), sem gerar erro no navegador. */
public record SessaoResponse(boolean autenticado, UsuarioResponse usuario) {

    public static SessaoResponse anonima() {
        return new SessaoResponse(false, null);
    }
}
