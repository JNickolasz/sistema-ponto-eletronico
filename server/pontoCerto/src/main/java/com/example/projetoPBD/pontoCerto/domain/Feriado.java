package com.example.projetoPBD.pontoCerto.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "feriado")
public class Feriado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    //Declara um relacionamento de "Muitos para Um" com a entidade Empresa.
    // A opção LAZY (carregamento preguiçoso) melhora a performance ao carregar os dados da empresa
    @JoinColumn(name = "empresa_id") // define coluna de chave estrangeira
    private Empresa empresa;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false)
    private Alcance alcance;

    @Column(name = "uf")
    private String uf;

    @Column(name = "municipio")
    private String municipio;

}
