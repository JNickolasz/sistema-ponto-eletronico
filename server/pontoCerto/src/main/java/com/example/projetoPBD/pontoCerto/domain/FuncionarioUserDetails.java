package com.example.projetoPBD.pontoCerto.domain;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class FuncionarioUserDetails implements UserDetails {

    private final Funcionario funcionario;

    public FuncionarioUserDetails(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public Funcionario getFuncionario(){
        return funcionario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + funcionario.getPerfilAcesso().name()));
    }

    @Override
    public String getPassword() {
        return funcionario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return funcionario.getUsuario();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
