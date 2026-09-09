package com.example.projetoPBD.pontoCerto.dto;

import java.time.Instant;

public record ErroResponseDTO(
        Instant timeStamp,
        Integer status,
        String error,
        String message,
        String path
) {}