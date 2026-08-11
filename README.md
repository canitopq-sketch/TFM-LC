# Linea Cano

Proyecto web con parte visual en HTML, CSS y JavaScript, y backend en Java para autenticacion por roles y consulta de disponibilidad sobre MySQL.

## Estructura principal

- `index.html`
  Interfaz principal del proyecto.
- `style.css`
  Estilos de la web.
- `script.js`
  Lógica de la interfaz en el navegador.
- `src/com/lineacano/servidor`
  Backend Java.
- `sql/auth_schema.sql`
  Esquema de usuarios de acceso y roles.
- `scripts/import_db.sh`
  Script de importacion de la base de datos.
- `scripts/run_backend.sh`
  Script de compilacion y arranque del backend.

## Qué hace ahora mismo

- Muestra la landing del proyecto.
- Permite iniciar sesion con usuario y contrasena mediante JSON.
- Mantiene una vista publica centrada en experiencia, producto y marca.
- Desbloquea zonas privadas diferenciadas para roles `registrado`, `horeca` y `maestro`.
- Permite consultar disponibilidad real desde la base de datos.
- Permite crear una reserva real de prueba en las tablas `reserva` y `reserva_habitacion`.
- Permite listar y cancelar reservas futuras desde el area privada del cliente.
- Incorpora una tienda privada con carrito en navegador y pedidos persistentes en MySQL.
- Separa el panel HORECA profesional del panel de cliente particular.
- Presenta un panel comercial con disponibilidad, alta HORECA y mapa inicial de backoffice.
- Persiste las sesiones en MySQL y valida el token aunque el servidor se reinicie.
- Sirve la web y la API desde un servidor Java ligero.

## Requisitos

- Java 23 o compatible
- Maven 3.9 o compatible (recomendado para compilar y ejecutar las pruebas)
- MySQL local
- Driver JDBC de MySQL (Maven lo descarga automaticamente)

En este equipo se ha localizado el driver en:

`/Users/adri/Java/mysql-connector-j-9.6.0.jar`

## Configuración

1. Crea el fichero de configuracion si no existe:

```bash
cp config/application.properties.example config/application.properties
```

2. Revisa estos valores:

```properties
app.puerto=8080
app.raizWeb=.

bd.url=jdbc:mysql://localhost:3306/linea_cano?serverTimezone=Europe/Madrid&useSSL=false&allowPublicKeyRetrieval=true
bd.usuario=root
bd.contrasena=TU_CONTRASENA
```

## Importar la base de datos

```bash
zsh scripts/import_db.sh
```

Este script importa:

- El volcado principal `backup_2026-03-20_11-35.sql`
- La tabla de accesos `sql/auth_schema.sql`

## Usuarios de prueba

- `cliente@lineacano.com` / `cliente123`
- `horeca@lineacano.com` / `horeca123`
- `comercial@lineacano.com` / `master123`
- Los usuarios HORECA tambien se pueden crear desde el panel comercial.

## Arrancar el backend

```bash
zsh scripts/run_backend.sh
```

También se puede compilar y ejecutar el plan de pruebas de forma reproducible con Maven:

```bash
mvn clean test
mvn javadoc:javadoc
```

Después abre:

`http://localhost:8080`

## Endpoints actuales

- `GET /api/salud`
- `POST /api/sesion/iniciar`
- `GET /api/sesion/validar`
- `GET /api/disponibilidad?fechaEntrada=2026-06-01&fechaSalida=2026-06-05&huespedes=2`
- `POST /api/reservas`
- `GET /api/reservas/mis-reservas`
- `POST /api/reservas/cancelar`
- `POST /api/pedidos`
- `GET /api/pedidos/mis-pedidos`

### Ejemplo de login JSON

```json
{
  "usuario": "cliente@lineacano.com",
  "contrasena": "cliente123"
}
```

### Ejemplo de reserva JSON

```json
{
  "fechaEntrada": "2026-06-20",
  "fechaSalida": "2026-06-22",
  "huespedes": 2
}
```

## Ideas para siguientes iteraciones

- Edicion avanzada de reservas
- Gestión de habitaciones y salones
- Pedidos HORECA persistentes en base de datos
- Catalogo profesional con tarifas por cliente
- Registro real de usuarios
- Integracion futura con cierres de sesion y caducidad configurable
