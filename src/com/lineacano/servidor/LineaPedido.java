package com.lineacano.servidor;

import java.math.BigDecimal;

/**
 * Datos de una linea de pedido guardada o recuperada por el DAO.
 *
 * @param codigo codigo del producto
 * @param nombre nombre mostrado
 * @param categoria categoria comercial
 * @param cantidad unidades solicitadas
 * @param precioUnitario precio por unidad
 * @param subtotal importe de la linea
 */
public record LineaPedido(
        String codigo,
        String nombre,
        String categoria,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
