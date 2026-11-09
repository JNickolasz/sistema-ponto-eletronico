package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.AcaoTratamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
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
@Table(name = "inconsistencia")
public class Inconsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoInconsistencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, columnDefinition = "varchar(30) default 'ABERTA'")
    private StatusInconsistencia situacao = StatusInconsistencia.ABERTA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = true) // nullable temporariamente
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = true)
    private Equipamento equipamento;

    @Column(name = "batida_origem_id", nullable = true)
    private UUID batidaOrigemId;

    @Column(name = "batida_secundaria_id", nullable = true)
    private UUID batidaSecundariaId;

    @Column(name = "arquivo_importado_id", nullable = true)
    private UUID arquivoImportadoId;

    @Column(name = "nome_arquivo", nullable = true)
    private String nomeArquivo;

    @Column(name = "identificador_recebido", nullable = true) // PIS/CPF/Matricula recebido
    private String identificadorRecebido;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "data_hora_origem", nullable = false)
    private LocalDateTime dataHoraOrigem;

    @Column(name = "descricao_origem", columnDefinition = "TEXT", nullable = false) // originador da pendencia
    private String descricaoOrigem;

    @Column(name = "chave_deduplicacao", nullable = true)
    private String chaveDeduplicacao;

    @Column(name = "criado_em", nullable = false, updatable = false) // da inconsistencia
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "acao_tomada", nullable = true)
    private AcaoTratamento acaoTomada;

    @Column(name = "tratado_por", nullable = true)
    private String tratadoPor;

    @Column(name = "tratado_por_id", nullable = true)
    private UUID tratadoPorId;

    @Column(name = "tratado_em", nullable = true)
    private LocalDateTime tratadoEm;

    @Column(name = "motivo_tratamento", columnDefinition = "TEXT", nullable = true)
    private String motivoTratamento;

    @Column(name = "detalhe_resolucao", columnDefinition = "TEXT", nullable = true)
    private String detalheResolucao;
}
