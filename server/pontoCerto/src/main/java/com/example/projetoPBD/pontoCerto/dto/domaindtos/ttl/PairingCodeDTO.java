package com.example.projetoPBD.pontoCerto.dto.domaindtos.ttl;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusPairingCode;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EstacaoDTO;

import java.time.Instant;
import java.util.UUID;

public class PairingCodeDTO {

    public record Response(

            UUID id,
            String pairingCode,
            StatusPairingCode status,
            Instant expiresAt,
            EstacaoDTO.Response estacao

    ){}

}
