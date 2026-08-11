package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Acceso JDBC a clientes, credenciales y datos profesionales HORECA.
 */
public class UsuarioDao {
    private static final String SQL_BUSCAR_USUARIO = """
            SELECT ua.correo, ua.contrasena_hash, ua.rol, ua.activo, ua.id_dni, c.nombre, c.apellido1
            FROM usuario_acceso ua
            LEFT JOIN cliente c ON c.id_dni = ua.id_dni
            WHERE ua.correo = ?
            """;
    private static final String SQL_EXISTE_CORREO = "SELECT 1 FROM usuario_acceso WHERE correo = ?";
    private static final String SQL_EXISTE_DOCUMENTO = "SELECT 1 FROM cliente WHERE id_dni = ?";
    private static final String SQL_INSERTAR_CLIENTE = """
            INSERT INTO cliente (id_dni, nombre, apellido1, correo, telefono)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SQL_INSERTAR_USUARIO = """
            INSERT INTO usuario_acceso (correo, contrasena_hash, rol, activo, id_dni)
            VALUES (?, ?, ?, 1, ?)
            """;
    private static final String SQL_INSERTAR_CLIENTE_HORECA = """
            INSERT INTO cliente_horeca (id_dni, empresa, cif, correo_profesional, telefono_contacto)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SQL_CREAR_TABLA_CLIENTE_HORECA = """
            CREATE TABLE IF NOT EXISTS cliente_horeca (
              id_dni varchar(20) NOT NULL,
              empresa varchar(150) NOT NULL,
              cif varchar(20) NOT NULL,
              correo_profesional varchar(100) NOT NULL,
              telefono_contacto varchar(20) DEFAULT NULL,
              fecha_alta timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id_dni),
              UNIQUE KEY uq_cliente_horeca_cif (cif),
              UNIQUE KEY uq_cliente_horeca_correo (correo_profesional),
              CONSTRAINT fk_cliente_horeca_cliente
                FOREIGN KEY (id_dni) REFERENCES cliente (id_dni)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;

    private final BaseDeDatos baseDeDatos;

    /**
     * Crea el DAO con el gestor de conexiones compartido.
     *
     * @param baseDeDatos gestor de la conexion JDBC
     */
    public UsuarioDao(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
    }

    /**
     * Busca las credenciales y los datos basicos de un usuario.
     *
     * @param correo correo normalizado de acceso
     * @return datos encontrados, o vacio si el correo no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<DatosAccesoUsuario> buscarPorCorreo(String correo) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_BUSCAR_USUARIO)) {
            sentencia.setString(1, correo);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    return Optional.empty();
                }
                return Optional.of(new DatosAccesoUsuario(
                        resultados.getString("correo"),
                        resultados.getString("contrasena_hash"),
                        resultados.getString("rol"),
                        resultados.getBoolean("activo"),
                        resultados.getString("id_dni"),
                        resultados.getString("nombre"),
                        resultados.getString("apellido1")
                ));
            }
        }
    }

    /**
     * Comprueba si un correo ya esta registrado.
     *
     * @param correo correo normalizado
     * @return {@code true} si existe un usuario con ese correo
     * @throws SQLException si falla la consulta
     */
    public boolean existeCorreo(String correo) throws SQLException {
        return existe(SQL_EXISTE_CORREO, correo);
    }

    /**
     * Comprueba si un documento ya pertenece a un cliente.
     *
     * @param documento DNI o documento normalizado
     * @return {@code true} si existe un cliente con ese documento
     * @throws SQLException si falla la consulta
     */
    public boolean existeDocumento(String documento) throws SQLException {
        return existe(SQL_EXISTE_DOCUMENTO, documento);
    }

    /**
     * Guarda un cliente y su cuenta de acceso en una unica transaccion.
     *
     * @param documento DNI o documento identificativo
     * @param nombre nombre del cliente
     * @param apellido primer apellido
     * @param correo correo de acceso
     * @param telefono telefono opcional
     * @param hashContrasena resumen de la contrasena
     * @param rol rol asignado a la cuenta
     * @throws SQLException si falla alguna insercion o la transaccion
     */
    public void registrarCliente(
            String documento,
            String nombre,
            String apellido,
            String correo,
            String telefono,
            String hashContrasena,
            String rol
    ) throws SQLException {
        Connection conexion = conexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            insertarCliente(conexion, documento, nombre, apellido, correo, telefono);
            insertarUsuario(conexion, correo, hashContrasena, rol, documento);
            conexion.commit();
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    /**
     * Guarda un cliente, su cuenta y su ficha profesional HORECA.
     *
     * @param documento DNI del contacto
     * @param nombre nombre del contacto
     * @param apellido primer apellido del contacto
     * @param empresa nombre de la empresa
     * @param cif identificador fiscal de la empresa
     * @param correo correo profesional de acceso
     * @param telefono telefono opcional
     * @param hashContrasena resumen de la contrasena
     * @throws SQLException si falla alguna insercion o la transaccion
     */
    public void registrarClienteHoreca(
            String documento,
            String nombre,
            String apellido,
            String empresa,
            String cif,
            String correo,
            String telefono,
            String hashContrasena
    ) throws SQLException {
        Connection conexion = conexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            try (Statement sentencia = conexion.createStatement()) {
                sentencia.executeUpdate(SQL_CREAR_TABLA_CLIENTE_HORECA);
            }
            insertarCliente(conexion, documento, nombre, apellido, correo, telefono);
            insertarUsuario(conexion, correo, hashContrasena, "horeca", documento);
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_CLIENTE_HORECA)) {
                sentencia.setString(1, documento);
                sentencia.setString(2, empresa);
                sentencia.setString(3, cif);
                sentencia.setString(4, correo);
                sentencia.setString(5, telefono);
                sentencia.executeUpdate();
            }
            conexion.commit();
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    private boolean existe(String sql, String valor) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(sql)) {
            sentencia.setString(1, valor);
            try (ResultSet resultados = sentencia.executeQuery()) {
                return resultados.next();
            }
        }
    }

    private void insertarCliente(
            Connection conexion,
            String documento,
            String nombre,
            String apellido,
            String correo,
            String telefono
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_CLIENTE)) {
            sentencia.setString(1, documento);
            sentencia.setString(2, nombre);
            sentencia.setString(3, apellido);
            sentencia.setString(4, correo);
            sentencia.setString(5, telefono);
            sentencia.executeUpdate();
        }
    }

    private void insertarUsuario(
            Connection conexion,
            String correo,
            String hashContrasena,
            String rol,
            String documento
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_USUARIO)) {
            sentencia.setString(1, correo);
            sentencia.setString(2, hashContrasena);
            sentencia.setString(3, rol);
            sentencia.setString(4, documento);
            sentencia.executeUpdate();
        }
    }

    private Connection conexion() throws SQLException {
        if (baseDeDatos == null) {
            throw new IllegalStateException("El DAO no tiene una base de datos configurada.");
        }
        return baseDeDatos.obtenerConexion();
    }
}
