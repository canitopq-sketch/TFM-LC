package com.lineacano.servidor;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Aplica las reglas de autenticacion, sesiones y altas de usuarios.
 * Delega la persistencia en {@link UsuarioDao} y {@link SesionDao}.
 */
public final class ServicioAutenticacion {
    private static final Duration DURACION_MAXIMA_SESION = Duration.ofDays(7);

    private final UsuarioDao usuarioDao;
    private final SesionDao sesionDao;

    /**
     * Crea el servicio con los DAO de usuarios y sesiones.
     *
     * @param usuarioDao acceso a los usuarios registrados
     * @param sesionDao acceso a las sesiones persistentes
     */
    public ServicioAutenticacion(UsuarioDao usuarioDao, SesionDao sesionDao) {
        this.usuarioDao = usuarioDao;
        this.sesionDao = sesionDao;
    }

    /**
     * Valida las credenciales y crea una sesion persistente.
     *
     * @param usuario correo introducido por el usuario
     * @param contrasena contrasena en texto plano
     * @return sesion creada con su token y rol
     * @throws SQLException si falla el acceso a los datos
     * @throws IllegalArgumentException si las credenciales no son validas
     */
    public UsuarioSesion iniciarSesion(String usuario, String contrasena) throws SQLException {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }

        ReglasNegocio.validarContrasena(contrasena);
        String correoNormalizado = usuario.trim().toLowerCase();
        String hashContrasena = Contrasenas.sha256(contrasena);

        try {
            DatosAccesoUsuario datos = usuarioDao.buscarPorCorreo(correoNormalizado)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos."));

            if (!datos.activo()) {
                throw new IllegalArgumentException("El usuario no esta activo.");
            }
            if (!hashContrasena.equalsIgnoreCase(datos.contrasenaHash())) {
                throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
            }

            UsuarioSesion sesion = new UsuarioSesion(
                    UUID.randomUUID().toString(),
                    correoNormalizado,
                    construirNombreVisible(datos.nombre(), datos.apellido(), correoNormalizado),
                    normalizarRol(datos.rol()),
                    datos.idDni(),
                    Instant.now()
            );
            sesionDao.registrar(sesion);
            return sesion;
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
     * Recupera una sesion activa y descarta las sesiones caducadas.
     *
     * @param token identificador enviado por el navegador
     * @return sesion valida, o vacio si no existe o no puede utilizarse
     */
    public Optional<UsuarioSesion> buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            sesionDao.eliminarCaducadas(Instant.now().minus(DURACION_MAXIMA_SESION));
            Optional<DatosSesion> resultado = sesionDao.buscarPorToken(token.trim());
            if (resultado.isEmpty()) {
                return Optional.empty();
            }

            DatosSesion datos = resultado.get();
            if (!datos.activo() || datos.fechaCreacion().plus(DURACION_MAXIMA_SESION).isBefore(Instant.now())) {
                sesionDao.eliminar(token.trim());
                return Optional.empty();
            }

            return Optional.of(new UsuarioSesion(
                    datos.token(),
                    datos.correo(),
                    construirNombreVisible(datos.nombre(), datos.apellido(), datos.correo()),
                    normalizarRol(datos.rol()),
                    datos.idDni(),
                    datos.fechaCreacion()
            ));
        } catch (SQLException excepcion) {
            return Optional.empty();
        }
    }

    /**
     * Elimina la sesion asociada al token actual.
     *
     * @param token identificador de la sesion
     * @return {@code true} si la sesion existia
     * @throws SQLException si falla la eliminacion
     * @throws IllegalArgumentException si no se recibe un token
     */
    public boolean cerrarSesion(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de sesion es obligatorio.");
        }
        return sesionDao.eliminar(token.trim());
    }

    /**
     * Valida y registra una cuenta de cliente particular.
     *
     * @param documento DNI o documento identificativo
     * @param nombre nombre del cliente
     * @param apellido primer apellido
     * @param correo correo utilizado para acceder
     * @param telefono telefono opcional
     * @param contrasena contrasena inicial
     * @return confirmacion del alta en formato JSON
     * @throws SQLException si falla el registro
     * @throws IllegalArgumentException si los datos no son validos o ya existen
     */
    public String registrarNuevoUsuario(
            String documento, String nombre, String apellido, String correo,
            String telefono, String contrasena
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

        validarDisponibilidadRegistro(correoNormalizado, documentoNormalizado);
        usuarioDao.registrarCliente(
                documentoNormalizado, nombreNormalizado, apellidoNormalizado, correoNormalizado,
                telefonoNormalizado, Contrasenas.sha256(contrasena.trim()), "registrado"
        );

        return """
                {
                  "titulo":"Registro completado",
                  "mensaje":"La cuenta de %s ya esta activa. Puedes iniciar sesion desde este mismo frontal.",
                  "correo":"%s",
                  "rol":"registrado"
                }
                """.formatted(UtilJson.escapar(nombreNormalizado), UtilJson.escapar(correoNormalizado));
    }

    /**
     * Valida y registra una cuenta profesional HORECA.
     *
     * @param documento DNI del contacto
     * @param nombreContacto nombre del contacto
     * @param apellidoContacto primer apellido del contacto
     * @param empresa nombre de la empresa
     * @param cif identificador fiscal de la empresa
     * @param correo correo profesional de acceso
     * @param telefono telefono opcional
     * @param contrasena contrasena inicial
     * @return confirmacion del alta en formato JSON
     * @throws SQLException si falla el registro
     * @throws IllegalArgumentException si los datos no son validos o ya existen
     */
    public String altaClienteHoreca(
            String documento, String nombreContacto, String apellidoContacto, String empresa,
            String cif, String correo, String telefono, String contrasena
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

        validarDisponibilidadRegistro(correoNormalizado, documentoNormalizado);
        usuarioDao.registrarClienteHoreca(
                documentoNormalizado, nombreNormalizado, apellidoNormalizado, empresaNormalizada,
                cifNormalizado, correoNormalizado, telefonoNormalizado, Contrasenas.sha256(contrasena.trim())
        );

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
    }

    private void validarDisponibilidadRegistro(String correo, String documento) throws SQLException {
        if (usuarioDao.existeCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }
        if (usuarioDao.existeDocumento(documento)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese documento.");
        }
    }

    private String construirNombreVisible(String nombre, String apellido, String valorPorDefecto) {
        String nombreCompuesto = ((nombre == null ? "" : nombre.trim()) + " "
                + (apellido == null ? "" : apellido.trim())).trim();
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
