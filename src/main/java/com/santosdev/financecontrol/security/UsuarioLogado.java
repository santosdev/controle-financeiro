package com.santosdev.financecontrol.security;

import com.santosdev.financecontrol.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario guardado na sessao depois do login. E o mesmo objeto tanto no login com senha
 * quanto no login com o Google, entao o resto do sistema so precisa conhecer este tipo.
 */
public class UsuarioLogado implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;

    public UsuarioLogado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return senhaHash == null ? "" : senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
