package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.TipoMovimentacaoBancoHoras;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BancoHorasDTO {

    public record RegistrarCompensacaoRequest(
            UUID funcionarioId,
            LocalDate dataReferencia,
            Integer minutos,
            String motivo
    ) {}

    public record ItemExtratoResponse(
            UUID id,
            LocalDate dataReferencia,
            TipoMovimentacaoBancoHoras tipo,
            String descricao,
            Integer minutos,
            Integer saldoAcumuladoMinutos,
            boolean periodoFechado,
            String registradoPor,
            LocalDateTime registradoEm
    ) {}

    public record ExtratoPeriodoResponse(
            UUID funcionarioId,
            String nomeFuncionario,
            String matricula,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer saldoInicialMinutos,
            Integer creditosPeriodoMinutos,
            Integer debitosPeriodoMinutos,
            Integer compensacoesPeriodoMinutos,
            Integer saldoFinalMinutos,
            List<ItemExtratoResponse> movimentacoes
    ) {}

    public record SaldoColaboradorEquipeResponse(
            UUID funcionarioId,
            String nomeFuncionario,
            String matricula,
            String cargo,
            Integer saldoAnteriorMinutos,
            Integer creditosPeriodoMinutos,
            Integer debitosPeriodoMinutos,
            Integer compensacoesPeriodoMinutos,
            Integer saldoFinalMinutos
    ) {}
}
