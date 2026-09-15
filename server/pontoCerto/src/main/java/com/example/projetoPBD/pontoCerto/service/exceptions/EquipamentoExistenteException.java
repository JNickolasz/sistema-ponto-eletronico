package com.example.projetoPBD.pontoCerto.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EquipamentoExistenteException extends RuntimeException {
    public EquipamentoExistenteException(String message) {
        super(message);
    }
}
