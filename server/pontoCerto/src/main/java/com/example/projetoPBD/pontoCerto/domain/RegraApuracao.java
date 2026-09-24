package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "regras_apuracao", uniqueConstraints = {@UniqueConstraint(columnNames = {"empresa_id","inicio_vigencia"})})
public class RegraApuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "inicio_vigencia", nullable = false)
    private LocalDate inicioVigencia;

    @Column(name = "limite_diario_extra_em_minutos", nullable = false)
    private Integer limiteDiarioExtraMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "vigencia_status", nullable = false)
    private VigenciaStatus vigenciaStatus = VigenciaStatus.AGENDADO;

    @OneToMany(mappedBy = "regras_apuracao")
    List<FaixaHoraExtra> faixaHoraExtras;

    @OneToOne(mappedBy = "regras_apuracao")
    RegraAdicionalNoturno regraAdicionalNoturno;

    // Precisa do AuditorAware para preenchimento automatico. True por enquanto.
    @Column(name = "created_at", nullable = true)
    private Instant createdAt;

    // Precisa do AuditorAware para preenchimento automatico.
    @Column(name = "updated_at")
    private Instant updatedAt;


    // Precisa do AuditorAware para preenchimento automatico. True por enquanto.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",  nullable = true)
    private Funcionario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Funcionario updatedBy;

}
