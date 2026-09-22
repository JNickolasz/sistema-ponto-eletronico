package com.example.projetoPBD.pontoCerto.service.utils;

import java.time.Duration;
import java.time.LocalTime;

public class CalcularCargaDiariaMinutos {

    public static int calcularCargaDiariaMinutos(
            LocalTime entrada,
            LocalTime saida,
            LocalTime intervaloInicio,
            LocalTime intervaloFim
    ) {
        if (entrada == null || saida == null) {
            return 0;
        }

        long minutosBrutos = Duration.between(entrada, saida).toMinutes();
        if (minutosBrutos <= 0) {
            minutosBrutos += 24 * 60;
        }

        long minutosIntervalo = 0;
        if (intervaloInicio != null && intervaloFim != null) {
            minutosIntervalo = Duration.between(intervaloInicio, intervaloFim).toMinutes();
            if (minutosIntervalo <= 0) {
                minutosIntervalo += 24 * 60;
            }
        }

        long liquido = minutosBrutos - minutosIntervalo;
        return (int) Math.max(liquido, 0);
    }
}