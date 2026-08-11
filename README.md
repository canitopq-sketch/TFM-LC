# Linea Cano

Aplicación web del TFG Linea Cano. Incluye una web pública, áreas privadas por rol, reservas, pedidos y persistencia en MySQL. El backend está desarrollado en Java y la interfaz utiliza HTML, CSS y JavaScript.

## Requisitos

- Git.
- JDK 23 o posterior con capacidad para compilar con Java 23.
- Maven 3.9 o posterior.
- MySQL 8.0 o posterior.
- `zsh` para utilizar los scripts incluidos.
- Conexión a Internet durante la primera ejecución de Maven para descargar las dependencias.

Los scripts están preparados para macOS o Linux con `zsh`. En Windows deben ejecutarse los comandos equivalentes de forma manual.

Comprueba las herramientas instaladas:

```bash
git --version
java -version
mvn -version
mysql --version
zsh --version
```

Maven descarga y utiliza automáticamente MySQL Connector/J. No es necesario descargar ni configurar manualmente ningún archivo `.jar`.

## 1. Clonar el repositorio

```bash
git clone https://github.com/canitopq-sketch/TFM-LC.git
cd TFM-LC
git switch cierre-tfg
```

Mientras el trabajo de cierre permanezca en `cierre-tfg`, debe utilizarse esa rama. Cuando se integre en la rama principal, el último comando dejará de ser necesario.

## 2. Arrancar MySQL

MySQL debe estar iniciado antes de importar los datos o arrancar Linea Cano.

Con la instalación oficial de MySQL en macOS puede utilizarse:

```bash
sudo /usr/local/mysql/support-files/mysql.server start
```

Si MySQL se instaló con Homebrew:

```bash
brew services start mysql
```

Comprueba la conexión:

```bash
mysql -u root -p -e "SELECT VERSION();"
```

## 3. Configurar la conexión

Crea la configuración local a partir del ejemplo:

```bash
cp config/application.properties.example config/application.properties
```

Edita `config/application.properties` y escribe el usuario y la contraseña de MySQL de tu equipo:

```properties
app.puerto=8080
app.raizWeb=.

bd.url=jdbc:mysql://localhost:3306/linea_cano?serverTimezone=Europe/Madrid&useSSL=false&allowPublicKeyRetrieval=true
bd.usuario=root
bd.contrasena=TU_CONTRASENA
```

Este archivo contiene datos locales y está excluido de Git.

La configuración también puede sobrescribirse mediante las variables `APP_PUERTO`, `APP_RAIZWEB`, `BD_URL`, `BD_USUARIO` y `BD_CONTRASENA`.

## 4. Importar la base de datos

Desde la raíz del proyecto ejecuta:

```bash
zsh scripts/import_db.sh
```

El script solicita la contraseña de MySQL en cada operación y realiza estos pasos:

1. Crea la base `linea_cano` si no existe.
2. Importa `backup_2026-03-20_11-35.sql`.
3. Aplica `sql/auth_schema.sql` con usuarios, roles, sesiones, HORECA y pedidos.

Comprueba que se han cargado las diez habitaciones:

```bash
mysql -u root -p linea_cano -e "SELECT COUNT(*) AS habitaciones FROM habitacion;"
```

La migración `sql/migracion_autenticacion_es.sql` no se utiliza en una instalación nueva. Se conserva únicamente para actualizar una base antigua con nombres de autenticación en inglés:

```bash
mysql -u root -p linea_cano < sql/migracion_autenticacion_es.sql
```

## 5. Compilar y ejecutar las pruebas

```bash
mvn clean test
```

El resultado esperado es:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 6. Generar JavaDoc

```bash
mvn javadoc:javadoc
```

La página principal se genera en:

```text
target/reports/apidocs/index.html
```

En macOS puede abrirse con:

```bash
open target/reports/apidocs/index.html
```

## 7. Arrancar Linea Cano

```bash
zsh scripts/run_backend.sh
```

El script se sitúa automáticamente en la raíz del proyecto y utiliza Maven para compilar, resolver MySQL Connector/J y ejecutar `com.lineacano.servidor.Aplicacion`.

Cuando aparezca este mensaje, la aplicación estará disponible:

```text
Servidor Linea Cano disponible en http://localhost:8080
```

Abre en el navegador:

```text
http://localhost:8080
```

## 8. Usuarios de demostración

| Perfil | Correo | Contraseña |
|---|---|---|
| Cliente | `cliente@lineacano.com` | `cliente123` |
| Profesional HORECA | `horeca@lineacano.com` | `horeca123` |
| Maestro/comercial | `comercial@lineacano.com` | `master123` |

Desde el panel maestro también se pueden crear nuevas cuentas HORECA.

## 9. Detener el servidor

Pulsa `Control + C` en la terminal donde está ejecutándose Linea Cano.

## API principal

- `GET /api/salud`
- `POST /api/sesion/iniciar`
- `GET /api/sesion/validar`
- `POST /api/sesion/cerrar`
- `POST /api/usuarios/registro`
- `POST /api/admin/clientes-horeca`
- `GET /api/disponibilidad`
- `POST /api/reservas`
- `GET /api/reservas/mis-reservas`
- `POST /api/reservas/cancelar`
- `POST /api/pedidos`
- `GET /api/pedidos/mis-pedidos`

## Problemas frecuentes

### MySQL muestra `Access denied for user`

Comprueba `bd.usuario` y `bd.contrasena` en `config/application.properties`. Verifica también las credenciales directamente:

```bash
mysql -u root -p -e "SELECT 1;"
```

### Aparece `Communications link failure`

MySQL no está iniciado o no acepta conexiones en `localhost:3306`. Arranca el servicio y repite la comprobación de conexión.

### Aparece `Unknown database 'linea_cano'`

Importa la base de datos:

```bash
zsh scripts/import_db.sh
```

### La terminal indica `mysql: command not found`

Instala el cliente de MySQL o añade su carpeta de ejecutables al `PATH`.

### Maven no compila con la versión de Java

Comprueba el JDK utilizado por Maven:

```bash
mvn -version
```

Debe mostrar Java 23 o posterior.

### Maven no puede descargar dependencias

Comprueba la conexión a Internet y vuelve a ejecutar el comando. Las dependencias descargadas quedan guardadas en el repositorio local de Maven.

### El puerto 8080 está ocupado

Cambia el puerto en `config/application.properties`:

```properties
app.puerto=8081
```

Después abre `http://localhost:8081`.

### El script crea la configuración y se detiene

Es el comportamiento esperado la primera vez. Edita `config/application.properties` con las credenciales de MySQL y vuelve a ejecutar:

```bash
zsh scripts/run_backend.sh
```

## Estructura principal

- `src/com/lineacano/servidor`: backend Java, controladores, servicios y DAO.
- `tests/com/lineacano/servidor`: pruebas JUnit.
- `*.html`, `*.js` y `style.css`: vistas e interacción del navegador.
- `config/application.properties.example`: configuración de ejemplo.
- `backup_2026-03-20_11-35.sql`: esquema y datos principales.
- `sql/auth_schema.sql`: autenticación, roles, sesiones, HORECA y pedidos.
- `scripts/import_db.sh`: importación de MySQL.
- `scripts/run_backend.sh`: arranque mediante Maven.
- `DOCUMENTO_FUNCIONAL.md`: descripción funcional del proyecto.

## Trabajos futuros

- Edición avanzada de reservas.
- Gestión de habitaciones y salones desde el panel maestro.
- Tarifas profesionales diferenciadas por cliente HORECA.
- Gestión ampliada de estados de pedido.
- Despliegue externo si lo solicita el tutor.
