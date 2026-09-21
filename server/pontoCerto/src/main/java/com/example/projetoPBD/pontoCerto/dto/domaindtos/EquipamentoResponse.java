package com.example.projetoPBD.pontoCerto.dto.domaindtos;

public sealed interface EquipamentoResponse
permits RelogioDTO.Response, EstacaoDTO.Response{
}
