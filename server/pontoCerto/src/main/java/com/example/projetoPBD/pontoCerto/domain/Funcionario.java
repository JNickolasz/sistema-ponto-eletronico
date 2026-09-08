package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "funcionario", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "cpf"}),
        @UniqueConstraint(columnNames = {"empresa_id", "matricula"}),
        @UniqueConstraint(columnNames = {"empresa_id", "usuario"}),
        @UniqueConstraint(columnNames = {"empresa_id", "email"})
})
public class Funcionario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@ManyToOne
    //@JoinColumn(name = "empresa_id", nullable = true) // provisoriamente, depois fica = false
    @Column(name = "empresa_id")
    private Long empresaId; // tem q mudar o tipo pra Empresa dps

    @ManyToOne
    @JoinColumn(name = "gestor_id", nullable = true) // provisoriamente, depois fica = false
    private Funcionario gestor;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_acesso", nullable = false)
    private PerfilAcesso perfilAcesso;

    @Column(name = "matricula", length = 20, nullable = true) // provisoriamente, depois fica = false
    private String matricula;

    @Column(name = "cpf", length = 14, nullable = true) // provisoriamente, depois fica = false
    private String cpf;

    @Column(name = "pis_pasep", length = 14, nullable = true) // provisoriamente, depois fica = false
    private String pisPasep;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "email", nullable = true) // provisoriamente, depois fica = false
    private String email;

    @Column(name = "telefone", nullable = true) // provisoriamente, depois fica = false
    private String telefone;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    @Column(name = "pin_hash", length = 60, nullable = true) // provisoriamente, depois fica = false
    private String pinHash;

    // lembrar de por o ManyToOne
    //@JoinColumn(name = "jornada_id", nullable = true)
    // Vai ficar como nullable apenas por enquanto, já q nem na T1 nem T2 pedem
    @Column(name = "jornada_id")
    private String jornada; // depoi vira tipo Jornada

    @Column(name = "cargo", nullable = true) // provisoriamente, depois fica = false
    private String cargo;

    @Column(name = "data_admissao", nullable = true) // provisoriamente, depois fica = false
    private LocalDate dataAdmissao;

    @Column(name = "data_desligamento", nullable = true) // provisoriamente, depois fica = false
    private LocalDate dataDesligamento;

    public Funcionario() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.perfilAcesso == null) {
            return List.of();
        }
        return List.of(
                new SimpleGrantedAuthority(this.perfilAcesso.name()),
                new SimpleGrantedAuthority("ROLE_" + this.perfilAcesso.name())
        );
    }
    @Override
    public String getPassword() { return this.senhaHash; }
    @Override
    public String getUsername() { return this.usuario; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return this.dataDesligamento == null; }
}