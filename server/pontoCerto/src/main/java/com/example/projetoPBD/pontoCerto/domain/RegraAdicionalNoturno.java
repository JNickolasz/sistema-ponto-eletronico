package com.example.projetoPBD.pontoCerto.domain;


import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "regra_adicional_noturno")
public class RegraAdicionalNoturno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "regra_apuracao_id", nullable = false)
    private RegraApuracao regraApuracao;

    @Column(name = "hora_inico", nullable = false)
    private Time horaIncio;

    @Column(name = "hora_fim", nullable = false)
    private Time horaFim;

    @Column(name = "duracao_hora_noturna_segundos", nullable = false)
    private Integer duracaoHoraNoturnaSegundos;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal percentual;

}
