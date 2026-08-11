package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReglasNegocioTest {
    @Test
    void aceptaPeriodoDeReservaValido() {
        assertDoesNotThrow(() -> ReglasNegocio.validarPeriodoReserva(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12)
        ));
    }

    @Test
    void rechazaSalidaIgualOAnteriorALaEntrada() {
        LocalDate entrada = LocalDate.of(2026, 9, 10);

        assertThrows(IllegalArgumentException.class,
                () -> ReglasNegocio.validarPeriodoReserva(entrada, entrada));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasNegocio.validarPeriodoReserva(entrada, entrada.minusDays(1)));
    }

    @Test
    void rechazaFechasAusentes() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasNegocio.validarPeriodoReserva(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasNegocio.validarPeriodoReserva(LocalDate.now(), null));
    }

    @Test
    void validaLimitesDeHuespedes() {
        assertDoesNotThrow(() -> ReglasNegocio.validarHuespedes(1));
        assertDoesNotThrow(() -> ReglasNegocio.validarHuespedes(20));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarHuespedes(0));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarHuespedes(21));
    }

    @Test
    void validaPoliticaDeContrasenas() {
        assertDoesNotThrow(() -> ReglasNegocio.validarContrasena("abc123"));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarContrasena(null));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarContrasena(""));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarContrasena("masDeDiez12"));
        assertThrows(IllegalArgumentException.class, () -> ReglasNegocio.validarContrasena("clave-123"));
    }
}
