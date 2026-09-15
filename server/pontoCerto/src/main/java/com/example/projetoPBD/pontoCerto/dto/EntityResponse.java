package com.example.projetoPBD.pontoCerto.dto;

public record EntityResponse<T>(
        T data,
        String message
) {
}
