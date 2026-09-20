package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class FeriadoDTO {

    public record Request(
        UUID empresaId, // se não tiver empresa, então o feriado é nacional

        @NotBlank(message = "Descrição obrigatória")
        String descricao,

        @NotNull(message = "Data obrigatória")
        LocalDate data,

        @NotNull(message = "Alcance é obrigatório, nacional, estadual e municipal")
        Alcance alcance,

        String uf,
        String municipio

    ){}

    public record Response(
        UUID id,
        UUID empresaId,
        String descricao,
        LocalDate data,
        Alcance alcance,
        String uf,
        String municipio
    ){
        public Response(Feriado feriado) {
            this(
                    feriado.getId(),
                    feriado.getEmpresa() != null ? feriado.getEmpresa().getId() : null,
                    feriado.getDescricao(),
                    feriado.getData(),
                    feriado.getAlcance(),
                    feriado.getUf(),
                    feriado.getMunicipio()
            );

        }
    }
}
