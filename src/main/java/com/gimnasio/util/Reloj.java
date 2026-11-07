package com.gimnasio.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fecha y hora de Colombia. Un servidor en la nube suele tener hora UTC (5 horas
 * adelantada), así que no usamos LocalDate.now() a secas.
 */
public final class Reloj {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Reloj() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}