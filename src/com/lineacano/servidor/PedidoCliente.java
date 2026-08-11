package com.lineacano.servidor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Datos de un pedido recuperado para el historial del cliente.
 *
 * @param idPedido identificador del pedido
 * @param canal canal particular o HORECA
 * @param estado estado actual
 * @param importeTotal importe total
 * @param fechaCreacion fecha de registro
 * @param lineas productos incluidos
 */
public record PedidoCliente(
        int idPedido,
        String canal,
        String estado,
        BigDecimal importeTotal,
        Timestamp fechaCreacion,
        List<LineaPedido> lineas
) {
}
