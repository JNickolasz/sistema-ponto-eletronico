package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "fechamento_periodo",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fechamento_empresa_comp", columnNames = {"empresa_id", "mes_referencia"})
        }
)
public class FechamentoPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "mes_referencia", nullable = false, length = 7)
    private String mesReferencia; // formato YYYY-MM (competencia)

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDateTime dataFechamento = LocalDateTime.now();

    @Column(name = "fechado_por", nullable = false)
    private String fechadoPor;

    @Column(name = "fechado_por_id", nullable = true)
    private UUID fechadoPorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusFechamentoPeriodo status;

    @Column(name = "pendencias_abertas_no_fechamento", nullable = false, columnDefinition = "integer default 0")
    private Integer pendenciasAbertasNoFechamento = 0;

    @Column(name = "aviso_pendencias", columnDefinition = "TEXT", nullable = true)
    private String avisoPendencias;

    // Aliases semânticos do Card 17 (compatíveis com T12)
    public String getCompetencia() {
        return mesReferencia;
    }

    public void setCompetencia(String competencia) {
        this.mesReferencia = competencia;
    }

    public LocalDateTime getFechadoEm() {
        return dataFechamento;
    }

    public void setFechadoEm(LocalDateTime fechadoEm) {
        this.dataFechamento = fechadoEm;
    }

    public Integer getQuantidadePendenciasAbertas() {
        return pendenciasAbertasNoFechamento;
    }

    public void setQuantidadePendenciasAbertas(Integer quantidadePendenciasAbertas) {
        this.pendenciasAbertasNoFechamento = quantidadePendenciasAbertas;
    }

    public String getRessalvaOuAviso() {
        return avisoPendencias;
    }

    public void setRessalvaOuAviso(String ressalvaOuAviso) {
        this.avisoPendencias = ressalvaOuAviso;
    }
}
