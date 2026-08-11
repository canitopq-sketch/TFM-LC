package com.lineacano.servidor;

import java.math.BigDecimal;
import java.sql.SQLException;
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
    private static final Map<String, ProductoCatalogo> CATALOGO = crearCatalogo();

    private final PedidoDao pedidoDao;

    /**
     * Crea el servicio con el DAO encargado de guardar los pedidos.
     *
     * @param pedidoDao acceso a la persistencia de pedidos
     */
    public ServicioPedidos(PedidoDao pedidoDao) {
        this.pedidoDao = pedidoDao;
    }

    /**
     * Registra un pedido y sus lineas dentro de una unica transaccion.
     *
     * @param idDni cliente autenticado que realiza el pedido
     * @param rol rol que determina el canal particular o HORECA
     * @param lineasBrutas codigos y cantidades enviados por el formulario
     * @return confirmacion JSON del pedido creado
     * @throws SQLException si falla la operacion contra MySQL
     */
    public String crearPedido(String idDni, String rol, String lineasBrutas) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado para comprar.");
        }

        List<LineaCarrito> lineas = analizarLineas(lineasBrutas);
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("Anade al menos un producto antes de confirmar el pedido.");
        }

        String canal = "horeca".equals(rol) ? "horeca" : "cliente";
        BigDecimal total = calcularTotal(lineas);
        List<LineaPedido> lineasPersistencia = lineas.stream()
                .map(linea -> new LineaPedido(
                        linea.producto().codigo(),
                        linea.producto().nombre(),
                        linea.producto().categoria(),
                        linea.cantidad(),
                        linea.producto().precio(),
                        linea.subtotal()
                ))
                .toList();
        int idPedido = pedidoDao.crearPedido(idDni, canal, total, lineasPersistencia);

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
    }

    /**
     * Recupera el historial persistido del cliente autenticado.
     *
     * @param idDni identificador del cliente
     * @return documento JSON con pedidos y lineas
     * @throws SQLException si falla la consulta contra MySQL
     */
    public String listarPedidosCliente(String idDni) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado.");
        }

        List<String> pedidos = new ArrayList<>();
        for (PedidoCliente pedido : pedidoDao.listarPedidosCliente(idDni)) {
            List<String> lineas = pedido.lineas().stream()
                    .map(this::lineaAJson)
                    .toList();
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
                    pedido.idPedido(),
                    pedido.idPedido(),
                    formatearFecha(pedido.fechaCreacion()),
                    UtilJson.escapar(pedido.estado()),
                    UtilJson.escapar(pedido.canal()),
                    pedido.importeTotal().toPlainString(),
                    String.join(",", lineas)
            ));
        }

        return """
                {
                  "titulo":"Pedidos del cliente",
                  "total":%d,
                  "pedidos":[%s]
                }
                """.formatted(pedidos.size(), String.join(",", pedidos));
    }

    private String lineaAJson(LineaPedido linea) {
        return """
                {
                  "id":"%s",
                  "nombre":"%s",
                  "categoria":"%s",
                  "cantidad":%d,
                  "precio":%s,
                  "subtotal":%s
                }
                """.formatted(
                UtilJson.escapar(linea.codigo()),
                UtilJson.escapar(linea.nombre()),
                UtilJson.escapar(linea.categoria()),
                linea.cantidad(),
                linea.precioUnitario().toPlainString(),
                linea.subtotal().toPlainString()
        );
    }

    private List<LineaCarrito> analizarLineas(String lineasBrutas) {
        List<LineaCarrito> lineas = new ArrayList<>();

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
                lineas.add(new LineaCarrito(producto, cantidad));
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

    private BigDecimal calcularTotal(List<LineaCarrito> lineas) {
        return lineas.stream()
                .map(LineaCarrito::subtotal)
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

    private record LineaCarrito(ProductoCatalogo producto, int cantidad) {
        private BigDecimal subtotal() {
            return producto.precio().multiply(BigDecimal.valueOf(cantidad));
        }
    }
}
