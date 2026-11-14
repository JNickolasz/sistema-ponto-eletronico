package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApuracaoDiariaDTO {

    public record ParMarcacao(
            LocalTime entrada,
            LocalTime saida,
            Integer duracaoMinutos,
            String origemEntrada,
            String origemSaida
    ) {}

    public record RecalcularRequest(
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {}

    public record Response(
            UUID id,
            UUID funcionarioId,
            String nomeFuncionario,
            String matricula,
            LocalDate dataReferencia,
            StatusApuracaoDiaria status,
            Integer minutosPrevistos,
            Integer minutosTrabalhados,
            Integer minutosIntervalo,
            Integer minutosAtraso,
            Integer minutosSaidaAntecipada,
            Integer minutosAdicionalNoturno,
            Integer minutosFalta,
            Integer minutosHoraExtraTotal,
            List<ParMarcacao> pares,
            Map<String, Integer> horasExtrasPorFaixa,
            LocalDateTime calculadoEm
    ) {}

    public record ResumoPeriodoResponse(
            UUID funcionarioId,
            String nomeFuncionario,
            LocalDate dataInicio,
            LocalDate dataFim,
            int totalDiasApurados,
            int totalMinutosPrevistos,
            int totalMinutosTrabalhados,
            int totalMinutosIntervalo,
            int totalMinutosAtraso,
            int totalMinutosSaidaAntecipada,
            int totalMinutosAdicionalNoturno,
            int totalMinutosFalta,
            int totalMinutosHoraExtra,
            List<Response> dias
    ) {}
}
