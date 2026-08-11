package com.lineacano.servidor;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Punto de entrada del servidor HTTP ligero de Linea Cano.
 *
 * <p>Inicializa configuracion, conexion a base de datos, servicios de negocio y
 * rutas publicas/API utilizadas por el prototipo funcional del TFM.</p>
 */
public final class Aplicacion {
    private Aplicacion() {
    }

    /**
     * Arranca el servidor web y registra los controladores de API y archivos estaticos.
     *
     * @param args argumentos de linea de comandos no utilizados
     * @throws Exception si falla la carga de configuracion, la base de datos o el servidor HTTP
     */
    public static void main(String[] args) throws Exception {
        // Carga la configuración y los servicios principales de la aplicación.
        Configuracion configuracion = Configuracion.cargar();
        BaseDeDatos baseDeDatos = new BaseDeDatos(configuracion);
        ServicioReservas servicioReservas = new ServicioReservas(baseDeDatos);
        ServicioAutenticacion servicioAutenticacion = new ServicioAutenticacion(baseDeDatos);
        ServicioPedidos servicioPedidos = new ServicioPedidos(baseDeDatos);

        // Se crea un servidor HTTP ligero suficiente para el prototipo del TFM.
        HttpServer servidor = HttpServer.create(new InetSocketAddress(configuracion.puerto()), 0);
        servidor.createContext("/api/salud", intercambio -> escribirJson(intercambio, 200, "{\"estado\":\"ok\"}"));
        servidor.createContext("/api/sesion/iniciar", new ControladorInicioSesion(servicioAutenticacion));
        servidor.createContext("/api/sesion/validar", new ControladorValidacionSesion(servicioAutenticacion));
        servidor.createContext("/api/sesion/cerrar", new ControladorCierreSesion(servicioAutenticacion));
        servidor.createContext("/api/usuarios/registro", new ControladorRegistroUsuario(servicioAutenticacion));
        servidor.createContext("/api/admin/clientes-horeca", new ControladorAltaHoreca(servicioAutenticacion));
        servidor.createContext("/api/disponibilidad", new ControladorDisponibilidad(servicioReservas, servicioAutenticacion));
        servidor.createContext("/api/reservas", new ControladorReserva(servicioReservas, servicioAutenticacion));
        servidor.createContext("/api/reservas/mis-reservas", new ControladorMisReservas(servicioReservas, servicioAutenticacion));
        servidor.createContext("/api/reservas/cancelar", new ControladorCancelarReserva(servicioReservas, servicioAutenticacion));
        servidor.createContext("/api/pedidos", new ControladorPedido(servicioPedidos, servicioAutenticacion));
        servidor.createContext("/api/pedidos/mis-pedidos", new ControladorMisPedidos(servicioPedidos, servicioAutenticacion));
        servidor.createContext("/", new ControladorArchivosEstaticos(configuracion.raizWeb()));
        servidor.setExecutor(Executors.newCachedThreadPool());

        Runtime.getRuntime().addShutdownHook(new Thread(baseDeDatos::cerrar));
        servidor.start();

        System.out.println("Servidor Linea Cano disponible en http://localhost:" + configuracion.puerto());
        System.out.println("Raiz de archivos estaticos: " + configuracion.raizWeb());
    }

    private static final class ControladorValidacionSesion implements HttpHandler {
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorValidacionSesion(ServicioAutenticacion servicioAutenticacion) {
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"La sesion no es valida o ha caducado.\"}");
                return;
            }

