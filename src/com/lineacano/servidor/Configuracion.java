package com.lineacano.servidor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public record Configuracion(
        int puerto,
        Path raizWeb,
        String urlBaseDeDatos,
        String usuarioBaseDeDatos,
        String contrasenaBaseDeDatos
) {
    public static Configuracion cargar() throws IOException {
        // Se intenta leer el fichero local y, si falta algún valor, se usa el valor por defecto.
        Properties propiedades = new Properties();
        Path rutaPropiedades = Path.of("config", "application.properties");

        if (rutaPropiedades.toFile().exists()) {
            try (InputStream entrada = java.nio.file.Files.newInputStream(rutaPropiedades)) {
                propiedades.load(entrada);
            }
        }

        int puerto = Integer.parseInt(resolver(propiedades, "app.puerto", "8080"));
        Path raizWeb = Path.of(resolver(propiedades, "app.raizWeb", ".")).toAbsolutePath().normalize();
        String urlBaseDeDatos = resolver(
                propiedades,
                "bd.url",
                "jdbc:mysql://localhost:3306/linea_cano?serverTimezone=Europe/Madrid&useSSL=false&allowPublicKeyRetrieval=true"
        );
        String usuarioBaseDeDatos = resolver(propiedades, "bd.usuario", "root");
        String contrasenaBaseDeDatos = resolver(propiedades, "bd.contrasena", "");

        return new Configuracion(puerto, raizWeb, urlBaseDeDatos, usuarioBaseDeDatos, contrasenaBaseDeDatos);
    }

    private static String resolver(Properties propiedades, String clave, String valorPorDefecto) {
        String claveEntorno = clave.toUpperCase().replace('.', '_');
        String valorEntorno = System.getenv(claveEntorno);

        if (valorEntorno != null && !valorEntorno.isBlank()) {
            return valorEntorno;
        }

        return propiedades.getProperty(clave, valorPorDefecto);
    }
}
