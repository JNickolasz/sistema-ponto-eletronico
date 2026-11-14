package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitacao_ajuste")
public class SolicitacaoAjuste {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Funcionario colaborador;

    @Column(name = "data_alvo", nullable = false)
    private LocalDate dataAlvo;

    @Column(name = "horario_pretendido", nullable = false)
    private LocalTime horarioPretendido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_registro", nullable = false)
    private TipoRegistro tipoRegistro;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSolicitacaoAjuste status = StatusSolicitacaoAjuste.PENDENTE;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_id", nullable = true)
    private Funcionario gestor;

    @Column(name = "analisado_em", nullable = true)
    private LocalDateTime analisadoEm;

    @Column(name = "justificativa_gestor", columnDefinition = "TEXT", nullable = true)
    private String justificativaGestor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batida_criada_id", nullable = true)
    private Batida batidaCriada;
}
