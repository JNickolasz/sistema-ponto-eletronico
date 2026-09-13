package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "funcionario", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "cpf"}),
        @UniqueConstraint(columnNames = {"empresa_id", "matricula"}),
        @UniqueConstraint(columnNames = {"empresa_id", "usuario"}),
        @UniqueConstraint(columnNames = {"empresa_id", "email"})
})
public class Funcionario{

    public Funcionario(){}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)

    @Column(name = "empresa_id")
    private Empresa empresaId;

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

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

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

}