package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusImportacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "historico_importacao")
public class HistoricoImportacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "data_importacao", nullable = false, updatable = false)
    private LocalDateTime dataImportacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = true)
    private Funcionario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "conteudo_arquivo", columnDefinition = "TEXT")
    private String conteudoArquivo;

    @Column(name = "total_lidas")
    private Integer totalLidas = 0;

    @Column(name = "total_validas", nullable = false)
    private Integer totalValidas = 0;

    @Column(name = "total_repetidas", nullable = false)
    private Integer totalRepetidas = 0;

    @Column(name = "total_recusadas", nullable = false)
    private Integer totalRecusadas = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusImportacao status = StatusImportacao.CONCLUIDA;
}
