package com.lineacano.servidor;

/**
 * Datos de un usuario recuperados para comprobar sus credenciales.
 *
 * @param correo correo de acceso
 * @param contrasenaHash contrasena almacenada como hash
 * @param rol perfil del usuario
 * @param activo indica si puede acceder
 * @param idDni cliente vinculado, si existe
 * @param nombre nombre del cliente
 * @param apellido primer apellido
 */
public record DatosAccesoUsuario(
        String correo,
        String contrasenaHash,
        String rol,
        boolean activo,
        String idDni,
        String nombre,
        String apellido
) {
}
