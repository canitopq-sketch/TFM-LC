package com.lineacano.servidor;

import java.time.Instant;

public record UsuarioSesion(
        String token,
        String usuario,
        String nombreVisible,
        String rol,
        String idDni,
        Instant fechaCreacion
) {
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
