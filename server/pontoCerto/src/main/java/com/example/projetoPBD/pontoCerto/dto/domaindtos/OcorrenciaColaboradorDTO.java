package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.OcorrenciaColaborador;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class OcorrenciaColaboradorDTO {

    public record CriarRequest(
            UUID funcionarioId,
            UUID tipoOcorrenciaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer minutosAbonoPadrao,
            String observacao
    ) {}

    public record Response(
            UUID id,
            UUID funcionarioId,
            String nomeFuncionario,
            String matriculaFuncionario,
            UUID tipoOcorrenciaId,
            String nomeTipoOcorrencia,
            TipoAbonoOcorrencia tipoAbono,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer minutosAbonadosPorDia,
            String observacao,
            String nomeArquivo,
            boolean possuiAnexo,
            String lancadoPor,
            LocalDateTime lancadoEm
    ) {
        public Response(OcorrenciaColaborador o) {
            this(
                    o.getId(),
                    o.getFuncionario() != null ? o.getFuncionario().getId() : null,
                    o.getFuncionario() != null ? o.getFuncionario().getNomeCompleto() : null,
                    o.getFuncionario() != null ? o.getFuncionario().getMatricula() : null,
                    o.getTipoOcorrencia() != null ? o.getTipoOcorrencia().getId() : null,
                    o.getTipoOcorrencia() != null ? o.getTipoOcorrencia().getNome() : null,
                    o.getTipoOcorrencia() != null ? o.getTipoOcorrencia().getTipoAbono() : null,
                    o.getDataInicio(),
                    o.getDataFim(),
                    o.getMinutosAbonoPorDia(),
                    o.getObservacao(),
                    o.getNomeArquivoAnexo(),
                    o.getCaminhoAnexo() != null && !o.getCaminhoAnexo().isBlank(),
                    o.getLancadoPor(),
                    o.getLancadoEm()
            );
        }
    }
}
