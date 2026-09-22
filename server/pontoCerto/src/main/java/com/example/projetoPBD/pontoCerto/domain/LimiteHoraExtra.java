package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "regra_limite_hora_extra")
public class LimiteHoraExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDate inicioVigencia;

    private LocalDate fimVigencia;

    private VigenciaStatus status;
}
