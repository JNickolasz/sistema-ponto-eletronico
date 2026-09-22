package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import java.time.LocalTime;
import java.util.UUID;

public class EscalaDTO {

    public record Criar(
        String nome,
        Integer diasTrabalho,
        Integer diasFolga,
        LocalTime horaEntrada,
        LocalTime horaSaida,
        LocalTime intervaloInicio,
        LocalTime intervaloFim,
        Integer toleranciaMinutos
    ) {}

    public record Response (
        UUID id,
        UUID empresa_id,
        String nome,
        Integer diasTrabalho,
        Integer diasFolga,
        LocalTime horaEntrada,
        LocalTime horaSaida,
        LocalTime intervaloInicio,
        LocalTime intervaloFim,
        Integer toleranciaMinutos,
        Integer cargaDiariaMinutos
    ) {}
}
