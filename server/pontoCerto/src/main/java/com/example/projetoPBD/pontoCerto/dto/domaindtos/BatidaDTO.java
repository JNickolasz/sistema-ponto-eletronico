package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BatidaDTO {

    public record Request(
            @NotNull(message = "ID do funcionário é obrigatório")
            UUID funcionarioId,

            @NotNull(message = "Tipo de registro é obrigatório")
            TipoRegistro tipoRegistro,

            // n vai receber datahora pra evitar fraude
            UUID equipamentoId,
            BigDecimal latitude,
            BigDecimal longitude,
            String ip
    ) {}

    public record ComprovanteResponse(
            UUID id,
            Long nsr,
            String nomeEmpresa,
            String cnpjEmpresa,
            String nomeFuncionario,
            String cpfFuncionario,
            LocalDateTime dataHoraBatida,
            TipoRegistro tipoRegistro,
            TipoOrigem tipoOrigem
    ) {
        public ComprovanteResponse(Batida batida) {
            this (
                    batida.getId(),
                    batida.getNsr(),
                    batida.getFuncionario().getEmpresa().getRazaoSocial(),
                    batida.getFuncionario().getEmpresa().getCnpj(),
                    batida.getFuncionario().getNomeCompleto(),
                    batida.getFuncionario().getCpf(),
                    batida.getDataHora(),
                    batida.getTipoRegistro(),
                    batida.getTipoOrigem()
            );
        }
    }
}
