package com.lineacano.servidor;

import java.time.LocalDate;

/**
 * Resultado funcional de una consulta de disponibilidad de habitaciones.
 *
 * <p>Incluye el estado visual que consume el frontend y los contadores de
 * inventario utilizados para el resumen de reserva.</p>
 *
 * @param estado clave visual de disponibilidad
 * @param titulo titulo mostrado al usuario
 * @param mensaje descripcion funcional de la disponibilidad
 * @param totalHabitaciones numero total de habitaciones
 * @param habitacionesReservadas habitaciones ocupadas en el rango
 * @param habitacionesDisponibles habitaciones libres en el rango
 * @param numeroNoches duracion de la estancia
 * @param fechaEntrada fecha de llegada
 * @param fechaSalida fecha de salida
 * @param huespedes numero de huespedes solicitado
 */
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
    /**
     * Serializa el resultado al formato JSON esperado por la API.
     *
     * @return representacion JSON del resultado de disponibilidad
     */
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
