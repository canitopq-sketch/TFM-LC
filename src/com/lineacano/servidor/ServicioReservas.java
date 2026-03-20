package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class ServicioReservas {
    private static final String SQL_TOTAL_HABITACIONES = "SELECT COUNT(*) FROM habitacion";
    private static final String SQL_HABITACIONES_RESERVADAS = """
            SELECT COUNT(DISTINCT rh.num_habitacion)
            FROM reserva_habitacion rh
            INNER JOIN reserva r ON r.id_reserva = rh.id_reserva
            WHERE r.estado_reserva = 'Confirmada'
              AND r.fecha_entrada < ?
              AND r.fecha_salida > ?
            """;
    private static final String SQL_HABITACIONES_LIBRES = """
            SELECT h.num_habitacion, h.precio_noche
            FROM habitacion h
            WHERE h.num_habitacion NOT IN (
                SELECT rh.num_habitacion
                FROM reserva_habitacion rh
                INNER JOIN reserva r ON r.id_reserva = rh.id_reserva
                WHERE r.estado_reserva = 'Confirmada'
                  AND r.fecha_entrada < ?
                  AND r.fecha_salida > ?
            )
            ORDER BY h.num_habitacion
            """;
    private static final String SQL_CREAR_RESERVA = """
            INSERT INTO reserva (id_dni, fecha_entrada, fecha_salida, estado_reserva, importe_total)
            VALUES (?, ?, ?, 'Confirmada', ?)
            """;
    private static final String SQL_ASIGNAR_HABITACION = """
            INSERT INTO reserva_habitacion (id_reserva, num_habitacion)
            VALUES (?, ?)
            """;

    private final BaseDeDatos baseDeDatos;

    public ServicioReservas(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
    }

    public ResultadoDisponibilidad comprobarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida, int huespedes)
            throws SQLException {
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de llegada.");
        }

        // La disponibilidad se calcula restando habitaciones reservadas a las habitaciones totales.
        Connection conexion = baseDeDatos.obtenerConexion();
        int totalHabitaciones = contarTotalHabitaciones(conexion);
        int habitacionesReservadas = contarHabitacionesReservadas(conexion, fechaEntrada, fechaSalida);
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

    public String crearReserva(LocalDate fechaEntrada, LocalDate fechaSalida, int huespedes, String idDni)
            throws SQLException {
        if (idDni == null || idDni.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un cliente vinculado para reservar.");
        }

        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de llegada.");
        }

        Connection conexion = baseDeDatos.obtenerConexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);

        try {
            List<HabitacionLibre> habitacionesLibres = buscarHabitacionesLibres(conexion, fechaEntrada, fechaSalida);
            if (habitacionesLibres.isEmpty()) {
                throw new IllegalArgumentException("No hay habitaciones disponibles para confirmar esta reserva.");
            }

            HabitacionLibre habitacionAsignada = habitacionesLibres.get(0);
            int numeroNoches = (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
            double importeTotal = habitacionAsignada.precioNoche() * numeroNoches;
            int idReserva = insertarReserva(conexion, idDni, fechaEntrada, fechaSalida, importeTotal);
            asignarHabitacion(conexion, idReserva, habitacionAsignada.numeroHabitacion());
            conexion.commit();

            return """
                    {
                      "estado":"confirmada",
                      "titulo":"Reserva creada",
                      "mensaje":"La reserva ha quedado registrada correctamente.",
                      "idReserva":%d,
                      "habitacionAsignada":%d,
                      "importeTotal":%.2f,
                      "numeroNoches":%d,
                      "fechaEntrada":"%s",
                      "fechaSalida":"%s",
                      "huespedes":%d
                    }
                    """.formatted(
                    idReserva,
                    habitacionAsignada.numeroHabitacion(),
                    importeTotal,
                    numeroNoches,
                    fechaEntrada,
                    fechaSalida,
                    huespedes
            );
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    private int contarTotalHabitaciones(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_TOTAL_HABITACIONES);
             ResultSet resultados = sentencia.executeQuery()) {
            resultados.next();
            return resultados.getInt(1);
        }
    }

    private int contarHabitacionesReservadas(Connection conexion, LocalDate fechaEntrada, LocalDate fechaSalida)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_HABITACIONES_RESERVADAS)) {
            sentencia.setObject(1, fechaSalida);
            sentencia.setObject(2, fechaEntrada);

            try (ResultSet resultados = sentencia.executeQuery()) {
                resultados.next();
                return resultados.getInt(1);
            }
        }
    }

    private List<HabitacionLibre> buscarHabitacionesLibres(Connection conexion, LocalDate fechaEntrada, LocalDate fechaSalida)
            throws SQLException {
        List<HabitacionLibre> habitacionesLibres = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_HABITACIONES_LIBRES)) {
            sentencia.setObject(1, fechaSalida);
            sentencia.setObject(2, fechaEntrada);

            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    habitacionesLibres.add(new HabitacionLibre(
                            resultados.getInt("num_habitacion"),
                            resultados.getDouble("precio_noche")
                    ));
                }
            }
        }

        return habitacionesLibres;
    }

    private int insertarReserva(Connection conexion, String idDni, LocalDate fechaEntrada, LocalDate fechaSalida, double importeTotal)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CREAR_RESERVA, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, idDni);
            sentencia.setObject(2, fechaEntrada);
            sentencia.setObject(3, fechaSalida);
            sentencia.setDouble(4, importeTotal);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se pudo recuperar el identificador de la reserva.");
                }

                return claves.getInt(1);
            }
        }
    }

    private void asignarHabitacion(Connection conexion, int idReserva, int numeroHabitacion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ASIGNAR_HABITACION)) {
            sentencia.setInt(1, idReserva);
            sentencia.setInt(2, numeroHabitacion);
            sentencia.executeUpdate();
        }
    }

    private record HabitacionLibre(int numeroHabitacion, double precioNoche) {
    }
}
