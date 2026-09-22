package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegime;

import java.time.LocalDate;
import java.util.UUID;

public class RegimeTrabalhoDTO {

    public record Vincular (
        UUID funcionarioId,
        TipoRegime tipoRegime,
        UUID jornadaId,
        UUID escalaId,
        LocalDate dataInicioVigencia,
        LocalDate dataFimVigencia
    ) {}

    public record Response (
        UUID id,
        UUID funcionarioId,
        String funcionarioNome,
        TipoRegime tipoRegime,
        UUID jornadaId,
        String jornadaNome,
        UUID escalaId,
        String escalaNome,
        LocalDate dataInicioVigencia,
        LocalDate dataFimVigencia
    ) {}
}
