package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "regra_faixa_hora_extra")
public class FaixaHoraExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column()
    private LocalDate inicioVigencia;

    private LocalDate fimVigencia;

    private VigenciaStatus status;

}
