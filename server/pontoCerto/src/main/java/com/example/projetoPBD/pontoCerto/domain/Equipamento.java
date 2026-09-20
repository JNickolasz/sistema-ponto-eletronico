package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "equipamento",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_equipamento_empresa_codigo", columnNames = {"empresa_id", "codigo"})
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20,  nullable = false)
    private String codigo; //Identificador usado pelo RH para pesquisas de Equipamentos

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_trabalho_id", nullable = false)
    private LocalDeTrabalho localTrabalho;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false)
    private TipoEquipamento tipo;

    @Column(name = "identificacao", nullable = false)
    private String identificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEquipamento status = StatusEquipamento.ATIVO;


    // AINDA NÃO É PREENCHIDO AUTOMATICAMENTE, TEM QUE CONFIGURAR O AuditorAware
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id",  nullable = false, updatable = false)
    private Funcionario createdBy;

    // AINDA NÃO É PREENCHIDO AUTOMATICAMENTE, TEM QUE CONFIGURAR O AuditorAware
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private Funcionario updatedBy;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Equipamento(TipoEquipamento tipo){
        this.tipo = tipo;
    }

    public Equipamento(){}

}
