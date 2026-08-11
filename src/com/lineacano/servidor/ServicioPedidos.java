package com.lineacano.servidor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para pedidos de producto persistidos en MySQL.
 */
public final class ServicioPedidos {
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
    private static final Map<String, ProductoCatalogo> CATALOGO = crearCatalogo();

    private final BaseDeDatos baseDeDatos;

    public ServicioPedidos(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
        try {
            asegurarTablas();
        } catch (SQLException excepcion) {
            throw new IllegalStateException("No se pudieron preparar las tablas de pedidos.", excepcion);
        }
    }

    public String crearPedido(String idDni, String rol, String lineasBrutas) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado para comprar.");
        }

        List<LineaPedido> lineas = analizarLineas(lineasBrutas);
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("Anade al menos un producto antes de confirmar el pedido.");
        }

        String canal = "horeca".equals(rol) ? "horeca" : "cliente";
        BigDecimal total = calcularTotal(lineas);
        Connection conexion = baseDeDatos.obtenerConexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);

        try {
            int idPedido = insertarPedido(conexion, idDni, canal, total);
            for (LineaPedido linea : lineas) {
                insertarLinea(conexion, idPedido, linea);
            }
            conexion.commit();

            return """
                    {
                      "titulo":"Pedido registrado",
                      "mensaje":"El pedido LC-%d ha quedado guardado en la base de datos.",
                      "idPedido":%d,
                      "referencia":"LC-%d",
                      "estado":"Solicitud recibida",
                      "canal":"%s",
                      "total":%s
                    }
                    """.formatted(
                    idPedido,
                    idPedido,
                    idPedido,
                    canal,
                    total.toPlainString()
            );
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    public String listarPedidosCliente(String idDni) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado.");
        }

        List<String> pedidos = new ArrayList<>();

        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_PEDIDOS_CLIENTE)) {
            sentencia.setString(1, idDni);

            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    int idPedido = resultados.getInt("id_pedido");
                    pedidos.add("""
                            {
                              "id":"LC-%d",
                              "idPedido":%d,
                              "fecha":"%s",
                              "estado":"%s",
                              "canal":"%s",
                              "total":%s,
                              "lineas":[%s]
                            }
                            """.formatted(
                            idPedido,
                            idPedido,
                            formatearFecha(resultados.getTimestamp("fecha_creacion")),
                            UtilJson.escapar(resultados.getString("estado")),
                            UtilJson.escapar(resultados.getString("canal")),
                            resultados.getBigDecimal("importe_total").toPlainString(),
                            String.join(",", listarLineasPedido(idPedido))
                    ));
                }
            }
        }

        return """
                {
                  "titulo":"Pedidos del cliente",
                  "total":%d,
                  "pedidos":[%s]
                }
                """.formatted(pedidos.size(), String.join(",", pedidos));
    }

    private void asegurarTablas() throws SQLException {
        Connection conexion = baseDeDatos.obtenerConexion();

        try (Statement sentencia = conexion.createStatement()) {
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
            sentencia.setString(2, linea.producto().codigo());
            sentencia.setString(3, linea.producto().nombre());
            sentencia.setString(4, linea.producto().categoria());
            sentencia.setInt(5, linea.cantidad());
            sentencia.setBigDecimal(6, linea.producto().precio());
            sentencia.setBigDecimal(7, linea.subtotal());
            sentencia.executeUpdate();
        }
    }

    private List<String> listarLineasPedido(int idPedido) throws SQLException {
        List<String> lineas = new ArrayList<>();

        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_LINEAS_PEDIDO)) {
            sentencia.setInt(1, idPedido);

            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    lineas.add("""
                            {
                              "id":"%s",
                              "nombre":"%s",
                              "categoria":"%s",
                              "cantidad":%d,
                              "precio":%s,
                              "subtotal":%s
                            }
                            """.formatted(
                            UtilJson.escapar(resultados.getString("producto_codigo")),
                            UtilJson.escapar(resultados.getString("producto_nombre")),
                            UtilJson.escapar(resultados.getString("categoria")),
                            resultados.getInt("cantidad"),
                            resultados.getBigDecimal("precio_unitario").toPlainString(),
                            resultados.getBigDecimal("subtotal").toPlainString()
                    ));
                }
            }
        }

        return lineas;
    }

    private List<LineaPedido> analizarLineas(String lineasBrutas) {
        List<LineaPedido> lineas = new ArrayList<>();

        if (lineasBrutas == null || lineasBrutas.isBlank()) {
            return lineas;
        }

        for (String fragmento : lineasBrutas.split(";")) {
            String[] partes = fragmento.split(":", 2);
            if (partes.length != 2) {
                continue;
            }

            ProductoCatalogo producto = CATALOGO.get(partes[0].trim());
            int cantidad = analizarCantidad(partes[1]);

            if (producto != null && cantidad > 0) {
                lineas.add(new LineaPedido(producto, cantidad));
            }
        }

        return lineas;
    }

    private int analizarCantidad(String valor) {
        try {
            return Math.min(Integer.parseInt(valor), 99);
        } catch (NumberFormatException excepcion) {
            return 0;
        }
    }

    private BigDecimal calcularTotal(List<LineaPedido> lineas) {
        return lineas.stream()
                .map(LineaPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatearFecha(Timestamp fecha) {
        if (fecha == null) {
            return "";
        }

        return fecha.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static Map<String, ProductoCatalogo> crearCatalogo() {
        Map<String, ProductoCatalogo> catalogo = new LinkedHashMap<>();
        catalogo.put("iberico", new ProductoCatalogo("iberico", "Seleccion iberica de montanera", "Iberico", new BigDecimal("89.00")));
        catalogo.put("buey", new ProductoCatalogo("buey", "Corte premium madurado", "Buey", new BigDecimal("126.00")));
        catalogo.put("despensa", new ProductoCatalogo("despensa", "Cesta fresca de temporada", "Despensa", new BigDecimal("42.00")));
        return catalogo;
    }

    private record ProductoCatalogo(String codigo, String nombre, String categoria, BigDecimal precio) {
    }

    private record LineaPedido(ProductoCatalogo producto, int cantidad) {
        private BigDecimal subtotal() {
            return producto.precio().multiply(BigDecimal.valueOf(cantidad));
        }
    }
}
