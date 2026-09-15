package com.example.projetoPBD.pontoCerto.dto;

public class PainelDTO {

    public record AcessoLiberado(
            String usuarioLogado,
            String permissoes
    ){}

    public record AcessoEspelho(
            String espelhoId,
            String donoDoEspelho
    ){}


}
