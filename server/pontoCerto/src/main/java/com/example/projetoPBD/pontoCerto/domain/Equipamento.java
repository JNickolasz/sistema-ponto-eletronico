package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "equipamento",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_equipamento_empresa_num_fabricacao", columnNames = {"empresa_id", "num_fabricacao"})
    }
)
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_trabalho_id", nullable = false)
    private LocalDeTrabalho localTrabalho;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEquipamento tipo;

    @Column(name = "identificacao", nullable = false)
    private String identificacao;

    @Column(name = "num_fabricacao")
    private String numFabricacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEquipamento status = StatusEquipamento.ATIVO;

}
