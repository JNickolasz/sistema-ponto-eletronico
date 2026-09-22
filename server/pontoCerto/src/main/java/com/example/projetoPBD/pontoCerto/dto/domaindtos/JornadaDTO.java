package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import java.util.List;
import java.util.UUID;

public class JornadaDTO {

    public record Criar(
            String nome,
            Integer toleranciaMinutos,
            List<JornadaDiaDTO.Criar> dias
    ) {}

    public record Response (
            UUID id,
            UUID empresa_id,
            String nome,
            Integer toleranciaMinutos,
            Integer cargaHorariaSemanaMinutos,
            List<JornadaDiaDTO.Response> dias
    ) {}
}
