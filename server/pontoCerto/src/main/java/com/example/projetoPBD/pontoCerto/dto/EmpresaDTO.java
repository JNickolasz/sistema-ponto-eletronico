package com.example.projetoPBD.pontoCerto.dto;

import java.util.UUID;

public class EmpresaDTO {
    public record Request(
            String razaoSocial,
            String subdominio,
            String cnpj,
            String endereco
            //String logoUrl,

    ) {}
    public record Response(
            UUID id,
            String razaoSocial

    ) {}
}
