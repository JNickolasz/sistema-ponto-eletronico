package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "funcionario", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "cpf"}),
        @UniqueConstraint(columnNames = {"empresa_id", "matricula"}),
        @UniqueConstraint(columnNames = {"empresa_id", "usuario"}),
        @UniqueConstraint(columnNames = {"empresa_id", "email"})
})
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@ManyToOne
    //@JoinColumn(name = "empresa_id", nullable = true) // provisoriamente, depois fica = false
    @Column(name = "empresa_id")
    private String empresaId; // tem q mudar o tipo pra Empresa dps

    @ManyToOne
    @JoinColumn(name = "gestor_id", nullable = true) // provisoriamente, depois fica = false
    private Funcionario gestor;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_acesso", nullable = false)
    private PerfilAcesso.Perfil perfilAcesso;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmpresa() {
        return empresaId;
    }

    public void setEmpresa(String empresa) {
        this.empresaId = empresaId;
    }

    public Funcionario getGestor() {
        return gestor;
    }

    public void setGestor(Funcionario gestor) {
        this.gestor = gestor;
    }

    public PerfilAcesso.Perfil getPerfilAcesso() {
        return perfilAcesso;
    }

    public void setPerfilAcesso(PerfilAcesso.Perfil perfilAcesso) {
        this.perfilAcesso = perfilAcesso;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPisPasep() {
        return pisPasep;
    }

    public void setPisPasep(String pisPasep) {
        this.pisPasep = pisPasep;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getJornada() {
        return jornada;
    }

    public void setJornada(String jornada) {
        this.jornada = jornada;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Funcionario that = (Funcionario) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}