            escribirJson(intercambio, 200, sesion.get().aJson());
        }
    }

    private static final class ControladorInicioSesion implements HttpHandler {
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorInicioSesion(ServicioAutenticacion servicioAutenticacion) {
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            // El login llega como formulario simple enviado desde la web.
            Map<String, String> cuerpo = analizarCuerpo(intercambio);
            String usuario = cuerpo.get("usuario");
            String contrasena = cuerpo.get("contrasena");

            try {
                UsuarioSesion sesion = servicioAutenticacion.iniciarSesion(usuario, contrasena);
                escribirJson(intercambio, 200, sesion.aJson());
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 401, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorCierreSesion implements HttpHandler {
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorCierreSesion(ServicioAutenticacion servicioAutenticacion) {
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            String token = intercambio.getRequestHeaders().getFirst("X-Linea-Token");

            try {
                servicioAutenticacion.cerrarSesion(token);
                escribirJson(intercambio, 200, "{\"mensaje\":\"Sesion cerrada correctamente.\"}");
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"No se pudo cerrar la sesion.\"}");
            }
        }
    }

    private static final class ControladorRegistroUsuario implements HttpHandler {
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorRegistroUsuario(ServicioAutenticacion servicioAutenticacion) {
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Map<String, String> cuerpo = analizarCuerpo(intercambio);

            try {
                String json = servicioAutenticacion.registrarNuevoUsuario(
                        cuerpo.get("documento"),
                        cuerpo.get("nombre"),
                        cuerpo.get("apellido"),
                        cuerpo.get("correo"),
                        cuerpo.get("telefono"),
                        cuerpo.get("contrasena")
                );
                escribirJson(intercambio, 201, json);
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorAltaHoreca implements HttpHandler {
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorAltaHoreca(ServicioAutenticacion servicioAutenticacion) {
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion como administrador para dar de alta clientes HORECA.\"}");
                return;
            }

            if (!"maestro".equals(sesion.get().rol())) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu perfil no puede crear clientes HORECA.\"}");
                return;
            }

            Map<String, String> cuerpo = analizarCuerpo(intercambio);

            try {
                String json = servicioAutenticacion.altaClienteHoreca(
                        cuerpo.get("documento"),
                        cuerpo.get("nombreContacto"),
                        cuerpo.get("apellidoContacto"),
                        cuerpo.get("empresa"),
                        cuerpo.get("cif"),
                        cuerpo.get("correo"),
                        cuerpo.get("telefono"),
                        cuerpo.get("contrasena")
                );
                escribirJson(intercambio, 201, json);
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorReserva implements HttpHandler {
        private final ServicioReservas servicioReservas;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorReserva(ServicioReservas servicioReservas, ServicioAutenticacion servicioAutenticacion) {
            this.servicioReservas = servicioReservas;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para crear una reserva.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"maestro".equals(rol) && !"horeca".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para crear reservas.\"}");
                return;
            }

            Map<String, String> cuerpo = analizarCuerpo(intercambio);
            String fechaEntrada = cuerpo.get("fechaEntrada");
            String fechaSalida = cuerpo.get("fechaSalida");
            int huespedes = analizarEntero(cuerpo.get("huespedes"), 2);

            if (fechaEntrada == null || fechaSalida == null) {
                escribirJson(intercambio, 400, "{\"error\":\"Faltan fechaEntrada o fechaSalida\"}");
                return;
            }

            try {
                String json = servicioReservas.crearReserva(
                        LocalDate.parse(fechaEntrada),
                        LocalDate.parse(fechaSalida),
                        huespedes,
                        sesion.get().idDni()
                );
                escribirJson(intercambio, 201, json);
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorMisReservas implements HttpHandler {
        private final ServicioReservas servicioReservas;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorMisReservas(ServicioReservas servicioReservas, ServicioAutenticacion servicioAutenticacion) {
            this.servicioReservas = servicioReservas;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para consultar tus reservas.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"horeca".equals(rol) && !"maestro".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para ver reservas.\"}");
                return;
            }

            try {
                escribirJson(intercambio, 200, servicioReservas.listarReservasCliente(sesion.get().idDni()));
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorCancelarReserva implements HttpHandler {
        private final ServicioReservas servicioReservas;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorCancelarReserva(ServicioReservas servicioReservas, ServicioAutenticacion servicioAutenticacion) {
            this.servicioReservas = servicioReservas;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para cancelar una reserva.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"horeca".equals(rol) && !"maestro".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para cancelar reservas.\"}");
                return;
            }

            Map<String, String> cuerpo = analizarCuerpo(intercambio);
            int idReserva = analizarEntero(cuerpo.get("idReserva"), -1);
            if (idReserva < 1) {
                escribirJson(intercambio, 400, "{\"error\":\"Falta un idReserva valido.\"}");
                return;
            }

            try {
                escribirJson(intercambio, 200, servicioReservas.cancelarReserva(idReserva, sesion.get().idDni()));
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorDisponibilidad implements HttpHandler {
        private final ServicioReservas servicioReservas;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorDisponibilidad(ServicioReservas servicioReservas, ServicioAutenticacion servicioAutenticacion) {
            this.servicioReservas = servicioReservas;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para consultar disponibilidad.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"maestro".equals(rol) && !"horeca".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para ver disponibilidad.\"}");
                return;
            }

            Map<String, String> consulta = analizarConsulta(intercambio.getRequestURI());
            String fechaEntrada = consulta.get("fechaEntrada");
            String fechaSalida = consulta.get("fechaSalida");
            int huespedes = analizarHuespedes(consulta.get("huespedes"));

            if (fechaEntrada == null || fechaSalida == null) {
                escribirJson(intercambio, 400, "{\"error\":\"Faltan fechaEntrada o fechaSalida\"}");
                return;
            }

            try {
                LocalDate entrada = LocalDate.parse(fechaEntrada);
                LocalDate salida = LocalDate.parse(fechaSalida);
                ResultadoDisponibilidad resultado = servicioReservas.comprobarDisponibilidad(entrada, salida, huespedes);
                escribirJson(intercambio, 200, resultado.aJson());
            } catch (Exception excepcion) {
                String mensaje = UtilJson.escapar(excepcion.getMessage() == null ? "Error inesperado" : excepcion.getMessage());
                escribirJson(intercambio, 500, "{\"error\":\"" + mensaje + "\"}");
            }
        }

        private int analizarHuespedes(String valorBruto) {
            if (valorBruto == null || valorBruto.isBlank()) {
                return 2;
            }

            try {
                return Integer.parseInt(valorBruto);
            } catch (NumberFormatException ignorada) {
                return 2;
            }
        }
    }

    private static final class ControladorPedido implements HttpHandler {
        private final ServicioPedidos servicioPedidos;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorPedido(ServicioPedidos servicioPedidos, ServicioAutenticacion servicioAutenticacion) {
            this.servicioPedidos = servicioPedidos;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para crear un pedido.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"horeca".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para crear pedidos.\"}");
                return;
            }

            Map<String, String> cuerpo = analizarCuerpo(intercambio);

            try {
                escribirJson(intercambio, 201, servicioPedidos.crearPedido(
                        sesion.get().idDni(),
                        rol,
                        cuerpo.get("lineas")
                ));
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorMisPedidos implements HttpHandler {
        private final ServicioPedidos servicioPedidos;
        private final ServicioAutenticacion servicioAutenticacion;

        private ControladorMisPedidos(ServicioPedidos servicioPedidos, ServicioAutenticacion servicioAutenticacion) {
            this.servicioPedidos = servicioPedidos;
            this.servicioAutenticacion = servicioAutenticacion;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                escribirJson(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            Optional<UsuarioSesion> sesion = servicioAutenticacion.buscarPorToken(
                    intercambio.getRequestHeaders().getFirst("X-Linea-Token")
            );

            if (sesion.isEmpty()) {
                escribirJson(intercambio, 401, "{\"error\":\"Debes iniciar sesion para consultar tus pedidos.\"}");
                return;
            }

            String rol = sesion.get().rol();
            if (!"registrado".equals(rol) && !"horeca".equals(rol)) {
                escribirJson(intercambio, 403, "{\"error\":\"Tu rol no tiene permiso para ver pedidos.\"}");
                return;
            }

            try {
                escribirJson(intercambio, 200, servicioPedidos.listarPedidosCliente(sesion.get().idDni()));
            } catch (IllegalArgumentException excepcion) {
                escribirJson(intercambio, 400, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            } catch (Exception excepcion) {
                escribirJson(intercambio, 500, "{\"error\":\"" + UtilJson.escapar(excepcion.getMessage()) + "\"}");
            }
        }
    }

    private static final class ControladorArchivosEstaticos implements HttpHandler {
        private final Path raizWeb;

        private ControladorArchivosEstaticos(Path raizWeb) {
            this.raizWeb = raizWeb;
        }

        @Override
        public void handle(HttpExchange intercambio) throws IOException {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                intercambio.sendResponseHeaders(405, -1);
                return;
            }

            String rutaBruta = intercambio.getRequestURI().getPath();
            String rutaRelativa = rutaBruta.equals("/") ? "index.html" : rutaBruta.substring(1);
            Path destino = raizWeb.resolve(rutaRelativa).normalize();

            if (!destino.startsWith(raizWeb) || Files.isDirectory(destino) || !Files.exists(destino)) {
                destino = raizWeb.resolve("index.html");
            }

            byte[] cuerpo = Files.readAllBytes(destino);
            Headers cabeceras = intercambio.getResponseHeaders();
            cabeceras.set("Content-Type", obtenerTipoContenido(destino));
            aplicarCabecerasSinCache(cabeceras);
            intercambio.sendResponseHeaders(200, cuerpo.length);

            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        }

        private String obtenerTipoContenido(Path archivo) {
            String tipo = URLConnection.guessContentTypeFromName(archivo.getFileName().toString());
            return tipo == null ? "application/octet-stream" : tipo;
        }
    }

    private static Map<String, String> analizarConsulta(URI uri) {
        Map<String, String> resultado = new HashMap<>();
        String consulta = uri.getRawQuery();

        if (consulta == null || consulta.isBlank()) {
            return resultado;
        }

        String[] pares = consulta.split("&");
        for (String par : pares) {
            int separador = par.indexOf('=');
            if (separador <= 0) {
                continue;
            }

            String clave = decodificar(par.substring(0, separador));
            String valor = decodificar(par.substring(separador + 1));
            resultado.put(clave, valor);
        }

        return resultado;
    }

    private static Map<String, String> analizarCuerpo(HttpExchange intercambio) throws IOException {
        try (InputStream cuerpo = intercambio.getRequestBody()) {
            String contenido = new String(cuerpo.readAllBytes(), StandardCharsets.UTF_8);
            String tipoContenido = intercambio.getRequestHeaders().getFirst("Content-Type");

            if (tipoContenido != null && tipoContenido.toLowerCase().contains("application/json")) {
                return UtilJson.analizarObjetoSimple(contenido);
            }

            return analizarConsulta(URI.create("http://localhost/?" + contenido));
        }
    }

    private static int analizarEntero(String valorBruto, int valorPorDefecto) {
        if (valorBruto == null || valorBruto.isBlank()) {
            return valorPorDefecto;
        }

        try {
            return Integer.parseInt(valorBruto);
        } catch (NumberFormatException ignorada) {
            return valorPorDefecto;
        }
    }

    private static String decodificar(String valor) {
        return java.net.URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    static void escribirJson(HttpExchange intercambio, int estado, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
        Headers cabeceras = intercambio.getResponseHeaders();
        cabeceras.set("Content-Type", "application/json; charset=utf-8");
        aplicarCabecerasSinCache(cabeceras);
        intercambio.sendResponseHeaders(estado, cuerpo.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private static void aplicarCabecerasSinCache(Headers cabeceras) {
        cabeceras.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        cabeceras.set("Pragma", "no-cache");
        cabeceras.set("Expires", "0");
    }
}
