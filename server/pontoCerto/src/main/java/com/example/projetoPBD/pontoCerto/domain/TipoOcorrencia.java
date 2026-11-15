package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "tipo_ocorrencia",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipo_ocorrencia_empresa_nome", columnNames = {"empresa_id", "nome"})
        })
public class TipoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 255, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_abono", nullable = false, length = 20)
    private TipoAbonoOcorrencia tipoAbono;

    @Column(name = "minutos_abono_padrao")
    private Integer minutosAbonoPadrao;

    @Column(name = "exige_anexo", nullable = false)
    private boolean exigeAnexo = false;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
