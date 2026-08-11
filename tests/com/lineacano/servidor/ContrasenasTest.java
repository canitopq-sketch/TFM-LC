package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ContrasenasTest {
    @Test
    void generaSha256Determinista() {
        assertEquals(
                "09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9",
                Contrasenas.sha256("cliente123")
        );
        assertNotEquals(Contrasenas.sha256("cliente123"), Contrasenas.sha256("otraClave"));
    }
}
