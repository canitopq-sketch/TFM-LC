package com.lineacano.servidor;

import java.time.Instant;

/**
 * Datos de una sesion autenticada y persistida en base de datos.
 *
 * @param token identificador opaco de sesion
 * @param usuario correo autenticado
 * @param nombreVisible nombre mostrado en el frontal privado
 * @param rol rol funcional de acceso
 * @param idDni identificador del cliente vinculado
 * @param fechaCreacion instante de creacion de la sesion
 */
public record UsuarioSesion(
        String token,
        String usuario,
        String nombreVisible,
        String rol,
        String idDni,
        Instant fechaCreacion
) {
    /**
     * Serializa la sesion al JSON devuelto al frontend tras login o validacion.
     *
     * @return representacion JSON de la sesion activa
     */
    public String aJson() {
        return """
                {
                  "token":"%s",
                  "usuario":"%s",
                  "nombreVisible":"%s",
                  "rol":"%s",
                  "idDni":"%s"
                }
                """.formatted(
                UtilJson.escapar(token),
                UtilJson.escapar(usuario),
                UtilJson.escapar(nombreVisible),
                UtilJson.escapar(rol),
                UtilJson.escapar(idDni)
        );
    }
}
