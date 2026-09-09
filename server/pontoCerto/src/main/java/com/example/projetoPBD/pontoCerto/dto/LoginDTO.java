package com.example.projetoPBD.pontoCerto.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "O usuário é obrigatório")
        String usuario,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}