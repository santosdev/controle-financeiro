package com.santosdev.financecontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Foto de perfil (sempre JPEG 256x256, ja processada). Fica em tabela propria, uma linha por usuario. */
@Entity
@Table(name = "fotos_usuario")
public class FotoUsuario {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private byte[] dados;

    @Column(nullable = false)
    private Instant atualizadaEm;

    protected FotoUsuario() {
    }

    public FotoUsuario(Long usuarioId, byte[] dados, Instant atualizadaEm) {
        this.usuarioId = usuarioId;
        this.dados = dados;
        this.atualizadaEm = atualizadaEm;
    }

    public Long getUsuarioId() { return usuarioId; }
    public byte[] getDados() { return dados; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
