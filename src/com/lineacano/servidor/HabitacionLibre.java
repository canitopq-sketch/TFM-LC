package com.lineacano.servidor;

/**
 * Datos basicos de una habitacion disponible para una reserva.
 *
 * @param numeroHabitacion identificador de la habitacion
 * @param precioNoche precio de una noche
 */
public record HabitacionLibre(int numeroHabitacion, double precioNoche) {
}
