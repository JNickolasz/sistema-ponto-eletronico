package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.PontoFacultativo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class PontoFacultativoDTO {
    public record Request(
        @NotBlank(message = "A descrição é obrigatória")
        String descricao,
        @NotNull(message = "A data é obrigatória")
        LocalDate data,
        @NotNull(message = "O alcance é obrigatório, nacional, estadual e municipal")
        Alcance alcance,

        String uf,
        String municipio
    ){}
    public record Response(
            UUID id,
            String descricao,
            LocalDate data,
            Alcance alcance,
            String uf,
            String municipio,
            boolean ativo
    ){
        public Response(PontoFacultativo pf) {
            this(
                    pf.getId(),
                    pf.getDescricao(),
                    pf.getData(),
                    pf.getAlcance(),
                    pf.getUf(),
                    pf.getMunicipio(),
                    pf.isAtivo()
            );
        }
    }
}
