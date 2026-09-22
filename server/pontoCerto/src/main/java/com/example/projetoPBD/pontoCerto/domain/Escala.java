package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "dias_trabalho", nullable = false)
    private Integer diasTrabalho;

    @Column(name = "dias_folga", nullable = false)
    private Integer diasFolga;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_saida", nullable = false)
    private LocalTime horaSaida;

    @Column(name = "intervalo_inicio", nullable = false)
    private LocalTime intervaloInicio;

    @Column(name = "intervalo_fim", nullable = false)
    private LocalTime intervaloFim;

    @Column(name = "tolerancia_minutos", nullable = false)
    private Integer toleranciaMinutos = 10;

    @Column(name = "carga_diaria_minutos", nullable = false)
    private Integer cargaDiariaMinutos;
}
