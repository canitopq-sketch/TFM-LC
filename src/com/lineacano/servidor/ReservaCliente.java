package com.lineacano.servidor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de una reserva recuperada para el historial de un cliente.
 *
 * @param idReserva identificador de la reserva
 * @param fechaEntrada fecha de llegada
 * @param fechaSalida fecha de salida
 * @param estado estado actual
 * @param importeTotal importe calculado
 * @param habitacionAsignada habitacion vinculada, si existe
 */
public record ReservaCliente(
        int idReserva,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String estado,
        BigDecimal importeTotal,
        Integer habitacionAsignada
) {
}
