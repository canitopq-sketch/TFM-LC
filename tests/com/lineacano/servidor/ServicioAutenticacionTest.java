package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioAutenticacionTest {
    @Test
    void iniciaSesionYOrdenaPersistirlaAlDao() throws SQLException {
        UsuarioDaoPrueba usuarioDao = new UsuarioDaoPrueba();
        usuarioDao.usuario = new DatosAccesoUsuario(
                "cliente@lineacano.com", Contrasenas.sha256("cliente123"),
                "registrado", true, "11111111A", "Ana", "Cano"
        );
        SesionDaoPrueba sesionDao = new SesionDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(usuarioDao, sesionDao);

        UsuarioSesion sesion = servicio.iniciarSesion(" Cliente@LineaCano.com ", "cliente123");

        assertEquals("cliente@lineacano.com", sesion.usuario());
        assertEquals("Ana Cano", sesion.nombreVisible());
        assertEquals("registrado", sesion.rol());
        assertNotNull(sesionDao.sesionRegistrada);
    }

    @Test
    void daDeAltaHorecaConDatosNormalizados() throws SQLException {
        UsuarioDaoPrueba usuarioDao = new UsuarioDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(usuarioDao, new SesionDaoPrueba());

        String json = servicio.altaClienteHoreca(
                " 12345678a ", " Laura ", " Cano ", " Mesón Prueba ",
                " b12345678 ", " HORECA.PRUEBA@LINEACANO.COM ", "600000000", "clave123"
        );

        assertEquals("12345678A", usuarioDao.documentoRegistrado);
        assertEquals("B12345678", usuarioDao.cifRegistrado);
        assertEquals("horeca.prueba@lineacano.com", usuarioDao.correoRegistrado);
        assertTrue(json.contains("\"rol\":\"horeca\""));
    }

    @Test
    void cerrarSesionEliminaElTokenMedianteElDao() throws SQLException {
        SesionDaoPrueba sesionDao = new SesionDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(new UsuarioDaoPrueba(), sesionDao);

        assertTrue(servicio.cerrarSesion(" token-prueba "));
        assertEquals("token-prueba", sesionDao.tokenEliminado);
    }

    @Test
    void rechazaContrasenaIncorrectaOUsuarioInactivo() {
        UsuarioDaoPrueba usuarioDao = new UsuarioDaoPrueba();
        usuarioDao.usuario = new DatosAccesoUsuario(
                "cliente@lineacano.com", Contrasenas.sha256("cliente123"),
                "registrado", true, "11111111A", "Ana", "Cano"
        );
        SesionDaoPrueba sesionDao = new SesionDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(usuarioDao, sesionDao);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.iniciarSesion("cliente@lineacano.com", "incorrecta"));

        usuarioDao.usuario = new DatosAccesoUsuario(
                "cliente@lineacano.com", Contrasenas.sha256("cliente123"),
                "registrado", false, "11111111A", "Ana", "Cano"
        );
        assertThrows(IllegalArgumentException.class,
                () -> servicio.iniciarSesion("cliente@lineacano.com", "cliente123"));
        assertFalse(sesionDao.registroInvocado);
    }

    @Test
    void rechazaSesionInexistenteOCaducada() {
        SesionDaoPrueba sesionDao = new SesionDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(new UsuarioDaoPrueba(), sesionDao);

        assertTrue(servicio.buscarPorToken("token-inexistente").isEmpty());

        sesionDao.datosSesion = Optional.of(new DatosSesion(
                "token-caducado",
                Instant.now().minusSeconds(8 * 24 * 60 * 60),
                "cliente@lineacano.com",
                "registrado",
                true,
                "11111111A",
                "Ana",
                "Cano"
        ));
        assertTrue(servicio.buscarPorToken("token-caducado").isEmpty());
        assertEquals("token-caducado", sesionDao.tokenEliminado);
    }

    @Test
    void rechazaRegistroConCorreoODocumentoDuplicado() {
        UsuarioDaoPrueba usuarioDao = new UsuarioDaoPrueba();
        ServicioAutenticacion servicio = new ServicioAutenticacion(usuarioDao, new SesionDaoPrueba());

        usuarioDao.correoExistente = true;
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarNuevoUsuario(
                "12345678A", "Laura", "Cano", "duplicado@lineacano.com", "600000000", "clave123"
        ));

        usuarioDao.correoExistente = false;
        usuarioDao.documentoExistente = true;
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarNuevoUsuario(
                "12345678A", "Laura", "Cano", "nuevo@lineacano.com", "600000000", "clave123"
        ));
        assertFalse(usuarioDao.registroClienteInvocado);
    }

    private static final class UsuarioDaoPrueba extends UsuarioDao {
        private DatosAccesoUsuario usuario;
        private String documentoRegistrado;
        private String cifRegistrado;
        private String correoRegistrado;
        private boolean correoExistente;
        private boolean documentoExistente;
        private boolean registroClienteInvocado;

        private UsuarioDaoPrueba() {
            super(null);
        }

        @Override
        public Optional<DatosAccesoUsuario> buscarPorCorreo(String correo) {
            return Optional.ofNullable(usuario);
        }

        @Override
        public boolean existeCorreo(String correo) {
            return correoExistente;
        }

        @Override
        public boolean existeDocumento(String documento) {
            return documentoExistente;
        }

        @Override
        public void registrarCliente(
                String documento, String nombre, String apellido, String correo,
                String telefono, String hashContrasena, String rol
        ) {
            registroClienteInvocado = true;
        }

        @Override
        public void registrarClienteHoreca(
                String documento, String nombre, String apellido, String empresa,
                String cif, String correo, String telefono, String hashContrasena
        ) {
            documentoRegistrado = documento;
            cifRegistrado = cif;
            correoRegistrado = correo;
        }
    }

    private static final class SesionDaoPrueba extends SesionDao {
        private UsuarioSesion sesionRegistrada;
        private String tokenEliminado;
        private boolean registroInvocado;
        private Optional<DatosSesion> datosSesion = Optional.empty();

        private SesionDaoPrueba() {
            super(null);
        }

        @Override
        public void registrar(UsuarioSesion sesion) {
            sesionRegistrada = sesion;
            registroInvocado = true;
        }

        @Override
        public Optional<DatosSesion> buscarPorToken(String token) {
            return datosSesion;
        }

        @Override
        public boolean eliminar(String token) {
            tokenEliminado = token;
            return true;
        }

        @Override
        public void eliminarCaducadas(Instant fechaLimite) {
        }
    }
}
