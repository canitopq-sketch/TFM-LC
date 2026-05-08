package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona una conexion JDBC reutilizable contra la base de datos configurada.
 */
public final class BaseDeDatos {
    private final Configuracion configuracion;
    private Connection conexion;

    /**
     * Crea el gestor de conexion con los parametros de configuracion de la aplicacion.
     *
     * @param configuracion configuracion de puerto, raiz web y credenciales de base de datos
     */
    public BaseDeDatos(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

    /**
     * Obtiene una conexion activa, abriendo una nueva si no existe o se habia cerrado.
     *
     * @return conexion JDBC lista para ejecutar consultas
     * @throws SQLException si el driver no puede abrir la conexion
     */
    public synchronized Connection obtenerConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(
                    configuracion.urlBaseDeDatos(),
                    configuracion.usuarioBaseDeDatos(),
                    configuracion.contrasenaBaseDeDatos()
            );
        }

        return conexion;
    }

    /**
     * Cierra la conexion abierta, si existe.
     */
    public synchronized void cerrar() {
        if (conexion == null) {
            return;
        }

        try {
            conexion.close();
        } catch (SQLException ignorada) {
        }
    }
}
