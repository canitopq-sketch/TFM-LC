# Plan de pruebas de LineaCano

## 1. Objetivo y alcance

Este documento define un conjunto de pruebas sencillo y reproducible para comprobar las funciones críticas de LineaCano. Incluye pruebas unitarias JUnit, pruebas de integración con MySQL y pruebas funcionales realizadas desde la interfaz web.

El plan cubre autenticación, sesiones, permisos, registro de usuarios, altas HORECA, reservas y pedidos. No pretende automatizar toda la aplicación: las reglas de negocio aislables se comprueban con JUnit y los recorridos completos que dependen del navegador y de MySQL se mantienen como pruebas manuales.

## 2. Entorno y datos de prueba

- Java y Maven con las versiones indicadas en `README.md`.
- MySQL en `localhost:3306`, base de datos `linea_cano`.
- Aplicación disponible en `http://localhost:8080`.
- Datos iniciales cargados desde el volcado SQL del proyecto.
- Cliente: `cliente@lineacano.com` / `cliente123`.
- HORECA: `horeca@lineacano.com` / `horeca123`.
- Maestro: `comercial@lineacano.com` / `master123`.
- Los registros temporales deben incluir palabras como `PRUEBA` o `ELIMINAR` para poder identificarlos.

## 3. Pruebas unitarias JUnit

Estas pruebas no conectan con MySQL. Los servicios reciben DAO de prueba internos que guardan datos en memoria y permiten comprobar tanto el resultado como si se solicitó una operación de persistencia.

