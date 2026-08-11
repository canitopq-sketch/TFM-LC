package com.lineacano.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private static final class UsuarioDaoPrueba extends UsuarioDao {
        private DatosAccesoUsuario usuario;
        private String documentoRegistrado;
        private String cifRegistrado;
        private String correoRegistrado;

        private UsuarioDaoPrueba() {
            super(null);
        }

        @Override
        public Optional<DatosAccesoUsuario> buscarPorCorreo(String correo) {
            return Optional.ofNullable(usuario);
        }

        @Override
        public boolean existeCorreo(String correo) {
            return false;
        }

        @Override
        public boolean existeDocumento(String documento) {
            return false;
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

        private SesionDaoPrueba() {
            super(null);
        }

        @Override
        public void registrar(UsuarioSesion sesion) {
            sesionRegistrada = sesion;
        }

        @Override
        public Optional<DatosSesion> buscarPorToken(String token) {
            return Optional.empty();
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
