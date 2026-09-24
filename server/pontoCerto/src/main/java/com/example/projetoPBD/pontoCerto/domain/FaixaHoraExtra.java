package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import jakarta.persistence.*;
import jakarta.validation.Constraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal porcentagem;

}