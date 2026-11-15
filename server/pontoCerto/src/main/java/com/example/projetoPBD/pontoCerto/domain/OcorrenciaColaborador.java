package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "ocorrencias_colaborador",
        indexes = {
                @Index(name = "idx_ocorrencia_func_periodo", columnList = "funcionario_id, data_inicio, data_fim")
        }
)
public class OcorrenciaColaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_ocorrencia_id", nullable = false)
    private TipoOcorrencia tipoOcorrencia;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "minutos_abono_por_dia")
    private Integer minutosAbonoPorDia;

    @Column(name = "observacao", nullable = true, columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "nome_arquivo_anexo", length = 255)
    private String nomeArquivoAnexo;

    @Column(name = "caminho_anexo", length = 500)
    private String caminhoAnexo;

    @Column(name = "content_type_anexo", length = 100)
    private String contentTypeAnexo;

    @Column(name = "lancado_por", nullable = false, length = 150)
    private String lancadoPor;

    @Column(name = "lancado_em", nullable = false)
    private LocalDateTime lancadoEm;
}
