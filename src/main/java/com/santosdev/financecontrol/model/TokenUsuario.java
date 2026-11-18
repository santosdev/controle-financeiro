package com.santosdev.financecontrol.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Token de uso unico enviado por e-mail. Guarda so o hash; o valor original vai apenas no link. */
@Entity
@Table(name = "tokens_usuario")
public class TokenUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoToken tipo;

    @Column(nullable = false, length = 64, unique = true, columnDefinition = "char(64)")
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiraEm;

    private Instant usadoEm;

    @Column(nullable = false)
    private Instant criadoEm;

    protected TokenUsuario() {
    }

    public TokenUsuario(Usuario usuario, TipoToken tipo, String tokenHash, Instant expiraEm) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
        this.criadoEm = Instant.now();
    }

    public boolean estaValido(Instant agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }

    public void marcarComoUsado() {
        this.usadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public TipoToken getTipo() { return tipo; }
    public Instant getCriadoEm() { return criadoEm; }
}
