package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
        name = "fechamento_banco_horas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bh_fechamento_func_mes", columnNames = {"funcionario_id", "mes_referencia"})
        },
        indexes = {
                @Index(name = "idx_bh_fechamento_func", columnList = "funcionario_id")
        }
)
public class FechamentoBancoHoras {

    public FechamentoBancoHoras() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    // Formato YYYY-MM
    @Column(name = "mes_referencia", nullable = false, length = 7)
    private String mesReferencia;

    @Column(name = "saldo_anterior_minutos", nullable = false)
    private Integer saldoAnteriorMinutos = 0;

    @Column(name = "creditos_periodo_minutos", nullable = false)
    private Integer creditosPeriodoMinutos = 0;

    @Column(name = "debitos_periodo_minutos", nullable = false)
    private Integer debitosPeriodoMinutos = 0;

    @Column(name = "compensacoes_periodo_minutos", nullable = false)
    private Integer compensacoesPeriodoMinutos = 0;

    // Valor travado no ato do fechamento, imune a alterações posteriores de regras
    @Column(name = "saldo_final_congelado_minutos", nullable = false)
    private Integer saldoFinalCongeladoMinutos = 0;

    @Column(name = "fechado_em", nullable = false)
    private LocalDateTime fechadoEm;

    @Column(name = "fechado_por", length = 150)
    private String fechadoPor;
}
