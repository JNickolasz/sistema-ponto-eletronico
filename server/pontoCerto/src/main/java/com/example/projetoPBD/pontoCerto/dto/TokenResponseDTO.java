package com.example.projetoPBD.pontoCerto.dto;

import org.hibernate.boot.models.annotations.internal.InstantiatorAnnotation;

import java.time.Instant;

public record TokenResponseDTO(
        String token,
        String tipo,
        String usuario,
        String perfil
) {}