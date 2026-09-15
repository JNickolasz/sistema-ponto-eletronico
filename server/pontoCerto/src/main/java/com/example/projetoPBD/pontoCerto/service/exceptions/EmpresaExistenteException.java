package com.example.projetoPBD.pontoCerto.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmpresaExistenteException extends RuntimeException {
    public EmpresaExistenteException(String message) {
        super(message);
    }
}
