package com.lineacano.servidor;

import java.time.LocalDate;

public record ResultadoDisponibilidad(
        String estado,
        String titulo,
        String mensaje,
        int totalHabitaciones,
        int habitacionesReservadas,
        int habitacionesDisponibles,
        int numeroNoches,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int huespedes
) {
    public String aJson() {
        return """
                {
                  "estado":"%s",
                  "titulo":"%s",
                  "mensaje":"%s",
                  "totalHabitaciones":%d,
                  "habitacionesReservadas":%d,
                  "habitacionesDisponibles":%d,
                  "numeroNoches":%d,
                  "fechaEntrada":"%s",
                  "fechaSalida":"%s",
                  "huespedes":%d
                }
                """.formatted(
                UtilJson.escapar(estado),
                UtilJson.escapar(titulo),
                UtilJson.escapar(mensaje),
                totalHabitaciones,
                habitacionesReservadas,
                habitacionesDisponibles,
                numeroNoches,
                fechaEntrada,
                fechaSalida,
                huespedes
        );
    }
}
