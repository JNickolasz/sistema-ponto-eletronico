package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEstacao;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class EstacaoDTO {

    public record Criar(
        @NotBlank(message = "Credential é obrigatória!")
        String credentialId,

        @NotBlank(message = "Chave publica é obrigatória!")
        String publicKey

    ) { }

    public record Response(

            UUID id,
            String codigo,
            TipoEquipamento tipo,
            String identificacao,
            StatusEquipamento status,
            UUID localTrabalhoId,

            String credentialId,
            String publicKey,
            StatusEstacao statusEstacao

    ) implements EquipamentoResponse{}


}
