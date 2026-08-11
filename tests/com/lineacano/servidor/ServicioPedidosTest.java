package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioPedidosTest {
    @Test
    void calculaTotalYEnviaLineasAlDao() throws SQLException {
        PedidoDaoPrueba dao = new PedidoDaoPrueba();
        ServicioPedidos servicio = new ServicioPedidos(dao);

        String json = servicio.crearPedido("11111111A", "registrado", "iberico:2;buey:1");

        assertTrue(json.contains("\"idPedido\":18"));
        assertTrue(json.contains("\"total\":304.00"));
        assertEquals("cliente", dao.canal);
        assertEquals(2, dao.lineas.size());
        assertEquals(new BigDecimal("304.00"), dao.total);
    }

    @Test
    void rechazaPedidoSinProductosValidos() {
        ServicioPedidos servicio = new ServicioPedidos(new PedidoDaoPrueba());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearPedido("11111111A", "registrado", "desconocido:2"));
    }

    private static final class PedidoDaoPrueba extends PedidoDao {
        private String canal;
        private BigDecimal total;
        private List<LineaPedido> lineas = List.of();

        private PedidoDaoPrueba() {
            super(null);
        }

        @Override
        public int crearPedido(String idDni, String canal, BigDecimal total, List<LineaPedido> lineas) {
            this.canal = canal;
            this.total = total;
            this.lineas = lineas;
            return 18;
        }
    }
}
