package com.example.projetoPBD.pontoCerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

public class EmpresaDTO {

    public record Criar(

            @NotBlank(message = "Razão social é obrigatório")
            String razaoSocial,

            @NotBlank(message = "Subdomínio é obrigatório")
            String subdominio,

            @NotBlank(message = "CNPJ é obrigatório")
            String cnpj,

            @NotBlank(message = "Endereço é obrigatório")
            String endereco,

            String logoUrl

            // Está faltando o campo de WhiteLabels

    ) {}
    public record Response(
            UUID id,
            String razaoSocial

    ) {}

}
