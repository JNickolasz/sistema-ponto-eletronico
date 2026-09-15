package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;


import java.util.UUID;

public class EmpresaDTO {

    public record Criar(


            @NotBlank(message = "Razão social é obrigatório")
            String razaoSocial,

            @NotBlank(message = "Subdomínio é obrigatório")
            String subdominio,

            @CNPJ(message = "CNPJ deve ser válido")
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
