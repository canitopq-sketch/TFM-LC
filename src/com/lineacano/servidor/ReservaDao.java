package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso JDBC a las tablas de habitaciones y reservas.
 */
public class ReservaDao {
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
    private static final String SQL_RESERVAS_CLIENTE = """
            SELECT r.id_reserva, r.fecha_entrada, r.fecha_salida, r.estado_reserva, r.importe_total, rh.num_habitacion
            FROM reserva r
            LEFT JOIN reserva_habitacion rh ON rh.id_reserva = r.id_reserva
            WHERE r.id_dni = ?
            ORDER BY r.fecha_entrada DESC, r.id_reserva DESC
            """;
    private static final String SQL_BUSCAR_RESERVA_CLIENTE = """
            SELECT id_reserva, fecha_entrada, estado_reserva
            FROM reserva
            WHERE id_reserva = ? AND id_dni = ?
            """;
    private static final String SQL_CANCELAR_RESERVA = """
            UPDATE reserva
            SET estado_reserva = 'Cancelada'
            WHERE id_reserva = ? AND id_dni = ?
            """;

    private final BaseDeDatos baseDeDatos;

    /**
     * Crea el DAO con el gestor de conexiones compartido.
     *
     * @param baseDeDatos gestor de la conexion JDBC
     */
    public ReservaDao(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
    }

    public int contarTotalHabitaciones() throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_TOTAL_HABITACIONES);
             ResultSet resultados = sentencia.executeQuery()) {
            resultados.next();
            return resultados.getInt(1);
        }
    }

    public int contarHabitacionesReservadas(LocalDate fechaEntrada, LocalDate fechaSalida) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_HABITACIONES_RESERVADAS)) {
            sentencia.setObject(1, fechaSalida);
            sentencia.setObject(2, fechaEntrada);
            try (ResultSet resultados = sentencia.executeQuery()) {
                resultados.next();
                return resultados.getInt(1);
            }
        }
    }

    public List<HabitacionLibre> buscarHabitacionesLibres(LocalDate fechaEntrada, LocalDate fechaSalida)
            throws SQLException {
        List<HabitacionLibre> habitaciones = new ArrayList<>();
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_HABITACIONES_LIBRES)) {
            sentencia.setObject(1, fechaSalida);
            sentencia.setObject(2, fechaEntrada);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    habitaciones.add(new HabitacionLibre(
                            resultados.getInt("num_habitacion"),
                            resultados.getDouble("precio_noche")
                    ));
                }
            }
        }
        return habitaciones;
    }

    public int crearReserva(
            String idDni,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            double importeTotal,
            int numeroHabitacion
    ) throws SQLException {
        Connection conexion = conexion();
        boolean autocommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);

        try {
            int idReserva;
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
                    idReserva = claves.getInt(1);
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ASIGNAR_HABITACION)) {
                sentencia.setInt(1, idReserva);
                sentencia.setInt(2, numeroHabitacion);
                sentencia.executeUpdate();
            }

            conexion.commit();
            return idReserva;
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    public List<ReservaCliente> listarReservasCliente(String idDni) throws SQLException {
        List<ReservaCliente> reservas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_RESERVAS_CLIENTE)) {
            sentencia.setString(1, idDni);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    Integer habitacion = resultados.getObject("num_habitacion") == null
                            ? null
                            : resultados.getInt("num_habitacion");
                    reservas.add(new ReservaCliente(
                            resultados.getInt("id_reserva"),
                            resultados.getObject("fecha_entrada", LocalDate.class),
                            resultados.getObject("fecha_salida", LocalDate.class),
                            resultados.getString("estado_reserva"),
                            resultados.getBigDecimal("importe_total"),
                            habitacion
                    ));
                }
            }
        }
        return reservas;
    }

    public Optional<EstadoReserva> buscarReservaCliente(int idReserva, String idDni) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_BUSCAR_RESERVA_CLIENTE)) {
            sentencia.setInt(1, idReserva);
            sentencia.setString(2, idDni);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    return Optional.empty();
                }
                return Optional.of(new EstadoReserva(
                        resultados.getInt("id_reserva"),
                        resultados.getObject("fecha_entrada", LocalDate.class),
                        resultados.getString("estado_reserva")
                ));
            }
        }
    }

    public boolean cancelarReserva(int idReserva, String idDni) throws SQLException {
        try (PreparedStatement sentencia = conexion().prepareStatement(SQL_CANCELAR_RESERVA)) {
            sentencia.setInt(1, idReserva);
            sentencia.setString(2, idDni);
            return sentencia.executeUpdate() > 0;
        }
    }

    private Connection conexion() throws SQLException {
        if (baseDeDatos == null) {
            throw new IllegalStateException("El DAO no tiene una base de datos configurada.");
        }
        return baseDeDatos.obtenerConexion();
    }
}
