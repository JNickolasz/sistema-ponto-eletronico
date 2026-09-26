package com.example.projetoPBD.pontoCerto.dto;

import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.enums.RegraApuracaoStatus;

import java.time.LocalDate;

public record RegraApuracaoConsulta(

        RegraApuracao regraApuracao,
        LocalDate proximaVigencia,
        String status

) {
}
