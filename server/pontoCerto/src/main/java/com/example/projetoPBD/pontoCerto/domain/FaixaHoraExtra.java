package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "regra_faixa_hora_extra",  uniqueConstraints = {
        @UniqueConstraint(columnNames = {"regra_apuracao_id", "ordem"})
})
public class FaixaHoraExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "regra_apuracao_id",  nullable = false)
    private RegraApuracao regraApuracao;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Column(name = "duracao_em_minutos")
    private Integer duracaoMinutos;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal percentual;


    public FaixaHoraExtra() {
    }

    public FaixaHoraExtra(Integer ordem, Integer duracaoMinutos, BigDecimal percentual) {
        this.ordem = ordem;
        this.duracaoMinutos = duracaoMinutos;
        this.percentual = percentual;
    }
}