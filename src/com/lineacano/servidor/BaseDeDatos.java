package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class BaseDeDatos {
    private final Configuracion configuracion;
    private Connection conexion;

    public BaseDeDatos(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

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
