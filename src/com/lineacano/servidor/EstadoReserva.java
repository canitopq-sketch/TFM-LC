package com.lineacano.servidor;

import java.time.LocalDate;

/**
 * Datos necesarios para decidir si una reserva puede cancelarse.
 *
 * @param idReserva identificador de la reserva
 * @param fechaEntrada fecha de llegada
 * @param estado estado actual
 */
public record EstadoReserva(int idReserva, LocalDate fechaEntrada, String estado) {
}
