package com.example.projetoPBD.pontoCerto.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "regra_adicional_noturno")
public class RegraAdicionalNoturno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "regra_apuracao_id", nullable = false, unique = true)
    private RegraApuracao regraApuracao;

    @Column(name = "hora_inicio", nullable = false)
    private Time horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private Time horaFim;

    @Column(name = "duracao_hora_noturna_segundos", nullable = false)
    private Integer duracaoHoraNoturnaSegundos;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal percentual;

    public RegraAdicionalNoturno() {
    }

    public RegraAdicionalNoturno(Time horaInicio, Time horaFim, Integer duracaoHoraNoturnaSegundos, BigDecimal percentual) {
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.duracaoHoraNoturnaSegundos = duracaoHoraNoturnaSegundos;
        this.percentual = percentual;
    }
}
