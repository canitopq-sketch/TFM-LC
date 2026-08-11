package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UtilJsonTest {
    @Test
    void escapaCaracteresEspeciales() {
        assertEquals("Linea \\\"Cano\\\"\\nDehesa", UtilJson.escapar("Linea \"Cano\"\nDehesa"));
    }

    @Test
    void analizaObjetoJsonPlano() {
        Map<String, String> resultado = UtilJson.analizarObjetoSimple(
                "{\"usuario\":\"cliente@lineacano.com\",\"contrasena\":\"cliente123\"}"
        );

        assertEquals("cliente@lineacano.com", resultado.get("usuario"));
        assertEquals("cliente123", resultado.get("contrasena"));
    }
}
