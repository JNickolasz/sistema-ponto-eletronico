package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "local_trabalho")

public class LocalDeTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "latitude", precision = 8, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "raio_metros")
    private Integer raioMetros = 0;

    @Column(name = "ip_esperado", length = 45)
    private String ipEsperado;

    @Column(name = "endereco", nullable = false)
    private String endereco;

    @Column(name = "municipio", nullable = false)
    private String municipio;

    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

}
