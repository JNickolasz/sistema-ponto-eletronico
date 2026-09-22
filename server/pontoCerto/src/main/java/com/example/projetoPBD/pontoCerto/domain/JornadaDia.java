package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "jornada_dia")
public class JornadaDia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jornada_id", nullable = false)
    private Jornada jornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15)
    private DiaSemana diaSemana;

    @Column(name = "dia_trabalho", nullable = false)
    private boolean diaTrabalho;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "hora_saida")
    private LocalTime horaSaida;

    @Column(name = "intervalo_inicio")
    private LocalTime intervaloInicio;

    @Column(name = "intervalo_fim")
    private LocalTime intervaloFim;

    @Column(name = "carga_diaria_minutos")
    private Integer cargaDiariaMinutos;
}
