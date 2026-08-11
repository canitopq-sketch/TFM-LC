package com.lineacano.servidor;

import java.time.LocalDate;

/**
 * Reglas de dominio compartidas por los servicios de la aplicacion.
 */
public final class ReglasNegocio {
    private ReglasNegocio() {
    }

    /**
     * Comprueba que un periodo de reserva tenga fechas coherentes.
     *
     * @param fechaEntrada fecha de llegada
     * @param fechaSalida fecha de salida
     * @throws IllegalArgumentException cuando falta una fecha o la salida no es posterior
     */
    public static void validarPeriodoReserva(LocalDate fechaEntrada, LocalDate fechaSalida) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new IllegalArgumentException("Las fechas de entrada y salida son obligatorias.");
        }

        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de llegada.");
        }
    }

    /**
     * Comprueba el numero de huespedes admitido por el prototipo.
     *
     * @param huespedes numero solicitado
     * @throws IllegalArgumentException cuando el valor queda fuera del rango permitido
     */
    public static void validarHuespedes(int huespedes) {
        if (huespedes < 1 || huespedes > 20) {
            throw new IllegalArgumentException("El numero de huespedes debe estar entre 1 y 20.");
        }
    }

    /**
     * Comprueba el formato de contrasena aceptado actualmente por la aplicacion.
     *
     * @param contrasena valor recibido del usuario
     * @throws IllegalArgumentException cuando no cumple la politica configurada
     */
    public static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        if (!contrasena.matches("[A-Za-z0-9]{1,10}")) {
            throw new IllegalArgumentException("La contraseña debe tener un maximo de 10 caracteres alfanumericos.");
        }
    }
}