| ID | Funcionalidad | Clase y método | Precondiciones | Datos de entrada | Acción | Resultado esperado | Evidencia | Estado |
|---|---|---|---|---|---|---|---|---|
| UT-01 | Cifrado de contraseñas | `ContrasenasTest.generaSha256Determinista` | Ninguna | La misma contraseña dos veces | Generar sus resúmenes SHA-256 | Ambos resultados son iguales y tienen formato válido | Informe Surefire | Superada |
| UT-02 | Fechas de reserva | `ReglasNegocioTest.aceptaPeriodoDeReservaValido` | Ninguna | Entrada anterior a salida | Validar el periodo | El periodo se acepta | Informe Surefire | Superada |
| UT-03 | Fechas de reserva | `ReglasNegocioTest.rechazaSalidaIgualOAnteriorALaEntrada` | Ninguna | Salida igual o anterior a entrada | Validar el periodo | Se rechazan ambos casos | Informe Surefire | Superada |
| UT-04 | Fechas de reserva | `ReglasNegocioTest.rechazaFechasAusentes` | Ninguna | Una fecha ausente | Validar el periodo | Se rechaza la petición | Informe Surefire | Superada |
| UT-05 | Número de huéspedes | `ReglasNegocioTest.validaLimitesDeHuespedes` | Ninguna | Valores dentro y fuera del límite | Validar huéspedes | Se aceptan los valores válidos y se rechazan los demás | Informe Surefire | Superada |
| UT-06 | Política de contraseña | `ReglasNegocioTest.validaPoliticaDeContrasenas` | Ninguna | Contraseñas válidas e inválidas | Validar contraseña | Solo se acepta la que cumple la regla | Informe Surefire | Superada |
| UT-07 | JSON | `UtilJsonTest.escapaCaracteresEspeciales` | Ninguna | Texto con caracteres especiales | Escapar el texto | Se obtiene JSON válido | Informe Surefire | Superada |
| UT-08 | JSON | `UtilJsonTest.analizaObjetoJsonPlano` | Ninguna | Objeto JSON sencillo | Analizar sus campos | Los valores se recuperan correctamente | Informe Surefire | Superada |
| UT-09 | Creación de reserva | `ServicioReservasTest.creaReservaUsandoHabitacionDevueltaPorDao` | DAO de prueba con habitación libre | Fechas, huéspedes y cliente válidos | Crear la reserva | Se usa la habitación ofrecida por el DAO y se solicita guardar la reserva | Informe Surefire | Superada |
| UT-10 | Falta de disponibilidad | `ServicioReservasTest.rechazaReservaCuandoDaoNoEncuentraHabitaciones` | DAO de prueba sin habitaciones | Reserva válida | Crear la reserva | Se informa de que no hay disponibilidad | Informe Surefire | Superada |
| UT-11 | Creación de pedido | `ServicioPedidosTest.calculaTotalYEnviaLineasAlDao` | DAO de prueba | Productos y cantidades válidos | Crear el pedido | Se calculan total y líneas y se envían al DAO | Informe Surefire | Superada |
| UT-12 | Pedido sin productos | `ServicioPedidosTest.rechazaPedidoSinProductosValidos` | DAO de prueba | Producto desconocido | Crear el pedido | El pedido se rechaza | Informe Surefire | Superada |
| UT-13 | Inicio de sesión | `ServicioAutenticacionTest.iniciaSesionYOrdenaPersistirlaAlDao` | Usuario activo en DAO de prueba | Correo y contraseña correctos | Iniciar sesión | Se crea una sesión y se solicita guardarla | Informe Surefire | Superada |
| UT-14 | Alta HORECA | `ServicioAutenticacionTest.daDeAltaHorecaConDatosNormalizados` | Correo y documento libres | Datos válidos de empresa | Dar de alta el usuario | Los datos normalizados se envían al DAO | Informe Surefire | Superada |
| UT-15 | Cierre de sesión | `ServicioAutenticacionTest.cerrarSesionEliminaElTokenMedianteElDao` | Token de prueba | `token-prueba` | Cerrar sesión | Se solicita al DAO eliminar el token | Informe Surefire | Superada |
| UT-16 | Credenciales e inactividad | `ServicioAutenticacionTest.rechazaContrasenaIncorrectaOUsuarioInactivo` | Usuario disponible en DAO de prueba | Contraseña incorrecta; usuario inactivo con contraseña correcta | Iniciar sesión en ambos escenarios | Ambos accesos se rechazan y no se registra una sesión | Informe Surefire | Superada |
| UT-17 | Validez de sesión | `ServicioAutenticacionTest.rechazaSesionInexistenteOCaducada` | DAO sin sesión y después con sesión de ocho días | Token inexistente; token caducado | Buscar ambos tokens | No se devuelve sesión y el token caducado se elimina | Informe Surefire | Superada |
| UT-18 | Registro duplicado | `ServicioAutenticacionTest.rechazaRegistroConCorreoODocumentoDuplicado` | DAO configurado con duplicados | Correo repetido; documento repetido | Registrar cliente | Ambos registros se rechazan antes de guardar datos | Informe Surefire | Superada |
| UT-19 | Cancelación inválida | `ServicioReservasTest.rechazaCancelacionPasadaOYaCancelada` | Reserva pasada y reserva futura cancelada en DAO de prueba | Identificador y cliente | Cancelar cada reserva | Ambas operaciones se rechazan y el DAO no cambia su estado | Informe Surefire | Superada |
| UT-20 | Cantidad de pedido | `ServicioPedidosTest.rechazaPedidoConCantidadInvalida` | DAO de prueba | Cantidad cero, negativa y no numérica | Crear los pedidos | Los tres pedidos se rechazan y no se guardan | Informe Surefire | Superada |

## 4. Pruebas de integración con MySQL

Estas pruebas comprueban conjuntamente servicios, DAO reales, JDBC y MySQL. Fueron realizadas durante el cierre técnico y no se han repetido al añadir las nuevas pruebas JUnit.

