# Documento Funcional

## Proyecto

**Nombre:** Linea Cano  
**Tipo de proyecto:** Ecosistema digital para TFM  
**Ámbito:** Marca agroganadera, producto premium, hospitality, reservas, eventos y acceso profesional

## 1. Visión General

Linea Cano se plantea como un ecosistema digital de marca construido alrededor de la dehesa española. La web no actúa solo como escaparate, sino como punto de entrada a varias líneas de negocio conectadas entre sí:

- ganaderías de origen
- producto premium
- hospitality y reservas
- eventos corporativos y bodas
- acceso profesional HORECA

La propuesta combina narrativa de marca, identidad visual, consulta de disponibilidad, reserva real y acceso por perfiles.

## 2. Objetivo del Proyecto

El objetivo funcional de la web es:

- presentar la marca Linea Cano de forma aspiracional y coherente
- explicar el origen del proyecto desde la dehesa y sus líneas ganaderas
- mostrar el producto premium con un lenguaje más comercial
- permitir el acceso a usuarios registrados
- habilitar consulta de disponibilidad y reserva
- preparar una capa profesional para clientes HORECA
- preparar una capa comercial y de administración para futuras gestiones internas

## 3. Líneas de Negocio Actuales

En el estado actual del proyecto, las líneas principales son las siguientes.

### 3.1. Ganaderías

Actualmente se han definido **tres líneas ganaderas**:

- **Toro bravo**
  Función: construir una experiencia de visita ligada al campo bravo, la selección y el valor simbólico de la finca.

- **Ibérico**
  Función: articular la experiencia de montanera y conectar el relato ganadero con el producto ibérico.

- **Buey**
  Función: sostener una línea de visita especializada y dar base al discurso de maduración larga y piezas de alto valor.

La línea de **limusín extensivo** ha sido retirada del frontal de ganaderías como línea principal.

### 3.2. Producto premium

El área de producto se concentra ahora en el frontal de carne y producto premium, con estas familias:

- **Línea ibérica**
  Función: venta y presentación de jamones, lomos, embutidos y cortes vinculados a montanera y dehesa.

- **Línea buey madurado**
  Función: presentación de chuleteros, cortes nobles y piezas de larga maduración como producto gastronómico premium.

- **Despensa fresca**
  Función: reforzar la propuesta de territorio, temporada y km 0 mediante vegetales frescos y producto complementario.

### 3.3. Hospitality y reservas

La capa de hospitality está orientada a:

- hotel boutique
- estancia de marca
- wellness 365
- paquetes combinados con visitas

La función actual es permitir una reserva real de habitaciones y dejar preparada la evolución hacia un motor más amplio.

### 3.4. Eventos

La vertical de eventos está organizada con prioridad actual en:

- **eventos corporativos y MICE**
- **bodas**

La función de esta página es servir como base comercial para:

- eventos de empresa
- reuniones y congresos
- retiros
- bodas de alto nivel
- solicitud de dossier
- presupuesto futuro

### 3.5. HORECA

La línea HORECA se plantea como una capa más especializada y profesional.

Su función es:

- canalizar clientes profesionales
- preparar el acceso privado
- segmentar restauración y hotelería
- dejar lista la evolución hacia tarifas, catálogo profesional y relación comercial específica

## 4. Arquitectura de Frontales

Actualmente el sistema se organiza en varios frontales HTML.

### 4.1. `index.html`

**Función:** Home pública principal.

Contiene:

- presentación de marca
- propuesta de valor
- relato de territorio
- entrada por perfiles
- arquitectura del sitio
- sostenibilidad
- contacto

### 4.2. `ganaderias.html`

**Función:** Página de segundo nivel para las líneas ganaderas.

Contiene:

- introducción a las ganaderías
- bloque de marca
- catálogo visual de tres líneas:
  - visita brava
  - ruta de la montanera
  - santuario del buey

### 4.3. `reservas.html`

**Función:** Página de información del motor de reservas.

Contiene:

- hotel boutique
- visitas ganaderas
- paquetes combinados
- acceso al entorno privado donde se realiza la reserva funcional

### 4.4. `horeca.html`

**Función:** Página de producto premium y capa profesional.

Contiene:

- producto premium como primera lectura
- línea ibérica
- línea buey madurado
- despensa fresca
- bloque de marca
- área HORECA como segunda lectura

### 4.5. `eventos.html`

**Función:** Página vertical de eventos.

Contiene:

- prioridad de eventos corporativos y MICE
- bodas como segunda línea
- propuesta del centro de eventos
- dossier
- presupuesto

### 4.6. `acceso.html`

**Función:** Portal de acceso y registro.

Contiene:

- inicio de sesión
- registro de nuevo usuario
- mensajes de estado
- redirección por rol

### 4.7. `cliente.html`

**Función:** Panel privado del usuario registrado.

Contiene:

- estado de sesión
- bloque de experiencia
- consulta de disponibilidad
- confirmación de reserva

### 4.8. `comercial.html`

**Función:** Panel privado de administración / comercial.

Contiene:

- consulta de disponibilidad
- creación de reservas
- alta manual de clientes HORECA
- base para futuras pantallas de gestión

## 5. Roles de Usuario

