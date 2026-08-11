package com.lineacano.servidor;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para disponibilidad, creacion, listado y cancelacion de reservas.
 */
public final class ServicioReservas {
    private final ReservaDao reservaDao;

    /**
     * Crea el servicio usando el DAO de reservas.
     *
     * @param reservaDao acceso a la persistencia de habitaciones y reservas
     */
    public ServicioReservas(ReservaDao reservaDao) {
        this.reservaDao = reservaDao;
    }

    /**
     * Comprueba el inventario disponible para un rango de fechas.
     *
     * @param fechaEntrada fecha de llegada
     * @param fechaSalida fecha de salida, posterior a la entrada
     * @param huespedes numero de huespedes solicitado
     * @return resumen de disponibilidad consumido por el frontend
     * @throws SQLException si falla la consulta a base de datos
     */
    public ResultadoDisponibilidad comprobarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida, int huespedes)
            throws SQLException {
        ReglasNegocio.validarPeriodoReserva(fechaEntrada, fechaSalida);
        ReglasNegocio.validarHuespedes(huespedes);

        // La disponibilidad se calcula restando habitaciones reservadas a las habitaciones totales.
        int totalHabitaciones = reservaDao.contarTotalHabitaciones();
        int habitacionesReservadas = reservaDao.contarHabitacionesReservadas(fechaEntrada, fechaSalida);
        int habitacionesDisponibles = Math.max(totalHabitaciones - habitacionesReservadas, 0);
        int numeroNoches = (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);

        String estado;
        String titulo;
        String mensaje;

        if (habitacionesDisponibles >= 4) {
            estado = "disponible";
            titulo = "Disponibilidad alta";
            mensaje = habitacionesDisponibles + " habitaciones disponibles para " + huespedes + " huespedes en las fechas seleccionadas.";
        } else if (habitacionesDisponibles >= 1) {
            estado = "demanda";
            titulo = "Alta demanda";
            mensaje = "Quedan " + habitacionesDisponibles + " habitaciones disponibles. Conviene confirmar la reserva cuanto antes.";
        } else {
            estado = "revision";
            titulo = "Revision comercial recomendada";
            mensaje = "No hay disponibilidad libre en este rango. Deriva esta consulta a maestro/comercial para buscar alternativas.";
        }

        return new ResultadoDisponibilidad(
                estado,
                titulo,
                mensaje,
                totalHabitaciones,
                habitacionesReservadas,
                habitacionesDisponibles,
                numeroNoches,
                fechaEntrada,
                fechaSalida,
                huespedes
        );
    }

    /**
     * Crea una reserva confirmada y asigna la primera habitacion libre.
     *
     * @param fechaEntrada fecha de llegada
     * @param fechaSalida fecha de salida, posterior a la entrada
     * @param huespedes numero de huespedes indicado en la reserva
     * @param idDni identificador del cliente autenticado
     * @return JSON con identificador, habitacion asignada e importe estimado
     * @throws SQLException si falla la transaccion de insercion
     */
    public String crearReserva(LocalDate fechaEntrada, LocalDate fechaSalida, int huespedes, String idDni)
            throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado para reservar.");
        }

        ReglasNegocio.validarPeriodoReserva(fechaEntrada, fechaSalida);
        ReglasNegocio.validarHuespedes(huespedes);

        List<HabitacionLibre> habitacionesLibres = reservaDao.buscarHabitacionesLibres(fechaEntrada, fechaSalida);
        if (habitacionesLibres.isEmpty()) {
            throw new IllegalArgumentException("No hay habitaciones disponibles para confirmar esta reserva.");
        }

        HabitacionLibre habitacionAsignada = habitacionesLibres.get(0);
        int numeroNoches = (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
        double importeTotal = habitacionAsignada.precioNoche() * numeroNoches;
        int idReserva = reservaDao.crearReserva(
                idDni,
                fechaEntrada,
                fechaSalida,
                importeTotal,
                habitacionAsignada.numeroHabitacion()
        );

        return """
                {
                  "estado":"confirmada",
                  "titulo":"Reserva creada",
                  "mensaje":"La reserva ha quedado registrada correctamente.",
                  "idReserva":%d,
                  "habitacionAsignada":%d,
                  "importeTotal":%s,
                  "numeroNoches":%d,
                  "fechaEntrada":"%s",
                  "fechaSalida":"%s",
                  "huespedes":%d
                }
                """.formatted(
                idReserva,
                habitacionAsignada.numeroHabitacion(),
                formatearNumeroJson(java.math.BigDecimal.valueOf(importeTotal)),
                numeroNoches,
                fechaEntrada,
                fechaSalida,
                huespedes
        );
    }

    /**
     * Lista las reservas asociadas a un cliente autenticado.
     *
     * @param idDni identificador del cliente
     * @return JSON con el total y el detalle de reservas localizadas
     * @throws SQLException si falla la consulta de reservas
     */
    public String listarReservasCliente(String idDni) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado.");
        }

        List<String> reservas = new ArrayList<>();
        for (ReservaCliente reserva : reservaDao.listarReservasCliente(idDni)) {
            reservas.add("""
                    {
                      "idReserva":%d,
                      "fechaEntrada":"%s",
                      "fechaSalida":"%s",
                      "estado":"%s",
                      "importeTotal":%s,
                      "habitacionAsignada":%s
                    }
                    """.formatted(
                    reserva.idReserva(),
                    reserva.fechaEntrada(),
                    reserva.fechaSalida(),
                    UtilJson.escapar(reserva.estado()),
                    formatearNumeroJson(reserva.importeTotal()),
                    reserva.habitacionAsignada() == null ? "null" : reserva.habitacionAsignada()
            ));
        }

        return """
                {
                  "titulo":"Reservas del cliente",
                  "total":%d,
                  "reservas":[%s]
                }
                """.formatted(
                reservas.size(),
                String.join(",", reservas)
        );
    }

    /**
     * Cancela una reserva futura perteneciente al cliente autenticado.
     *
     * @param idReserva identificador de la reserva a cancelar
     * @param idDni identificador del cliente propietario de la reserva
     * @return JSON de confirmacion de cancelacion
     * @throws SQLException si falla la lectura o actualizacion de la reserva
     */
    public String cancelarReserva(int idReserva, String idDni) throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado.");
        }

        EstadoReserva reserva = reservaDao.buscarReservaCliente(idReserva, idDni)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La reserva indicada no pertenece al cliente autenticado."
                ));

        if ("Cancelada".equalsIgnoreCase(reserva.estado())) {
            throw new IllegalArgumentException("La reserva ya estaba cancelada.");
        }

        if (reserva.fechaEntrada() != null && !reserva.fechaEntrada().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se pueden cancelar reservas futuras.");
        }

        if (!reservaDao.cancelarReserva(idReserva, idDni)) {
            throw new SQLException("No se pudo actualizar el estado de la reserva.");
        }

        return """
                {
                  "titulo":"Reserva cancelada",
                  "mensaje":"La reserva %d ha quedado cancelada correctamente.",
                  "idReserva":%d
                }
                """.formatted(idReserva, idReserva);
    }

    private String formatearNumeroJson(java.math.BigDecimal valor) {
        return valor == null ? "null" : valor.toPlainString();
    }
}
