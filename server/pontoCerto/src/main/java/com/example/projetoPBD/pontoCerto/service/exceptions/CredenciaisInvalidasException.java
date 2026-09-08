package com.example.projetoPBD.pontoCerto.service.exceptions;

public class CredenciaisInvalidasException extends RuntimeException{
    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
