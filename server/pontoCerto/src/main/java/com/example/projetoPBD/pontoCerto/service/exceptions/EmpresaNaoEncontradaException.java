package com.example.projetoPBD.pontoCerto.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmpresaNaoEncontradaException extends RuntimeException {
    public EmpresaNaoEncontradaException(String message) {
        super(message);
    }
}
