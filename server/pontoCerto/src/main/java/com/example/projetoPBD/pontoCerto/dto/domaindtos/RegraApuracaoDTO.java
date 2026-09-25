package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import com.example.projetoPBD.pontoCerto.domain.enums.RegraApuracaoStatus;

import java.time.LocalDate;
import java.util.UUID;

public class RegraApuracaoDTO {

    public record Criar(

            LocalDate inicioVigencia,

            Integer limiteDiarioExtraMinutos,
            FaixaHoraExtra faixaHoraExtra,
            RegraAdicionalNoturno regraAdicionalNoturno

    ){}

    public record Response(
            UUID id,
            LocalDate inicioVigencia,
            LocalDate fimVigencia,
            RegraApuracaoStatus status,

            Integer limiteDiarioExtraMinutos,
            FaixaHoraExtra faixaHoraExtra,
            RegraAdicionalNoturno regraAdicionalNoturno



    ){

    }

}

