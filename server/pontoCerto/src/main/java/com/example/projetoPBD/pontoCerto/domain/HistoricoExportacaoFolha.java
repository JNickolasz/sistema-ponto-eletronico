package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "historico_exportacao_folha")
public class HistoricoExportacaoFolha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "competencia", nullable = false, length = 7)
    private String competencia;

    @Column(name = "autor", nullable = false)
    private String autor;

    @Column(name = "autor_id", nullable = true)
    private UUID autorId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "quantidade_colaboradores", nullable = false)
    private Integer quantidadeColaboradores = 0;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;
}