Actualmente el sistema contempla estos roles funcionales:

- **público**
  Puede navegar por la web pública sin iniciar sesión.

- **registrado**
  Puede iniciar sesión, consultar disponibilidad y realizar reservas.

- **horeca**
  Se crea desde administración. Actualmente reutiliza el acceso privado del cliente, aunque el proyecto queda preparado para una vista específica futura.

- **maestro**
  Perfil administrativo/comercial. Puede consultar disponibilidad, registrar reservas y dar de alta clientes HORECA.

## 6. Funcionalidades Actuales

### 6.1. Funcionalidades públicas

- navegación entre frontales principales
- exposición de marca
- presentación de producto y servicios
- acceso al portal de login/registro

### 6.2. Registro de usuarios

Existe registro funcional de nuevos usuarios desde `acceso.html`.

Datos actuales del formulario:

- DNI o documento
- nombre
- primer apellido
- correo
- teléfono
- contraseña

Resultado:

- se crea el cliente
- se crea su usuario de acceso
- se asigna el rol `registrado`

### 6.3. Inicio de sesión

El login funciona contra base de datos.

Resultado del login:

- validación de credenciales
- creación de sesión
- devolución de token
- redirección al frontal correspondiente según rol

### 6.4. Consulta de disponibilidad

La consulta de disponibilidad es funcional desde los paneles privados.

Permite:

- indicar fecha de entrada
- indicar fecha de salida
- indicar número de huéspedes
- consultar habitaciones libres sobre MySQL

### 6.5. Creación de reserva

La reserva es funcional en entorno privado.

Permite:

- validar disponibilidad
- confirmar reserva
- registrar datos en `reserva`
- registrar datos en `reserva_habitacion`

### 6.6. Alta HORECA

Existe alta manual de cliente HORECA desde el panel `comercial.html`.

Datos actuales:

- empresa
- CIF
- nombre del contacto
- apellido
- DNI del contacto
- correo profesional
- teléfono
- contraseña inicial

Resultado:

- se crea el cliente base
- se crea el usuario de acceso
- se asigna rol `horeca`
- se crea la ficha `cliente_horeca`

## 7. Arquitectura Técnica

### 7.1. Frontend

Tecnologías actuales:

- HTML
- CSS
- JavaScript

Ficheros principales:

- `index.html`
- `ganaderias.html`
- `reservas.html`
- `horeca.html`
- `eventos.html`
- `acceso.html`
- `cliente.html`
- `comercial.html`
- `style.css`
- `script.js`
- `acceso.js`
- `portal-privado.js`

### 7.2. Backend

Tecnología actual:

- Java con servidor HTTP ligero

Paquete principal:

- `src/com/lineacano/servidor`

Clases destacadas:

- `Aplicacion.java`
- `ServicioAutenticacion.java`
- `ServicioReservas.java`
- `BaseDeDatos.java`
- `Configuracion.java`

### 7.3. Base de datos

Base de datos actual en MySQL.

Tablas relevantes:

- `cliente`
- `usuario_acceso`
- `cliente_horeca`
- `habitacion`
- `reserva`
- `reserva_habitacion`
- `salon`
- `reserva_salon`

### 7.4. Endpoints activos

- `GET /api/salud`
- `POST /api/sesion/iniciar`
- `POST /api/usuarios/registro`
- `POST /api/admin/clientes-horeca`
- `GET /api/disponibilidad`
- `POST /api/reservas`

## 8. Reglas de Acceso y Validación

Actualmente se aplican estas reglas:

- el acceso privado requiere autenticación
- la disponibilidad requiere sesión válida
- la reserva requiere sesión válida
- el alta HORECA requiere rol `maestro`
- la contraseña se limita a un máximo de 12 caracteres alfanuméricos

## 9. Material Visual Actual

El proyecto ya utiliza imágenes locales propias en varias secciones.

Material integrado:

- `imagenes/Dehesa.png`
- `imagenes/Toro.png`
- `imagenes/Iberico.png`
- `imagenes/buey.png`
- `imagenes/Fresco.png`
- `imagenes/carnes.png`
- `Logo_LineaCano.jpg`

## 10. Estado Actual del Proyecto

En este momento el proyecto ya dispone de:

- identidad visual funcional
- estructura web por verticales
- acceso y registro reales
- roles funcionales
- reserva funcional sobre base de datos
- alta HORECA funcional desde administración
- contenido alineado con el TFM

## 11. Pendientes Naturales de Evolución

Quedan como posibles siguientes iteraciones:

- vista privada específica para HORECA
- catálogo profesional con fichas de producto reales
- gestión de pedidos
- calendario de eventos y salones
- formularios comerciales conectados
- gestión avanzada de usuarios
- paneles internos más completos
- bilingüismo español/inglés
- integración de WhatsApp y mapas reales

## 12. Conclusión Funcional

Linea Cano ya no se comporta como una única landing, sino como un ecosistema digital estructurado por verticales. En su estado actual, combina:

- marca
- territorio
- ganaderías
- producto premium
- reservas
- eventos
- acceso por roles

Esto permite presentar una base sólida y coherente para el TFM, tanto desde el punto de vista visual como funcional.
