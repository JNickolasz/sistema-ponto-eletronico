package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class RegraApuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    private FaixaHoraExtra faixaHoraExtraVigente;

    @OneToOne
    private RegraAdicionalNoturno regraAdicionalNoturnoVigente;

    @OneToOne
    private LimiteHoraExtra limiteHoraExtraVigente;

}
