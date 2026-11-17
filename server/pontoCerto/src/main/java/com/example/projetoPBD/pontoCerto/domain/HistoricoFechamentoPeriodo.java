package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.AcaoFechamentoPeriodo;
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
@Table(
        name = "historico_fechamento_periodo",
        indexes = {
                @Index(name = "idx_hist_fechamento_periodo", columnList = "fechamento_periodo_id, data_hora")
        }
)
public class HistoricoFechamentoPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechamento_periodo_id", nullable = false)
    private FechamentoPeriodo fechamentoPeriodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 40)
    private AcaoFechamentoPeriodo acao;

    @Column(name = "autor", nullable = false, length = 150)
    private String autor;

    @Column(name = "autor_id", nullable = true)
    private UUID autorId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(name = "motivo", columnDefinition = "TEXT", nullable = true)
    private String motivo;
}
