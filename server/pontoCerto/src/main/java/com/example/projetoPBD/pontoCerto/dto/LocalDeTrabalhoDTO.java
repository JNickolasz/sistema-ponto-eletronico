package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;

import java.math.BigDecimal;
import java.util.UUID;

public class LocalDeTrabalhoDTO {
    public record Request(
            UUID empresaId,
            String nome,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer raioMetros,
            String ipEsperado,
            String endereco,
            String municipio,
            String uf
    ) {}

    public record Response(
            UUID id,
            UUID empresaId,
            String razaoSocialEmpresa,
            String nome,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer raioMetros,
            String ipEsperado,
            String endereco,
            String municipio,
            String uf
    ) {
        public Response(LocalDeTrabalho local) {
            this(
                    local.getId(),
                    local.getEmpresa().getId(),
                    local.getEmpresa().getRazaoSocial(),
                    local.getNome(),
                    local.getLatitude(),
                    local.getLongitude(),
                    local.getRaioMetros(),
                    local.getIpEsperado(),
                    local.getEndereco(),
                    local.getMunicipio(),
                    local.getUf()
            );
        }
    }
}


