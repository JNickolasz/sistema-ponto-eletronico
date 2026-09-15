package com.example.projetoPBD.pontoCerto.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EquipamentoInvalidoException extends RuntimeException {
    public EquipamentoInvalidoException(String message) {
        super(message);
    }
}
