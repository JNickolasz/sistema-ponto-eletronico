package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;

import java.time.LocalDate;

public class RegraApuracaoDTO {

    public record Criar(

            LocalDate inicioVigencia,

            Integer limiteDiarioExtraMinutos,
            FaixaHoraExtra faixaHoraExtra,
            RegraAdicionalNoturno regraAdicionalNoturno

    ){}

}

