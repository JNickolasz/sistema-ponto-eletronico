package com.example.projetoPBD.pontoCerto.domain.enums;

public enum DiaSemana {
    DOMINGO,
    SEGUNDA,
    TERÇA,
    QUARTA,
    QUINTA,
    SEXTA,
    SÁBADO;

    public static DiaSemana from(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> DOMINGO;
            case MONDAY -> SEGUNDA;
            case TUESDAY -> TERÇA;
            case WEDNESDAY -> QUARTA;
            case THURSDAY -> QUINTA;
            case FRIDAY -> SEXTA;
            case SATURDAY -> SÁBADO;
        };
    }
}
