package com.lineacano.servidor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ServicioAutenticacion {
    private static final String SQL_INICIO_SESION = """
            SELECT ua.correo, ua.contrasena_hash, ua.rol, ua.activo, ua.id_dni, c.nombre, c.apellido1
            FROM usuario_acceso ua
            LEFT JOIN cliente c ON c.id_dni = ua.id_dni
            WHERE ua.correo = ?
            """;

    private final BaseDeDatos baseDeDatos;
    private final ConcurrentMap<String, UsuarioSesion> sesiones = new ConcurrentHashMap<>();

    public ServicioAutenticacion(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
    }

    public UsuarioSesion iniciarSesion(String usuario, String contrasena) throws SQLException {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }

        if (!contrasena.matches("[A-Za-z0-9]{1,12}")) {
            throw new IllegalArgumentException("La contraseña debe tener un maximo de 12 caracteres alfanumericos.");
        }

        // Se normaliza el correo para evitar diferencias por mayúsculas o espacios.
        String correoNormalizado = usuario.trim().toLowerCase();
        String hashContrasena = Contrasenas.sha256(contrasena);

        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_INICIO_SESION)) {
            sentencia.setString(1, correoNormalizado);

            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
                }

                boolean activo = resultados.getBoolean("activo");
                if (!activo) {
                    throw new IllegalArgumentException("El usuario no esta activo.");
                }

                String hashEsperado = resultados.getString("contrasena_hash");
                if (!hashContrasena.equalsIgnoreCase(hashEsperado)) {
                    throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
                }

                String nombreVisible = construirNombreVisible(
                        resultados.getString("nombre"),
                        resultados.getString("apellido1"),
                        correoNormalizado
                );

                String rolNormalizado = normalizarRol(resultados.getString("rol"));

                // La sesión se guarda en memoria porque el objetivo actual es un prototipo funcional.
                UsuarioSesion sesion = new UsuarioSesion(
                        UUID.randomUUID().toString(),
                        correoNormalizado,
                        nombreVisible,
                        rolNormalizado,
                        resultados.getString("id_dni"),
                        Instant.now()
                );

                sesiones.put(sesion.token(), sesion);
                return sesion;
            }
        } catch (SQLException excepcion) {
            if (excepcion.getMessage() != null && excepcion.getMessage().contains("usuario_acceso")) {
                throw new IllegalStateException(
                        "Falta la tabla usuario_acceso. Ejecuta la importacion con sql/auth_schema.sql."
                );
            }

            throw excepcion;
        }
    }

    public Optional<UsuarioSesion> buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(sesiones.get(token));
    }

    private String construirNombreVisible(String nombre, String apellido1, String valorPorDefecto) {
        String nombreCompuesto = ((nombre == null ? "" : nombre.trim()) + " " + (apellido1 == null ? "" : apellido1.trim())).trim();
        return nombreCompuesto.isBlank() ? valorPorDefecto : nombreCompuesto;
    }

    private String normalizarRol(String rolBruto) {
        if (rolBruto == null) {
            return "publico";
        }

        return switch (rolBruto.trim().toLowerCase()) {
            case "member", "registrado" -> "registrado";
            case "master", "maestro", "comercial" -> "maestro";
            default -> rolBruto.trim().toLowerCase();
        };
    }
}
