package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.enums.RegraApuracaoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class RegraApuracaoDTO {

    public record Criar(
            @NotNull
            LocalDate inicioVigencia,

            @Positive
            @NotNull
            Integer limiteDiarioExtraMinutos,

            List<FaixaHoraExtraDTO.Criar> faixaHoraExtra,
            RegraAdicionalNoturnoDTO.Criar regraAdicionalNoturno

    ){}

    public record Response(
            UUID id,
            LocalDate inicioVigencia,
            LocalDate fimVigencia,
            RegraApuracaoStatus status,

            Integer limiteDiarioExtraMinutos,
            List<FaixaHoraExtraDTO.Response> faixaHoraExtra,
            RegraAdicionalNoturnoDTO.Response regraAdicionalNoturno

    ){ }

}

