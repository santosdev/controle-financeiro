package com.santosdev.financecontrol.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Como a pessoa quer ser chamada. Opcional: sem apelido, aparece o primeiro nome. */
    @Column(length = 40)
    private String apelido;

    /** Sempre guardado em minusculas e sem espacos nas pontas. */
    @Column(nullable = false, length = 160, unique = true)
    private String email;

    /** Hash BCrypt. Nulo para quem so entra pelo Google. */
    @Column(length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProvedorLogin provedor;

    @Column(nullable = false)
    private boolean emailVerificado;

    @Column(nullable = false)
    private int tentativasFalhas;

    private Instant bloqueadoAte;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, ProvedorLogin provedor, boolean emailVerificado) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.provedor = provedor;
        this.emailVerificado = emailVerificado;
        this.criadoEm = Instant.now();
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    /** Conta uma senha errada; ao atingir o limite, bloqueia o login por um tempo. */
    public void registrarFalhaDeLogin(int limite, java.time.Duration bloqueio) {
        tentativasFalhas++;
        if (tentativasFalhas >= limite) {
            bloqueadoAte = Instant.now().plus(bloqueio);
            tentativasFalhas = 0;
        }
    }

    /** Conta temporaria do "Entrar como visitante": e apagada ao sair ou depois de algumas horas. */
    public boolean isVisitante() {
        return provedor == ProvedorLogin.VISITANTE;
    }

    public void zerarFalhasDeLogin() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    /** Nome mostrado no topo do sistema: o apelido, ou o primeiro nome quando nao ha apelido. */
    public String nomeDeExibicao() {
        if (apelido != null && !apelido.isBlank()) {
            return apelido;
        }
        String completo = nome == null ? "" : nome.trim();
        int espaco = completo.indexOf(' ');
        return espaco > 0 ? completo.substring(0, espaco) : completo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public ProvedorLogin getProvedor() { return provedor; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public Instant getBloqueadoAte() { return bloqueadoAte; }
    public Instant getCriadoEm() { return criadoEm; }
}