| ID | Funcionalidad | Tipo | Precondiciones | Datos de entrada | Pasos o acción | Resultado esperado | Automatizada/manual | Evidencia disponible | Estado |
|---|---|---|---|---|---|---|---|---|---|
| IT-01 | Importación inicial | Integración | MySQL iniciado y configuración local creada | Volcado SQL del proyecto | Ejecutar el script de importación y consultar tablas principales | Esquema y datos de demostración disponibles | Manual | Salida de MySQL y recuentos de tablas | Superada |
| IT-02 | Persistencia y cierre de sesión | Integración | Usuario de demostración activo | Credenciales de cliente | Iniciar y cerrar sesión; consultar `sesion_acceso` | El token aparece al entrar y desaparece al salir | Manual | Consultas MySQL antes y después | Superada |
| IT-03 | Ciclo de reserva | Integración | Cliente autenticado y habitación disponible | Reserva marcada como prueba | Crear, consultar histórico y cancelar | La reserva se guarda, aparece en el histórico y queda cancelada | Manual | Interfaz, respuestas del servidor y consultas MySQL | Superada |
| IT-04 | Ciclo de pedido | Integración | Cliente autenticado | Pedido marcado como prueba | Crear pedido y consultar histórico | Pedido y líneas quedan guardados y visibles | Manual | Interfaz y consultas MySQL | Superada |
| IT-05 | Alta de HORECA | Integración | Maestro autenticado | Correo, documento y empresa de prueba | Crear HORECA e iniciar sesión con la nueva cuenta | Usuario, cliente HORECA y acceso quedan relacionados correctamente | Manual | Interfaz y consultas MySQL | Superada |
| IT-06 | Limpieza | Integración | IT-03, IT-04 e IT-05 completadas | Identificadores temporales | Eliminar en orden los datos de prueba y consultar recuentos | No queda ningún registro ni sesión temporal | Manual | Consultas con resultado `0` y sesiones vacías | Superada |
| IT-07 | Reserva vinculada al perfil maestro | Integración | Aplicación iniciada y cuenta comercial asociada a `MAESTRODEMO01` | `comercial@lineacano.com`, reserva temporal para 2 huéspedes | Iniciar sesión como maestro, consultar disponibilidad, crear la reserva, comprobar su persistencia, limpiar la reserva y cerrar sesión | La reserva queda vinculada a `MAESTRODEMO01`, se asocia a una habitación y tanto la reserva temporal como el token se eliminan al finalizar | Manual | Respuestas HTTP y consultas MySQL de asociación, persistencia y limpieza | Superada |

### 4.1. Registro de IT-07 — Reserva vinculada al perfil maestro

- **Datos utilizados:** cuenta `comercial@lineacano.com`, rol `maestro`, cliente asociado `MAESTRODEMO01` y reserva temporal número `21`.
- **Pasos realizados:** se inició sesión con la cuenta comercial, se consultó la disponibilidad para dos huéspedes, se confirmó una reserva desde el perfil maestro, se comprobó su persistencia en MySQL y después se eliminó la reserva temporal. Finalmente se cerró la sesión.
- **Resultado esperado:** acceso correcto con rol maestro, reserva creada con HTTP 201 y vinculada a `MAESTRODEMO01`, relación con una habitación, limpieza completa de la reserva y eliminación del token de `sesion_acceso`.
- **Resultado obtenido:** el inicio de sesión devolvió el rol `maestro` y el documento `MAESTRODEMO01`; la consulta de disponibilidad fue correcta; la reserva `21` se creó con HTTP 201, estado `Confirmada`, habitación `101` e importe de `300 €`. La reserva y su relación con la habitación se localizaron en MySQL. Después de la limpieza no quedó la reserva temporal y, tras cerrar sesión, no quedó el token utilizado en `sesion_acceso`.
- **Evidencia:** respuesta JSON del inicio de sesión y de la consulta de disponibilidad, respuesta HTTP 201 de creación, consultas sobre `reserva`, `reserva_habitacion` y `sesion_acceso`, y recuentos finales con resultado `0` para la reserva temporal y el token.
- **Estado final:** superada.

## 5. Pruebas funcionales de la aplicación

