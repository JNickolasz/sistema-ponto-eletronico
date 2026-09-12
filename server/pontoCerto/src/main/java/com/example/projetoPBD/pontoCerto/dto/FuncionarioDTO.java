package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.PerfilAcesso;

import java.util.UUID;

public class FuncionarioDTO {

    public record Criar(
            String nomeCompleto,
            String usuario,
            String senha,
            PerfilAcesso perfilAcesso
    ) {}

    public record Response(
            UUID id,
            String nomeCompleto,
            String usuario,
            PerfilAcesso perfilAcesso
    ) {}
}
