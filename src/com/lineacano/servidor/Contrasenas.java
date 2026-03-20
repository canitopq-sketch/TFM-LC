package com.lineacano.servidor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Contrasenas {
    private Contrasenas() {
    }

    public static String sha256(String textoPlano) {
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] hash = resumen.digest(textoPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder constructor = new StringBuilder(hash.length * 2);

            for (byte valor : hash) {
                constructor.append(String.format("%02x", valor));
            }

            return constructor.toString();
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("SHA-256 no disponible", excepcion);
        }
    }
}
