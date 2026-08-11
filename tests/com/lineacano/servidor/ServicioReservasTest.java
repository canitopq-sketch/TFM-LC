package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioReservasTest {
    @Test
    void creaReservaUsandoHabitacionDevueltaPorDao() throws SQLException {
        ReservaDaoPrueba dao = new ReservaDaoPrueba();
        ServicioReservas servicio = new ServicioReservas(dao);

        String json = servicio.crearReserva(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                2,
                "11111111A"
        );

        assertTrue(json.contains("\"idReserva\":45"));
        assertTrue(json.contains("\"habitacionAsignada\":7"));
        assertTrue(json.contains("\"importeTotal\":180.0"));
        assertTrue(dao.crearReservaInvocada);
    }

    @Test
    void rechazaReservaCuandoDaoNoEncuentraHabitaciones() {
        ReservaDaoPrueba dao = new ReservaDaoPrueba();
        dao.habitaciones = List.of();
        ServicioReservas servicio = new ServicioReservas(dao);

        assertThrows(IllegalArgumentException.class, () -> servicio.crearReserva(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                2,
                "11111111A"
        ));
    }

    private static final class ReservaDaoPrueba extends ReservaDao {
        private List<HabitacionLibre> habitaciones = List.of(new HabitacionLibre(7, 90.0));
        private boolean crearReservaInvocada;

        private ReservaDaoPrueba() {
            super(null);
        }

        @Override
        public List<HabitacionLibre> buscarHabitacionesLibres(LocalDate entrada, LocalDate salida) {
            return habitaciones;
        }

        @Override
        public int crearReserva(String idDni, LocalDate entrada, LocalDate salida, double importe, int habitacion) {
            crearReservaInvocada = true;
            return 45;
        }

        @Override
        public Optional<EstadoReserva> buscarReservaCliente(int idReserva, String idDni) {
            return Optional.empty();
        }
    }
}
