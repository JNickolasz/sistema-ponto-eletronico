package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public class InconsistenciaDTO {

    public record Response(
            UUID id,
            TipoInconsistencia tipo,
            StatusInconsistencia situacao,
            LocalDate dataReferencia,
            LocalDateTime dataHoraOrigem,
            String descricaoOrigem,
            UUID funcionarioId,
            String funcionarioNome,
            String funcionarioMatricula,
            UUID equipamentoId,
            String equipamentoIdentificacao,
            String equipamentoCodigo,
            String identificadorRecebido,
            String nomeArquivo,
            UUID batidaOrigemId,
            UUID batidaSecundariaId,
            AcaoTratamento acaoTomada,
            String tratadoPor,
            LocalDateTime tratadoEm,
            String motivoTratamento,
            String detalheResolucao
    ){}

    public record TratarRequest(
            @NotNull(message = "A ação de tratamento é obrigatória.")
            AcaoTratamento acao,
            @NotBlank(message = "O motivo do tratamento é obrigatório.")
            String motivo,
            UUID funcionarioId,
            UUID equipamentoId,
            LocalTime horarioAjuste,
            TipoRegistro tipoRegistroAjuste
    ) {}

    public record PainelContagemResponse(
            long totalAbertas,
            long totalResolvidas,
            Map<TipoInconsistencia, Long> abertasPorTipo
    ) {}

    public record FechamentoRequest(
            @NotBlank(message = "O mês de referência (YYYY-MM) é obrigatório.")
            String mesReferencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            Boolean confirmarComPendencias
    ) {}

    public record FechamentoResponse(
            UUID id,
            String mesReferencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            LocalDateTime dataFechamento,
            String fechadoPor,
            StatusFechamentoPeriodo status,
            int pendenciasAbertasNoFechamento,
            String avisoPendencias,
            boolean fechadoComRessalva
    ) {}
}
