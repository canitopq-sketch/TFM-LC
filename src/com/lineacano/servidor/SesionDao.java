package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Acceso JDBC a las sesiones de usuario.
 */
public class SesionDao {
    private static final String SQL_CREAR_TABLA = """
            CREATE TABLE IF NOT EXISTS sesion_acceso (
              token varchar(80) NOT NULL,
              correo varchar(100) NOT NULL,
              fecha_creacion timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (token),
              KEY fk_sesion_acceso_usuario (correo),
              CONSTRAINT fk_sesion_acceso_usuario
                FOREIGN KEY (correo) REFERENCES usuario_acceso (correo)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;
    private static final String SQL_INSERTAR = """
            INSERT INTO sesion_acceso (token, correo, fecha_creacion)
            VALUES (?, ?, ?)
            """;
    private static final String SQL_BUSCAR = """
            SELECT sa.token, sa.fecha_creacion, ua.correo, ua.rol, ua.activo, ua.id_dni, c.nombre, c.apellido1
            FROM sesion_acceso sa
            INNER JOIN usuario_acceso ua ON ua.correo = sa.correo
            LEFT JOIN cliente c ON c.id_dni = ua.id_dni
            WHERE sa.token = ?
            """;
    private static final String SQL_ELIMINAR = "DELETE FROM sesion_acceso WHERE token = ?";
    private static final String SQL_ELIMINAR_CADUCADAS = "DELETE FROM sesion_acceso WHERE fecha_creacion < ?";

    private final BaseDeDatos baseDeDatos;

    public SesionDao(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
        if (baseDeDatos != null) {
            try {
                asegurarTabla();
            } catch (SQLException excepcion) {
                throw new IllegalStateException("No se pudo preparar la tabla de sesiones.", excepcion);
            }
        }
    }

    public void registrar(UsuarioSesion sesion) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_INSERTAR)) {
            sentencia.setString(1, sesion.token());
            sentencia.setString(2, sesion.usuario());
            sentencia.setTimestamp(3, Timestamp.from(sesion.fechaCreacion()));
            sentencia.executeUpdate();
        }
    }

    public Optional<DatosSesion> buscarPorToken(String token) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_BUSCAR)) {
            sentencia.setString(1, token);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    return Optional.empty();
                }
                Timestamp marcaTiempo = resultados.getTimestamp("fecha_creacion");
                Instant fechaCreacion = marcaTiempo == null ? Instant.now() : marcaTiempo.toInstant();
                return Optional.of(new DatosSesion(
                        resultados.getString("token"),
                        fechaCreacion,
                        resultados.getString("correo"),
                        resultados.getString("rol"),
                        resultados.getBoolean("activo"),
                        resultados.getString("id_dni"),
                        resultados.getString("nombre"),
                        resultados.getString("apellido1")
                ));
            }
        }
    }

    public boolean eliminar(String token) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_ELIMINAR)) {
            sentencia.setString(1, token);
            return sentencia.executeUpdate() > 0;
        }
    }

    public void eliminarCaducadas(Instant fechaLimite) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_ELIMINAR_CADUCADAS)) {
            sentencia.setTimestamp(1, Timestamp.from(fechaLimite));
            sentencia.executeUpdate();
        }
    }

    private void asegurarTabla() throws SQLException {
        try (Statement sentencia = conexion().createStatement()) {
            sentencia.executeUpdate(SQL_CREAR_TABLA);
        }
    }

    private Connection conexion() throws SQLException {
        if (baseDeDatos == null) {
            throw new IllegalStateException("El DAO no tiene una base de datos configurada.");
        }
        return baseDeDatos.obtenerConexion();
    }
}
