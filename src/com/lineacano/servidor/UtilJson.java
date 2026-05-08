package com.lineacano.servidor;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidades minimas para construir y leer JSON simple sin dependencias externas.
 */
public final class UtilJson {
    private UtilJson() {
    }

    /**
     * Escapa caracteres conflictivos para insertar valores seguros en cadenas JSON.
     *
     * @param valor texto original
     * @return texto escapado, o cadena vacia si el valor es nulo
     */
    public static String escapar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Interpreta un objeto JSON plano con claves y valores de texto.
     *
     * @param json cuerpo recibido por la API
     * @return mapa de claves y valores extraidos
     */
    public static Map<String, String> analizarObjetoSimple(String json) {
        Map<String, String> valores = new HashMap<>();

        if (json == null) {
            return valores;
        }

        String contenido = json.trim();
        if (contenido.isEmpty()) {
            return valores;
        }

        if (contenido.startsWith("{")) {
            contenido = contenido.substring(1);
        }

        if (contenido.endsWith("}")) {
            contenido = contenido.substring(0, contenido.length() - 1);
        }

        for (String fragmento : contenido.split(",")) {
            String[] partes = fragmento.split(":", 2);
            if (partes.length != 2) {
                continue;
            }

            String clave = limpiarToken(partes[0]);
            String valor = limpiarToken(partes[1]);
            if (!clave.isBlank()) {
                valores.put(clave, valor);
            }
        }

        return valores;
    }

    private static String limpiarToken(String valor) {
        String resultado = valor == null ? "" : valor.trim();

        if (resultado.startsWith("\"")) {
            resultado = resultado.substring(1);
        }

        if (resultado.endsWith("\"")) {
            resultado = resultado.substring(0, resultado.length() - 1);
        }

        return resultado
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }
}
