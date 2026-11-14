package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "apuracao_diaria", uniqueConstraints = {
        @UniqueConstraint(name = "uk_apuracao_func_data", columnNames = {"funcionario_id", "data_referencia"})
})
public class ApuracaoDiaria {

    public ApuracaoDiaria() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusApuracaoDiaria status;

    // totalizador do dia em minutos
    @Column(name = "minutos_previstos", nullable = false)
    private Integer minutosPrevistos = 0;

    @Column(name = "minutos_trabalhados", nullable = false)
    private Integer minutosTrabalhados = 0;

    @Column(name = "minutos_intervalo", nullable = false)
    private Integer minutosIntervalo = 0;

    @Column(name = "minutos_atraso", nullable = false)
    private Integer minutosAtraso = 0;

    @Column(name = "minutos_saida_antecipada", nullable = false)
    private Integer minutosSaidaAntecipada = 0;

    @Column(name = "minutos_adicional_noturno", nullable = false)
    private Integer minutosAdicionalNoturno = 0;

    @Column(name = "minutos_falta", nullable = false)
    private Integer minutosFalta = 0;

    @Column(name = "minutos_hora_extra_total", nullable = false)
    private Integer minutosHoraExtraTotal = 0;

    // detalhamento em JSON pra o espelho de ponto
    @Column(name = "pares_json", columnDefinition = "TEXT")
    private String paresJson;

    @Column(name = "horas_extras_por_faixa_json", columnDefinition = "TEXT")
    private String horasExtrasPorFaixaJson;

    @Column(name = "calculado_em", nullable = false)
    private LocalDateTime calculadoEm;
}
