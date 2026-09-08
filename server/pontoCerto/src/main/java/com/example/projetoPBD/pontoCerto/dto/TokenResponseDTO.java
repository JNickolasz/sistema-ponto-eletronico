package com.example.projetoPBD.pontoCerto.dto;

public record TokenResponseDTO(
        String token,
        String tipo,
        String usuario,
        String perfil
) {}