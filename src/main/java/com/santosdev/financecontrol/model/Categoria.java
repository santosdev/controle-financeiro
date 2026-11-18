package com.santosdev.financecontrol.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    public Categoria() {
    }

    public Categoria(Usuario usuario, String nome, TipoTransacao tipo) {
        this.usuario = usuario;
        this.nome = nome;
        this.tipo = tipo;
    }

    public Usuario getUsuario() { return usuario; }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoTransacao getTipo() { return tipo; }
    public void setTipo(TipoTransacao tipo) { this.tipo = tipo; }
}
