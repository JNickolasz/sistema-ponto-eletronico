package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "empresa")

public class Empresa {

    public Empresa(){}

    public Empresa(String razaoSocial, String cnpj){
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "subdominio", nullable = false, unique = true)
    private String subDominio;

    @Column(name = "cnpj", nullable = false, unique = true)
    private String cnpj;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "endereco")
    private String endereco;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_whitelabel", columnDefinition = "jsonb")
    private WhiteLabel configWhiteLabel;

}
