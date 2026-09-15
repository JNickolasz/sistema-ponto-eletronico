package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.TipoEquipamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class EquipamentoDTO {

    public record Request(
            @NotNull(message = "O tipo do equipamento é obrigatório (RELOGIO ou ESTACAO)")
            TipoEquipamento tipo,

            @NotBlank(message = "A identificação do equipamento é obrigatória")
            String identificacao,

            @NotNull(message = "O local de trabalho é obrigatório")
            UUID localTrabalhoId,

            String numFabricacao
    ) {}

    public record Response(
            UUID id,
            TipoEquipamento tipo,
            String identificacao,
            String numFabricacao,
            StatusEquipamento status,
            UUID localTrabalhoId,
            String localTrabalhoNome,
            UUID empresaId,
            String empresaRazaoSocial
    ) {
        public Response(Equipamento eq) {
            this(
                    eq.getId(),
                    eq.getTipo(),
                    eq.getIdentificacao(),
                    eq.getNumFabricacao(),
                    eq.getStatus(),
                    eq.getLocalTrabalho() != null ? eq.getLocalTrabalho().getId() : null,
                    eq.getLocalTrabalho() != null ? eq.getLocalTrabalho().getNome() : null,
                    eq.getEmpresa() != null ? eq.getEmpresa().getId() : null,
                    eq.getEmpresa() != null ? eq.getEmpresa().getRazaoSocial() : null
            );
        }
    }
}