| ID | Funcionalidad | Tipo | Precondiciones | Datos de entrada | Pasos o acción | Resultado esperado | Automatizada/manual | Evidencia disponible | Estado |
|---|---|---|---|---|---|---|---|---|---|
| FT-01 | Navegación pública | Funcional | Servidor iniciado | Ninguno | Abrir las páginas públicas y recorrer su navegación | Las páginas cargan, mantienen su diseño y no muestran errores | Manual | Revisión visual en navegador | Superada |
| FT-02 | Acceso por perfiles | Funcional | Datos de demostración cargados | Cliente, HORECA y maestro | Iniciar sesión con cada cuenta | Cada usuario entra en su zona y ve las acciones de su perfil | Manual | Revisión en navegador | Superada |
| FT-03 | Disponibilidad | Funcional | Cliente autenticado | Fechas y huéspedes válidos | Consultar alojamiento | Se muestran las opciones disponibles o un mensaje claro | Manual | Interfaz y respuesta de la aplicación | Superada |
| FT-04 | Reservas | Funcional | Cliente autenticado | Datos de reserva válidos | Crear, abrir histórico y cancelar | El histórico refleja todo el recorrido | Manual | Interfaz y comprobación MySQL | Superada |
| FT-05 | Pedidos | Funcional | Cliente autenticado | Productos y cantidades válidos | Confirmar y abrir histórico | El pedido aparece con sus datos y total | Manual | Interfaz y comprobación MySQL | Superada |
| FT-06 | Cierre de sesión | Funcional | Sesión activa | Botón «Cerrar sesión» | Pulsar el botón y volver a una zona privada | Se vuelve al acceso y el token anterior ya no sirve | Manual | Navegador y consulta MySQL | Superada |
| FT-07 | Registro de cliente | Funcional | Correo y documento nuevos | Datos identificados como prueba | Completar el formulario e iniciar sesión | Se crea una cuenta utilizable | Manual | Mensaje de registro, acceso al panel y consultas MySQL de limpieza | Superada |

## 6. Permisos y casos negativos

| ID | Funcionalidad | Tipo | Precondiciones | Datos de entrada | Pasos o acción | Resultado esperado | Automatizada/manual | Evidencia disponible | Estado |
|---|---|---|---|---|---|---|---|---|---|
| PN-01 | Contraseña incorrecta | Negativa | Usuario activo | Contraseña errónea | Iniciar sesión | Acceso rechazado sin crear sesión | JUnit (UT-16) | Informe Surefire | Superada |
| PN-02 | Usuario inactivo | Negativa | Usuario inactivo | Contraseña correcta | Iniciar sesión | Acceso rechazado sin crear sesión | JUnit (UT-16) | Informe Surefire | Superada |
| PN-03 | Sesión inexistente o caducada | Negativa | Token ausente o de ocho días | Ambos tokens | Validar sesión | No se autoriza y el token caducado se elimina | JUnit (UT-17) | Informe Surefire | Superada |
| PN-04 | Correo o documento duplicado | Negativa | Dato ya existente | Dos intentos de registro | Registrar cliente | No se guarda ninguno | JUnit (UT-18) | Informe Surefire | Superada |
| PN-05 | Reserva pasada o cancelada | Negativa | Reservas con ambos estados | Identificador y cliente | Cancelar | No cambia la reserva | JUnit (UT-19) | Informe Surefire | Superada |
| PN-06 | Cantidad inválida | Negativa | Cliente válido | Cero, negativa y texto | Crear pedido | No se crea el pedido | JUnit (UT-20) | Informe Surefire | Superada |
| PN-07 | Sin habitaciones | Negativa | DAO sin disponibilidad | Reserva válida | Crear reserva | Se informa de falta de disponibilidad | JUnit (UT-10) | Informe Surefire | Superada |
| PN-08 | Endpoint privado sin token | Permisos | Sin sesión en el navegador | `POST /api/pedidos` sin `X-Linea-Token` | Enviar la petición | El servidor devuelve HTTP 401 | Manual | Código y cuerpo de la respuesta HTTP | Superada |
| PN-09 | Rol no autorizado | Permisos | Sesión válida de maestro | `POST /api/pedidos` con token de maestro | Enviar la petición | El servidor devuelve HTTP 403 | Manual | Código y cuerpo de la respuesta HTTP | Superada |

Los casos PN-08 y PN-09 se conservan como pruebas manuales porque dependen del recorrido HTTP completo. Automatizarlos exigiría infraestructura adicional o una reorganización del controlador que no aporta valor suficiente a este TFG.

### 6.1. Registro de las pruebas manuales finales

#### FT-07 — Registro funcional completo de cliente

