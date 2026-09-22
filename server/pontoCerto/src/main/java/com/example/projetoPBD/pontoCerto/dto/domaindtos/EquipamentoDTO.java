package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class EquipamentoDTO {

    public record Criar(

            @NotBlank(message = "Código é obrigatorio!")
            String codigo,

            @NotNull(message = "O local de trabalho é obrigatório!")
            UUID localTrabalhoId,

            @NotNull(message = "O tipo do equipamento é obrigatório (RELOGIO ou ESTACAO)")
            TipoEquipamento tipo,

            @NotBlank(message = "A identificação do equipamento é obrigatória")
            String identificacao,

            @Valid
            EstacaoDTO.Criar estacao,

            @Valid
            RelogioDTO.Criar relogio

    ) {}

}
