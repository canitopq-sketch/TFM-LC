package com.lineacano.servidor;

import java.time.Instant;

/**
 * Datos de una sesion y del usuario al que pertenece.
 *
 * @param token identificador de sesion
 * @param fechaCreacion instante de creacion
 * @param correo correo del usuario
 * @param rol perfil del usuario
 * @param activo indica si el usuario sigue activo
 * @param idDni cliente vinculado
 * @param nombre nombre del cliente
 * @param apellido primer apellido
 */
public record DatosSesion(
        String token,
        Instant fechaCreacion,
        String correo,
        String rol,
        boolean activo,
        String idDni,
        String nombre,
        String apellido
) {
}
