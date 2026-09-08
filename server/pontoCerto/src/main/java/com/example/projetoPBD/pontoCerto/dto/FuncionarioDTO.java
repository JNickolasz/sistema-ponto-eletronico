package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.PerfilAcesso;

public class FuncionarioDTO {

    public record Criar(
            String nomeCompleto,
            String usuario,
            String senha,
            PerfilAcesso.Perfil perfilAcesso
    ) {}

    public record Response(
            Long id,
            String nomeCompleto,
            String usuario,
            PerfilAcesso.Perfil perfilAcesso
    ) {}
}
