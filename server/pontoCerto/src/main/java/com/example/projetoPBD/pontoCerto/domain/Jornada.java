package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "jornada")
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "nome", length = 100)
    private String nome;

    @Column(name = "toleranciaMinutos", nullable = false)
    private Integer toleranciaMinutos = 10;

    @Column(name = "carga_horaria_semanal_minutos")
    private Integer cargaHorariaSemanalMinutos;

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JornadaDia> dias;
}
