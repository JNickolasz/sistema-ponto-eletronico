package com.example.projetoPBD.pontoCerto.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<String> handleException(AcessoNegadoException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<String> handleException(CredenciaisInvalidasException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(EmpresaCampoInvalidoException.class)
    public ResponseEntity<String> handleException(EmpresaCampoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(EmpresaExistenteException.class)
    public ResponseEntity<String> handleException(EmpresaExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(EmpresaNaoEncontradaException.class)
    public ResponseEntity<String> handleException(EmpresaNaoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(EquipamentoExistenteException.class)
    public ResponseEntity<String> handleException(EquipamentoExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(EquipamentoInvalidoException.class)
    public ResponseEntity<String> handleException(EquipamentoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(EquipamentoNaoEncontradoException.class)
    public ResponseEntity<String> handleException(EquipamentoNaoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<String> handleException(UsuarioDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<String> handleException(UsuarioNaoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(FiltroDeBuscaException.class)
    public ResponseEntity<String> handleException(FiltroDeBuscaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(CadastroRegraApuracaoInvalidoException.class)
    public ResponseEntity<String> handleException(CadastroRegraApuracaoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }


    private ResponseEntity<String> buildResponse(
            HttpStatus status,
            RuntimeException ex
    ) {
        return ResponseEntity
                .status(status)
                .body(ex.getMessage());
    }
}