- **Datos utilizados:** documento `12082606Q`, nombre `Prueba`, apellido `M06 Eliminar`, correo `prueba.m06.eliminar@lineacano.test`, teléfono `600060606` y contraseña temporal `Prueba06`.
- **Pasos realizados:** se completó el formulario público de alta, se pulsó «Crear cuenta» y después se inició sesión con el correo y la contraseña creados.
- **Resultado esperado:** alta aceptada y acceso al área privada con rol de cliente registrado.
- **Resultado obtenido:** la web mostró «Registro completado» y redirigió correctamente a `cliente.html`, donde apareció el nombre `Prueba M06 Eliminar` y el estado «Acceso autorizado».
- **Evidencia:** mensajes visibles de registro y sesión, redirección al panel del cliente y eliminación posterior comprobada en MySQL.
- **Estado final:** superada.

#### PN-08 — Acceso a endpoint privado sin token

- **Datos utilizados:** petición `POST /api/pedidos` sin la cabecera `X-Linea-Token`.
- **Pasos realizados:** con el navegador sin sesión activa, se envió la petición al controlador de pedidos.
- **Resultado esperado:** código HTTP 401 y rechazo de la operación.
- **Resultado obtenido:** HTTP 401 con el cuerpo `{"error":"Debes iniciar sesion para crear un pedido."}`.
- **Evidencia:** código de estado y cuerpo de la respuesta HTTP observados durante la prueba.
- **Estado final:** superada.

#### PN-09 — Operación con rol no autorizado

- **Datos utilizados:** cuenta de demostración `comercial@lineacano.com`, rol `maestro`, y petición `POST /api/pedidos` con su token de sesión válido.
- **Pasos realizados:** se inició sesión como maestro y se intentó crear un pedido, operación permitida únicamente a los roles `registrado` y `horeca`.
- **Resultado esperado:** código HTTP 403 sin creación del pedido.
- **Resultado obtenido:** HTTP 403 con el cuerpo `{"error":"Tu rol no tiene permiso para crear pedidos."}`.
- **Evidencia:** código de estado y cuerpo de la respuesta HTTP observados durante la prueba. La sesión del maestro se cerró al terminar.
- **Estado final:** superada.

## 7. Limpieza de datos

Las pruebas JUnit no generan datos reales. En las pruebas manuales debe anotarse cada identificador creado y eliminarse respetando las relaciones entre tablas. Después se comprueba con consultas `COUNT(*)` que quedan cero reservas, pedidos, usuarios, clientes HORECA y sesiones asociados a los datos temporales.

En las pruebas de integración se eliminaron la reserva, el pedido, el usuario HORECA, el cliente asociado y sus sesiones temporales. Todos los recuentos finales fueron cero. En IT-07 también se eliminó la reserva temporal `21` y se comprobó que el token utilizado por el maestro ya no existía en `sesion_acceso`. El cliente de demostración `MAESTRODEMO01` se conserva porque forma parte de los datos permanentes de la aplicación.

Tras FT-07 también se eliminaron el usuario y el cliente temporal `prueba.m06.eliminar@lineacano.test`. Las consultas finales sobre `usuario_acceso`, `cliente` y `sesion_acceso` devolvieron `restantes = 0` en los tres casos.

## 8. Ejecución y evidencias

1. Ejecutar las pruebas unitarias desde la raíz:

   ```bash
   mvn clean test
   ```

2. Conservar como evidencia el resumen de Maven y los informes de `target/surefire-reports`. El resultado esperado es: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`.
3. Para pruebas manuales, iniciar MySQL y LineaCano siguiendo `README.md`.
4. Realizar cada caso con datos identificados como prueba y anotar fecha, resultado y cualquier incidencia.
5. Guardar capturas de las pantallas relevantes y de las consultas MySQL de comprobación.
6. Eliminar los datos temporales y guardar los recuentos finales como evidencia de limpieza.

## 9. Criterio de cierre

La versión se considera preparada para la entrega cuando las 20 pruebas JUnit terminan sin fallos, las funciones principales superan la revisión manual contra MySQL, los casos de permisos se ejecutan y no quedan datos temporales. Cualquier fallo debe anotarse, corregirse y volver a probarse antes de grabar la demostración final.
