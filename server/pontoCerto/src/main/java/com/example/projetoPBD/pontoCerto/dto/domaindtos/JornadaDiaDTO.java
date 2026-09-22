package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;

import java.time.LocalTime;
import java.util.UUID;

public class JornadaDiaDTO {

    public record Criar(
        DiaSemana diaSemana,
        boolean diaTrabalho,
        LocalTime horaEntrada,
        LocalTime horaSaida,
        LocalTime intervaloInicio,
        LocalTime intervaloFim
    ) {}

    public record Response (
        UUID id,
        DiaSemana diaSemana,
        boolean diaTrabalho,
        LocalTime horaEntrada,
        LocalTime horaSaida,
        LocalTime intervaloInicio,
        LocalTime intervaloFim,
        Integer cargaDiariaMinutos
    ) {}
}
