package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class RelogioDTO {

    public record Criar(

            @NotBlank(message = "Número de fabricação é obrigatório!")
            String numFabricacao,

            Long linhasImportadas

    ) { }

    public record Response(

            UUID id,
            String codigo,
            TipoEquipamento tipo,
            String identificacao,
            StatusEquipamento status,
            UUID localTrabalhoId,
            String numFabricacao,
            Long linhasImportadas


    ) implements EquipamentoResponse{}

}
