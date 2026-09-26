package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.UUID;

public class RegraAdicionalNoturnoDTO {

    public record Criar(

            @NotNull
            Time horaInicio,

            @NotNull
            Time horaFim,

            @NotNull
            @Positive
            Integer duracaoHoraNoturnaSegundos,

            @NotNull
            @DecimalMin("0.00")
            @DecimalMax("100.00")
            BigDecimal percentual
    ){}

    public record Response(

            UUID id,
            Time horaInicio,
            Time horaFim,
            Integer duracaoHoraNoturnaSegundos,
            BigDecimal percentual

    ){ }

}
