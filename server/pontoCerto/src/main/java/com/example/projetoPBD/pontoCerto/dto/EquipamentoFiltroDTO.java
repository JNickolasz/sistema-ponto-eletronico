package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEstacao;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;

import java.util.UUID;

public record EquipamentoFiltroDTO(
        UUID localDeTrabalhoId,
        TipoEquipamento tipoEquipamento,
        StatusEquipamento status,
        StatusEstacao statusEstacao,
        String numFabricacao,
        Long linhasImportadas
) {
}
