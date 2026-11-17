package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.TipoMovimentacaoBancoHoras;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
        name = "movimentacao_banco_horas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bh_apuracao_tipo", columnNames = {"apuracao_diaria_id", "tipo"})
        },
        indexes = {
                @Index(name = "idx_bh_func_data", columnList = "funcionario_id, data_referencia")
        }
)
public class MovimentacaoBancoHoras {

    public MovimentacaoBancoHoras() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimentacaoBancoHoras tipo;

    // Positivo para crédito (+), negativo para débito ou compensação (-)
    @Column(name = "minutos", nullable = false)
    private Integer minutos;

    // Vínculo opcional com a ApuracaoDiaria que gerou o lançamento automático
    @Column(name = "apuracao_diaria_id")
    private UUID apuracaoDiariaId;

    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Column(name = "periodo_fechado", nullable = false)
    private boolean periodoFechado = false;

    @Column(name = "registrado_por", length = 150)
    private String registradoPor;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;
}
