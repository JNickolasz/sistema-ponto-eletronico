package com.example.projetoPBD.pontoCerto.service.mapper;


import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.Relogio;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoResponse;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EstacaoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RelogioDTO;

public class EquipamentoResponseMapper {

    public static EquipamentoResponse from(Equipamento equipamento) {

        if (equipamento instanceof Relogio relogio){
            return new RelogioDTO.Response(
                    relogio.getId(),
                    relogio.getCodigo(),
                    relogio.getTipo(),
                    relogio.getIdentificacao(),
                    relogio.getStatus(),
                    relogio.getLocalTrabalho().getId(),

                    relogio.getNumFabricante(),
                    relogio.getLinhasImportadas()
            );
        }


        if (equipamento instanceof Estacao estacao){
            return new EstacaoDTO.Response(
                    estacao.getId(),
                    estacao.getCodigo(),
                    estacao.getTipo(),
                    estacao.getIdentificacao(),
                    estacao.getStatus(),
                    estacao.getLocalTrabalho().getId(),

                    estacao.getCredentialId(),
                    estacao.getPublicKey(),
                    estacao.getStatusEstacao()
            );
        }

        throw new IllegalArgumentException("Tipo de equipamento não suportado" + equipamento.getClass());

    }

}
