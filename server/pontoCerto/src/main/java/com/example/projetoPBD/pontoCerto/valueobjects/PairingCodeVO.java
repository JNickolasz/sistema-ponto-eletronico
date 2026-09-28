package com.example.projetoPBD.pontoCerto.valueobjects;

public record PairingCodeVO(String value) {

    public PairingCodeVO {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Código não pode ser vazio");
        }

    }

    @Override
    public String value() {
        return value;
    }
}
