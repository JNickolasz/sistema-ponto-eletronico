package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "batida", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nsr"})
})
@SequenceGenerator(name = "seq_nsr_batida", sequenceName = "seq_nsr_batida", allocationSize = 1)
public class Batida {
    public Batida () {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "nsr",
            nullable = false,
            updatable = false,
            insertable = false,
            columnDefinition = "BIGINT DEFAULT nextval('seq_nsr_batida')"
    )
    @org.hibernate.annotations.Generated

    private Long nsr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = true)
    private Funcionario funcionario;

    @Column(name = "identificador_original", nullable = true, updatable = false)
    private String identificadorOriginal;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_registro", nullable = false, updatable = false)
    private TipoRegistro tipoRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_batida", nullable = false, columnDefinition = "varchar(30) default 'NORMAL'")
    private StatusBatida statusBatida = StatusBatida.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_origem", nullable = false, updatable = false)
    private TipoOrigem tipoOrigem;

    @Column(name = "vinculado_por", nullable = true)
    private String vinculadoPor;

    @Column(name = "data_vinculo", nullable = true)
    private LocalDateTime dataVinculo;

    @Column(name = "motivo_vinculo", nullable = true)
    private String motivoVinculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = true, updatable = false)
    private Equipamento equipamento;

    // futuramente quando implementar a exportação e importação de arquivos
    // e criar a entiade de arquivo mudar pra tipo Arquivo
    //@ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "arquivo_importado_id", nullable = true, updatable = false) // mudar pra false dps
    private UUID arquivoImportadoId;

    // futuramente quando implementar a abertura de pedidos de ajuste
    // e criar a entiade de ajuste mudar pra tipo respectivo
    //@ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "pedido_ajuste_id", nullable = true, updatable = false) // mudar pra false dps
    private UUID pedidoAjusteId;

    @Column(name = "foto_url", nullable = true, updatable = false) // para teste, false, mas só por hora
    private String fotoUrl;

    @Column(name = "latitude_recebida", nullable = true, updatable = false) // só pra testes
    private BigDecimal latitudeRecebida;

    @Column(name = "longitude_recebida", nullable = true, updatable = false) // tbm pra testes só
    private BigDecimal longitudeRecebida; // tem q ver isso aqui como vai ser

    @Column(name = "ip_recebido", nullable = true, updatable = false) // mesma coisa
    private String ipRecebido;
}
