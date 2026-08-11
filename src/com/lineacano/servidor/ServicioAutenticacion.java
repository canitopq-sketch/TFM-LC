package com.lineacano.servidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de negocio para autenticacion, sesiones persistentes y altas de usuarios.
 */
public final class ServicioAutenticacion {
    private static final Duration DURACION_MAXIMA_SESION = Duration.ofDays(7);
    private static final String SQL_INICIO_SESION = """
            SELECT ua.correo, ua.contrasena_hash, ua.rol, ua.activo, ua.id_dni, c.nombre, c.apellido1
            FROM usuario_acceso ua
            LEFT JOIN cliente c ON c.id_dni = ua.id_dni
            WHERE ua.correo = ?
            """;
    private static final String SQL_EXISTE_CORREO = """
            SELECT 1
            FROM usuario_acceso
            WHERE correo = ?
            """;
    private static final String SQL_EXISTE_DOCUMENTO = """
            SELECT 1
            FROM cliente
            WHERE id_dni = ?
            """;
    private static final String SQL_INSERTAR_CLIENTE = """
            INSERT INTO cliente (id_dni, nombre, apellido1, correo, telefono)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SQL_INSERTAR_USUARIO = """
            INSERT INTO usuario_acceso (correo, contrasena_hash, rol, activo, id_dni)
            VALUES (?, ?, ?, 1, ?)
            """;
    private static final String SQL_INSERTAR_CLIENTE_HORECA = """
            INSERT INTO cliente_horeca (id_dni, empresa, cif, correo_profesional, telefono_contacto)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SQL_CREAR_TABLA_CLIENTE_HORECA = """
            CREATE TABLE IF NOT EXISTS cliente_horeca (
              id_dni varchar(20) NOT NULL,
              empresa varchar(150) NOT NULL,
              cif varchar(20) NOT NULL,
              correo_profesional varchar(100) NOT NULL,
              telefono_contacto varchar(20) DEFAULT NULL,
              fecha_alta timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id_dni),
              UNIQUE KEY uq_cliente_horeca_cif (cif),
              UNIQUE KEY uq_cliente_horeca_correo (correo_profesional),
              CONSTRAINT fk_cliente_horeca_cliente
                FOREIGN KEY (id_dni) REFERENCES cliente (id_dni)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;
    private static final String SQL_CREAR_TABLA_SESIONES = """
            CREATE TABLE IF NOT EXISTS sesion_acceso (
              token varchar(80) NOT NULL,
              correo varchar(100) NOT NULL,
              fecha_creacion timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (token),
              KEY fk_sesion_acceso_usuario (correo),
              CONSTRAINT fk_sesion_acceso_usuario
                FOREIGN KEY (correo) REFERENCES usuario_acceso (correo)
                ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
            """;
    private static final String SQL_INSERTAR_SESION = """
            INSERT INTO sesion_acceso (token, correo, fecha_creacion)
            VALUES (?, ?, ?)
            """;
    private static final String SQL_BUSCAR_SESION = """
            SELECT sa.token, sa.fecha_creacion, ua.correo, ua.rol, ua.activo, ua.id_dni, c.nombre, c.apellido1
            FROM sesion_acceso sa
            INNER JOIN usuario_acceso ua ON ua.correo = sa.correo
            LEFT JOIN cliente c ON c.id_dni = ua.id_dni
            WHERE sa.token = ?
            """;
    private static final String SQL_ELIMINAR_SESION = """
            DELETE FROM sesion_acceso
            WHERE token = ?
            """;
    private static final String SQL_ELIMINAR_SESIONES_CADUCADAS = """
            DELETE FROM sesion_acceso
            WHERE fecha_creacion < ?
            """;

    private final BaseDeDatos baseDeDatos;

    /**
     * Crea el servicio y asegura la tabla de sesiones necesaria para validar tokens.
     *
     * @param baseDeDatos acceso JDBC compartido por la aplicacion
     */
    public ServicioAutenticacion(BaseDeDatos baseDeDatos) {
        this.baseDeDatos = baseDeDatos;
        try {
            asegurarTablaSesiones();
        } catch (SQLException excepcion) {
            throw new IllegalStateException("No se pudo preparar la tabla de sesiones.", excepcion);
        }
    }

    /**
     * Valida credenciales y registra una nueva sesion persistente.
     *
     * @param usuario correo de acceso
     * @param contrasena contrasena en texto plano introducida por el usuario
     * @return datos de sesion que se devuelven al frontend
     * @throws SQLException si falla la consulta o el registro de sesion
     */
    public UsuarioSesion iniciarSesion(String usuario, String contrasena) throws SQLException {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }

        ReglasNegocio.validarContrasena(contrasena);

        // Se normaliza el correo para evitar diferencias por mayúsculas o espacios.
        String correoNormalizado = usuario.trim().toLowerCase();
        String hashContrasena = Contrasenas.sha256(contrasena);

        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_INICIO_SESION)) {
            sentencia.setString(1, correoNormalizado);

            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
                }

                boolean activo = resultados.getBoolean("activo");
                if (!activo) {
                    throw new IllegalArgumentException("El usuario no esta activo.");
                }

                String hashEsperado = resultados.getString("contrasena_hash");
                if (!hashContrasena.equalsIgnoreCase(hashEsperado)) {
                    throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
                }

                String nombreVisible = construirNombreVisible(
                        resultados.getString("nombre"),
                        resultados.getString("apellido1"),
                        correoNormalizado
                );

                String rolNormalizado = normalizarRol(resultados.getString("rol"));

                Instant fechaCreacion = Instant.now();
                UsuarioSesion sesion = new UsuarioSesion(
                        UUID.randomUUID().toString(),
                        correoNormalizado,
                        nombreVisible,
                        rolNormalizado,
                        resultados.getString("id_dni"),
                        fechaCreacion
                );

                registrarSesionPersistente(sesion, fechaCreacion);
                return sesion;
            }
        } catch (SQLException excepcion) {
            if (excepcion.getMessage() != null && excepcion.getMessage().contains("usuario_acceso")) {
                throw new IllegalStateException(
                        "Falta la tabla usuario_acceso. Ejecuta la importacion con sql/auth_schema.sql."
                );
            }

            throw excepcion;
        }
    }

    /**
     * Busca y valida una sesion a partir del token recibido por cabecera.
     *
     * @param token token enviado por el frontend
     * @return sesion valida, o vacio si el token no existe, ha caducado o el usuario esta inactivo
     */
    public Optional<UsuarioSesion> buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            limpiarSesionesCaducadas();

            try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_BUSCAR_SESION)) {
                sentencia.setString(1, token.trim());

                try (ResultSet resultados = sentencia.executeQuery()) {
                    if (!resultados.next()) {
                        return Optional.empty();
                    }

                    if (!resultados.getBoolean("activo")) {
                        eliminarSesion(token);
                        return Optional.empty();
                    }

                    Timestamp marcaTiempo = resultados.getTimestamp("fecha_creacion");
                    Instant fechaCreacion = marcaTiempo == null ? Instant.now() : marcaTiempo.toInstant();

                    if (fechaCreacion.plus(DURACION_MAXIMA_SESION).isBefore(Instant.now())) {
                        eliminarSesion(token);
                        return Optional.empty();
                    }

                    return Optional.of(new UsuarioSesion(
                            resultados.getString("token"),
                            resultados.getString("correo"),
                            construirNombreVisible(
                                    resultados.getString("nombre"),
                                    resultados.getString("apellido1"),
                                    resultados.getString("correo")
                            ),
                            normalizarRol(resultados.getString("rol")),
                            resultados.getString("id_dni"),
                            fechaCreacion
                    ));
                }
            }
        } catch (SQLException excepcion) {
            return Optional.empty();
        }
    }

    /**
     * Registra un nuevo usuario con rol de cliente registrado.
     *
     * @param documento DNI o documento identificativo
     * @param nombre nombre del cliente
     * @param apellido primer apellido del cliente
     * @param correo correo de acceso
     * @param telefono telefono opcional
     * @param contrasena contrasena inicial
     * @return JSON de confirmacion del alta
     * @throws SQLException si falla la transaccion de alta
     */
    public String registrarNuevoUsuario(
            String documento,
            String nombre,
            String apellido,
            String correo,
            String telefono,
            String contrasena
    ) throws SQLException {
        validarDocumento(documento, "El DNI o documento es obligatorio.");
        validarTexto(nombre, "El nombre es obligatorio.");
        validarTexto(apellido, "El primer apellido es obligatorio.");
        validarCorreo(correo);
        validarContrasena(contrasena);

        String documentoNormalizado = normalizarDocumento(documento);
        String nombreNormalizado = nombre.trim();
        String apellidoNormalizado = apellido.trim();
        String correoNormalizado = correo.trim().toLowerCase();
        String telefonoNormalizado = limpiarSiVacio(telefono);
        String hashContrasena = Contrasenas.sha256(contrasena.trim());

        var conexion = baseDeDatos.obtenerConexion();
        boolean autocommitOriginal = conexion.getAutoCommit();

        try {
            conexion.setAutoCommit(false);
            validarDisponibilidadRegistro(correoNormalizado, documentoNormalizado);

            try (PreparedStatement insertarCliente = conexion.prepareStatement(SQL_INSERTAR_CLIENTE);
                 PreparedStatement insertarUsuario = conexion.prepareStatement(SQL_INSERTAR_USUARIO)) {
                insertarCliente.setString(1, documentoNormalizado);
                insertarCliente.setString(2, nombreNormalizado);
                insertarCliente.setString(3, apellidoNormalizado);
                insertarCliente.setString(4, correoNormalizado);
                insertarCliente.setString(5, telefonoNormalizado);
                insertarCliente.executeUpdate();

                insertarUsuario.setString(1, correoNormalizado);
                insertarUsuario.setString(2, hashContrasena);
                insertarUsuario.setString(3, "registrado");
                insertarUsuario.setString(4, documentoNormalizado);
                insertarUsuario.executeUpdate();
            }

            conexion.commit();

            return """
                    {
                      "titulo":"Registro completado",
                      "mensaje":"La cuenta de %s ya esta activa. Puedes iniciar sesion desde este mismo frontal.",
                      "correo":"%s",
                      "rol":"registrado"
                    }
                    """.formatted(
                    UtilJson.escapar(nombreNormalizado),
                    UtilJson.escapar(correoNormalizado)
            );
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    /**
     * Da de alta un cliente profesional HORECA desde el perfil maestro.
     *
     * @param documento DNI del contacto profesional
     * @param nombreContacto nombre del contacto
     * @param apellidoContacto primer apellido del contacto
     * @param empresa nombre comercial de la empresa
     * @param cif identificador fiscal de la empresa
     * @param correo correo profesional de acceso
     * @param telefono telefono opcional de contacto
     * @param contrasena contrasena inicial
     * @return JSON de confirmacion del alta HORECA
     * @throws SQLException si falla la transaccion de alta
     */
    public String altaClienteHoreca(
            String documento,
            String nombreContacto,
            String apellidoContacto,
            String empresa,
            String cif,
            String correo,
            String telefono,
            String contrasena
    ) throws SQLException {
        validarDocumento(documento, "El DNI del contacto es obligatorio.");
        validarTexto(nombreContacto, "El nombre del contacto es obligatorio.");
        validarTexto(apellidoContacto, "El primer apellido del contacto es obligatorio.");
        validarTexto(empresa, "La empresa es obligatoria.");
        validarDocumento(cif, "El CIF es obligatorio.");
        validarCorreo(correo);
        validarContrasena(contrasena);

        String documentoNormalizado = normalizarDocumento(documento);
        String nombreNormalizado = nombreContacto.trim();
        String apellidoNormalizado = apellidoContacto.trim();
        String empresaNormalizada = empresa.trim();
        String cifNormalizado = normalizarDocumento(cif);
        String correoNormalizado = correo.trim().toLowerCase();
        String telefonoNormalizado = limpiarSiVacio(telefono);
        String hashContrasena = Contrasenas.sha256(contrasena.trim());

        var conexion = baseDeDatos.obtenerConexion();
        boolean autocommitOriginal = conexion.getAutoCommit();

        try {
            conexion.setAutoCommit(false);
            asegurarTablaClienteHoreca();
            validarDisponibilidadRegistro(correoNormalizado, documentoNormalizado);

            try (PreparedStatement insertarCliente = conexion.prepareStatement(SQL_INSERTAR_CLIENTE);
                 PreparedStatement insertarUsuario = conexion.prepareStatement(SQL_INSERTAR_USUARIO);
                 PreparedStatement insertarClienteHoreca = conexion.prepareStatement(SQL_INSERTAR_CLIENTE_HORECA)) {
                insertarCliente.setString(1, documentoNormalizado);
                insertarCliente.setString(2, nombreNormalizado);
                insertarCliente.setString(3, apellidoNormalizado);
                insertarCliente.setString(4, correoNormalizado);
                insertarCliente.setString(5, telefonoNormalizado);
                insertarCliente.executeUpdate();

                insertarUsuario.setString(1, correoNormalizado);
                insertarUsuario.setString(2, hashContrasena);
                insertarUsuario.setString(3, "horeca");
                insertarUsuario.setString(4, documentoNormalizado);
                insertarUsuario.executeUpdate();

                insertarClienteHoreca.setString(1, documentoNormalizado);
                insertarClienteHoreca.setString(2, empresaNormalizada);
                insertarClienteHoreca.setString(3, cifNormalizado);
                insertarClienteHoreca.setString(4, correoNormalizado);
                insertarClienteHoreca.setString(5, telefonoNormalizado);
                insertarClienteHoreca.executeUpdate();
            }

            conexion.commit();

            return """
                    {
                      "titulo":"Cliente HORECA creado",
                      "mensaje":"%s ya dispone de acceso profesional activo.",
                      "correo":"%s",
                      "empresa":"%s",
                      "rol":"horeca"
                    }
                    """.formatted(
                    UtilJson.escapar(nombreNormalizado + " " + apellidoNormalizado),
                    UtilJson.escapar(correoNormalizado),
                    UtilJson.escapar(empresaNormalizada)
            );
        } catch (Exception excepcion) {
            conexion.rollback();
            throw excepcion;
        } finally {
            conexion.setAutoCommit(autocommitOriginal);
        }
    }

    private String construirNombreVisible(String nombre, String apellido1, String valorPorDefecto) {
        String nombreCompuesto = ((nombre == null ? "" : nombre.trim()) + " " + (apellido1 == null ? "" : apellido1.trim())).trim();
        return nombreCompuesto.isBlank() ? valorPorDefecto : nombreCompuesto;
    }

    private String normalizarRol(String rolBruto) {
        if (rolBruto == null) {
            return "publico";
        }

        return switch (rolBruto.trim().toLowerCase()) {
            case "member", "registrado" -> "registrado";
            case "horeca" -> "horeca";
            case "master", "maestro", "comercial" -> "maestro";
            default -> rolBruto.trim().toLowerCase();
        };
    }

    private void validarDisponibilidadRegistro(String correo, String documento) throws SQLException {
        if (existeCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }

        if (existeDocumento(documento)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese documento.");
        }
    }

    private boolean existeCorreo(String correo) throws SQLException {
        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_EXISTE_CORREO)) {
            sentencia.setString(1, correo);

            try (ResultSet resultados = sentencia.executeQuery()) {
                return resultados.next();
            }
        }
    }

    private boolean existeDocumento(String documento) throws SQLException {
        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_EXISTE_DOCUMENTO)) {
            sentencia.setString(1, documento);

            try (ResultSet resultados = sentencia.executeQuery()) {
                return resultados.next();
            }
        }
    }

    private void asegurarTablaClienteHoreca() throws SQLException {
        try (Statement sentencia = baseDeDatos.obtenerConexion().createStatement()) {
            sentencia.executeUpdate(SQL_CREAR_TABLA_CLIENTE_HORECA);
        }
    }

    private void asegurarTablaSesiones() throws SQLException {
        try (Statement sentencia = baseDeDatos.obtenerConexion().createStatement()) {
            sentencia.executeUpdate(SQL_CREAR_TABLA_SESIONES);
        }
    }

    private void registrarSesionPersistente(UsuarioSesion sesion, Instant fechaCreacion) throws SQLException {
        Connection conexion = baseDeDatos.obtenerConexion();

        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_SESION)) {
            sentencia.setString(1, sesion.token());
            sentencia.setString(2, sesion.usuario());
            sentencia.setTimestamp(3, Timestamp.from(fechaCreacion));
            sentencia.executeUpdate();
        }
    }

    private void limpiarSesionesCaducadas() throws SQLException {
        Instant fechaLimite = Instant.now().minus(DURACION_MAXIMA_SESION);

        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_ELIMINAR_SESIONES_CADUCADAS)) {
            sentencia.setTimestamp(1, Timestamp.from(fechaLimite));
            sentencia.executeUpdate();
        }
    }

    private void eliminarSesion(String token) throws SQLException {
        try (PreparedStatement sentencia = baseDeDatos.obtenerConexion().prepareStatement(SQL_ELIMINAR_SESION)) {
            sentencia.setString(1, token);
            sentencia.executeUpdate();
        }
    }

    private void validarCorreo(String correo) {
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            throw new IllegalArgumentException("El correo es obligatorio y debe tener un formato valido.");
        }
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        ReglasNegocio.validarContrasena(contrasena);
    }

    private void validarDocumento(String documento, String mensajeError) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }

        if (!normalizarDocumento(documento).matches("[A-Z0-9]{5,20}")) {
            throw new IllegalArgumentException("El documento o CIF debe tener entre 5 y 20 caracteres alfanumericos.");
        }
    }

    private void validarTexto(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private String normalizarDocumento(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private String limpiarSiVacio(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isBlank() ? null : valorNormalizado;
    }
}
