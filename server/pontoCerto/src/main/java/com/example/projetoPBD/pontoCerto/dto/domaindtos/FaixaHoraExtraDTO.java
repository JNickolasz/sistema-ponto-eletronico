package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class FaixaHoraExtraDTO {

    public record Criar(

            @NotNull
            @Positive
            Integer ordem,

            @NotNull
            @Positive
            Integer duracaoMinutos,

            @NotNull
            @DecimalMin("1.00")
            @DecimalMax("100.00")
            BigDecimal percentual

    ){}


    public record Response(

            UUID id,
            Integer ordem,
            Integer duracaoMinutos,
            BigDecimal percentual

    ){ }

}
