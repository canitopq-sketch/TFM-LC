package com.lineacano.servidor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso JDBC a las tablas de pedidos y sus lineas.
 */
public class PedidoDao {
    private static final String SQL_CREAR_TABLA_PEDIDO = """
            CREATE TABLE IF NOT EXISTS pedido_producto (
              id_pedido INT NOT NULL AUTO_INCREMENT,
              id_dni VARCHAR(20) NOT NULL,
              canal VARCHAR(20) NOT NULL,
              estado VARCHAR(40) NOT NULL DEFAULT 'Solicitud recibida',
              importe_total DECIMAL(10,2) NOT NULL DEFAULT 0,
              fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id_pedido),
              KEY fk_pedido_producto_cliente (id_dni),
              CONSTRAINT fk_pedido_producto_cliente
                FOREIGN KEY (id_dni) REFERENCES cliente (id_dni)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;
    private static final String SQL_CREAR_TABLA_LINEA = """
            CREATE TABLE IF NOT EXISTS pedido_producto_linea (
              id_linea INT NOT NULL AUTO_INCREMENT,
              id_pedido INT NOT NULL,
              producto_codigo VARCHAR(40) NOT NULL,
              producto_nombre VARCHAR(120) NOT NULL,
              categoria VARCHAR(60) NOT NULL,
              cantidad INT NOT NULL,
              precio_unitario DECIMAL(10,2) NOT NULL,
              subtotal DECIMAL(10,2) NOT NULL,
              PRIMARY KEY (id_linea),
              KEY fk_pedido_producto_linea_pedido (id_pedido),
              CONSTRAINT fk_pedido_producto_linea_pedido
                FOREIGN KEY (id_pedido) REFERENCES pedido_producto (id_pedido)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;
    private static final String SQL_INSERTAR_PEDIDO = """
            INSERT INTO pedido_producto (id_dni, canal, estado, importe_total)
            VALUES (?, ?, 'Solicitud recibida', ?)
            """;
    private static final String SQL_INSERTAR_LINEA = """
            INSERT INTO pedido_producto_linea
              (id_pedido, producto_codigo, producto_nombre, categoria, cantidad, precio_unitario, subtotal)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_PEDIDOS_CLIENTE = """
            SELECT id_pedido, canal, estado, importe_total, fecha_creacion
            FROM pedido_producto
            WHERE id_dni = ?
            ORDER BY fecha_creacion DESC, id_pedido DESC
            """;
    private static final String SQL_LINEAS_PEDIDO = """
            SELECT producto_codigo, producto_nombre, categoria, cantidad, precio_unitario, subtotal
            FROM pedido_producto_linea
            WHERE id_pedido = ?
            ORDER BY id_linea
            """;

    private final BaseDeDatos baseDeDatos;

    /**
     * Crea el DAO y prepara las tablas que ya utiliza la aplicacion.
     *
     * @param baseDeDatos gestor de la conexion JDBC
     */
    public PedidoDao(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
        if (baseDeDatos != null) {
            try {
                asegurarTablas();
            } catch (SQLException excepcion) {
                throw new IllegalStateException("No se pudieron preparar las tablas de pedidos.", excepcion);
            }
        }
    }

    /**
     * Guarda un pedido y todas sus lineas en una unica transaccion.
     *
     * @param idDni identificador del cliente que realiza el pedido
     * @param canal canal particular o HORECA
     * @param total importe total calculado por el servicio
     * @param lineas productos y cantidades del pedido
     * @return identificador generado para el pedido
     * @throws SQLException si falla alguna insercion o la transaccion
     */
    public int crearPedido(String idDni, String canal, BigDecimal total, List<LineaPedido> lineas)
            throws SQLException {
        Connection conexion = conexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            int idPedido = insertarPedido(conexion, idDni, canal, total);
            for (LineaPedido linea : lineas) {
                insertarLinea(conexion, idPedido, linea);
            }
            conexion.commit();
            return idPedido;
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    /**
     * Recupera los pedidos y sus lineas para un cliente.
     *
     * @param idDni identificador del cliente
     * @return pedidos ordenados del mas reciente al mas antiguo
     * @throws SQLException si falla la consulta
     */
    public List<PedidoCliente> listarPedidosCliente(String idDni) throws SQLException {
        List<PedidoCliente> pedidos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_PEDIDOS_CLIENTE)) {
            sentencia.setString(1, idDni);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    int idPedido = resultados.getInt("id_pedido");
                    pedidos.add(new PedidoCliente(
                            idPedido,
                            resultados.getString("canal"),
                            resultados.getString("estado"),
                            resultados.getBigDecimal("importe_total"),
                            resultados.getTimestamp("fecha_creacion"),
                            listarLineasPedido(idPedido)
                    ));
                }
            }
        }
        return pedidos;
    }

    private void asegurarTablas() throws SQLException {
        try (Statement sentencia = conexion().createStatement()) {
            sentencia.execute(SQL_CREAR_TABLA_PEDIDO);
            sentencia.execute(SQL_CREAR_TABLA_LINEA);
        }
    }

    private int insertarPedido(Connection conexion, String idDni, String canal, BigDecimal total) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_PEDIDO, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, idDni);
            sentencia.setString(2, canal);
            sentencia.setBigDecimal(3, total);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se pudo recuperar el identificador del pedido.");
                }
                return claves.getInt(1);
            }
        }
    }

    private void insertarLinea(Connection conexion, int idPedido, LineaPedido linea) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_LINEA)) {
            sentencia.setInt(1, idPedido);
            sentencia.setString(2, linea.codigo());
            sentencia.setString(3, linea.nombre());
            sentencia.setString(4, linea.categoria());
            sentencia.setInt(5, linea.cantidad());
            sentencia.setBigDecimal(6, linea.precioUnitario());
            sentencia.setBigDecimal(7, linea.subtotal());
            sentencia.executeUpdate();
        }
    }

    private List<LineaPedido> listarLineasPedido(int idPedido) throws SQLException {
        List<LineaPedido> lineas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_LINEAS_PEDIDO)) {
            sentencia.setInt(1, idPedido);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    lineas.add(new LineaPedido(
                            resultados.getString("producto_codigo"),
                            resultados.getString("producto_nombre"),
                            resultados.getString("categoria"),
                            resultados.getInt("cantidad"),
                            resultados.getBigDecimal("precio_unitario"),
                            resultados.getBigDecimal("subtotal")
                    ));
                }
            }
        }
        return lineas;
    }

    private Connection conexion() throws SQLException {
        if (baseDeDatos == null) {
            throw new IllegalStateException("El DAO no tiene una base de datos configurada.");
        }
        return baseDeDatos.obtenerConexion();
    }
}
