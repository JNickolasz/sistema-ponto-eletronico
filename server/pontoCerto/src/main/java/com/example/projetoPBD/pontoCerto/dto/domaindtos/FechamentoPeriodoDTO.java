package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.AcaoFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FechamentoPeriodoDTO {

    public record FecharRequest(
            String competencia,
            Boolean confirmarComPendencias,
            String observacao
    ) {}

    public record ReabrirRequest(
            String competencia,
            String motivo
    ) {}

    public record HistoricoItemResponse(
            UUID id,
            UUID fechamentoPeriodoId,
            String competencia,
            AcaoFechamentoPeriodo acao,
            String autor,
            UUID autorId,
            LocalDateTime dataHora,
            String motivo
    ) {}

    public record PreviaResponse(
            UUID empresaId,
            String competencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            StatusFechamentoPeriodo statusAtual,
            boolean periodoFechado,
            int totalPendenciasAbertas,
            List<InconsistenciaDTO.Response> pendenciasAbertas
    ) {}

    public record FechamentoDetalheResponse(
            UUID id,
            UUID empresaId,
            String competencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            StatusFechamentoPeriodo status,
            String fechadoPor,
            UUID fechadoPorId,
            LocalDateTime fechadoEm,
            int quantidadePendenciasAbertas,
            String ressalvaOuAviso,
            List<HistoricoItemResponse> historico
    ) {}
}
