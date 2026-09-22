package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class HorarioPrevistoDTO {

    public record Response (
            UUID funcionarioId,
            LocalDate data,
            DiaSemana diaSemana,
            boolean diaTrabalho,
            TipoRegime tipoRegime,
            String descricaoRegime,
            LocalTime horaEntrada,
            LocalTime horaSaida,
            LocalTime intervaloInicio,
            LocalTime intervaloFim,
            Integer toleranciaMinutos,
            Integer cargaDiariaMinutos,
            String mensagem
    ) {}
}
