const botonCerrarSesion = document.querySelector("#boton-cerrar-sesion");
const formularioReserva = document.querySelector("#formulario-reserva");
const campoFechaEntrada = document.querySelector("#fecha-entrada");
const campoFechaSalida = document.querySelector("#fecha-salida");
const campoHuespedes = document.querySelector("#huespedes");
const tarjetaDisponibilidad = document.querySelector("#tarjeta-disponibilidad");
const botonConfirmarReserva = document.querySelector("#boton-confirmar-reserva");
const resumenReserva = document.querySelector("#resumen-reserva");
const estadoSesion = document.querySelector("#estado-sesion");
const tituloPrivado = document.querySelector("#titulo-pagina-privada");
const nombrePrivado = document.querySelector("#nombre-usuario-privado");
const textoPrivado = document.querySelector("#texto-usuario-privado");
const formularioHoreca = document.querySelector("#formulario-horeca");
const estadoHoreca = document.querySelector("#estado-horeca");
const botonActualizarReservas = document.querySelector("#boton-actualizar-reservas");
const estadoReservas = document.querySelector("#estado-reservas");
const historialReservas = document.querySelector("#historial-reservas");
const botonesAgregarProducto = document.querySelectorAll("[data-action='agregar-producto']");
const listaCarrito = document.querySelector("#lista-carrito");
const totalCarrito = document.querySelector("#total-carrito");
const estadoCarrito = document.querySelector("#estado-carrito");
const botonConfirmarCompra = document.querySelector("#boton-confirmar-compra");
const botonVaciarCarrito = document.querySelector("#boton-vaciar-carrito");
const historialCompras = document.querySelector("#historial-compras");
const enlacesNavegacionPrivada = document.querySelectorAll(".site-nav a[href^='#']");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";
const CATALOGO_PRODUCTOS = {
    iberico: {
        nombre: "Seleccion iberica de montanera",
        categoria: "Iberico",
        precio: 89
    },
    buey: {
        nombre: "Corte premium madurado",
        categoria: "Buey",
        precio: 126
    },
    despensa: {
        nombre: "Cesta fresca de temporada",
        categoria: "Despensa",
        precio: 42
    }
};
let sesionActiva = cargarSesion();
let ultimaDisponibilidad = null;

inicializarPortalPrivado();

formularioReserva.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    if (!fechasSonValidas()) {
        return;
    }

    try {
        const parametros = new URLSearchParams({
            fechaEntrada: campoFechaEntrada.value,
            fechaSalida: campoFechaSalida.value,
            huespedes: campoHuespedes.value
        });

        const respuesta = await fetch(
            `/api/disponibilidad?${parametros.toString()}`,
            {
                headers: {
                    "Accept": "application/json",
                    "X-Linea-Token": sesionActiva.token
                }
            }
        );

        if (respuesta.status === 401) {
            cerrarSesionPorExpiracion();
            return;
        }

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo consultar la disponibilidad real.");
        }

        ultimaDisponibilidad = datos;
        renderizarDisponibilidad(datos.titulo, datos.mensaje, datos.estado);
        actualizarResumenReserva(datos);
        actualizarBotonReserva();
    } catch (error) {
        ultimaDisponibilidad = null;
        actualizarBotonReserva();
        renderizarDisponibilidad("Error de consulta", error.message || "No se pudo obtener disponibilidad.", "aviso");
    }
});

botonConfirmarReserva.addEventListener("click", async () => {
    if (!ultimaDisponibilidad || ultimaDisponibilidad.habitacionesDisponibles < 1) {
        renderizarDisponibilidad(
            "Reserva no disponible",
            "Antes de confirmar, realiza una consulta con disponibilidad positiva.",
            "aviso"
        );
        return;
    }

    try {
        botonConfirmarReserva.disabled = true;

        const respuesta = await fetch("/api/reservas", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
                "X-Linea-Token": sesionActiva.token
            },
            body: JSON.stringify({
                fechaEntrada: campoFechaEntrada.value,
                fechaSalida: campoFechaSalida.value,
                huespedes: Number(campoHuespedes.value)
            })
        });

        if (respuesta.status === 401) {
            cerrarSesionPorExpiracion();
            return;
        }

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo crear la reserva.");
        }

        renderizarDisponibilidad(
            datos.titulo,
            `${datos.mensaje} Estancia del ${formatearFecha(datos.fechaEntrada)} al ${formatearFecha(datos.fechaSalida)}. Habitacion ${datos.habitacionAsignada}. Reserva ${datos.idReserva}. Importe estimado ${formatearImporte(datos.importeTotal)}.`,
            "disponible"
        );

        actualizarResumenReserva({
            habitacionesDisponibles: Math.max((ultimaDisponibilidad?.habitacionesDisponibles || 1) - 1, 0),
            habitacionesReservadas: (ultimaDisponibilidad?.habitacionesReservadas || 0) + 1,
            totalHabitaciones: ultimaDisponibilidad?.totalHabitaciones || 0
        });

        ultimaDisponibilidad = null;
        actualizarBotonReserva();
        cargarMisReservas();
    } catch (error) {
        renderizarDisponibilidad("No se pudo confirmar", error.message || "La reserva no pudo registrarse.", "aviso");
    } finally {
        botonConfirmarReserva.disabled = false;
    }
});

botonCerrarSesion.addEventListener("click", () => {
    localStorage.removeItem(CLAVE_ALMACENAMIENTO);
    window.location.href = "acceso.html";
});

if (formularioHoreca) {
    formularioHoreca.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        const datosFormulario = {
            empresa: document.querySelector("#horeca-empresa").value.trim(),
            cif: document.querySelector("#horeca-cif").value.trim(),
            nombreContacto: document.querySelector("#horeca-nombre").value.trim(),
            apellidoContacto: document.querySelector("#horeca-apellido").value.trim(),
            documento: document.querySelector("#horeca-documento").value.trim(),
            correo: document.querySelector("#horeca-correo").value.trim(),
            telefono: document.querySelector("#horeca-telefono").value.trim(),
            contrasena: document.querySelector("#horeca-contrasena").value.trim()
        };

        if (!/^[A-Za-z0-9]{1,12}$/.test(datosFormulario.contrasena)) {
            renderizarEstadoHoreca(
                "aviso",
                "Contrasena no valida",
                "La contrasena inicial debe tener un maximo de 12 caracteres alfanumericos."
            );
            document.querySelector("#horeca-contrasena").focus();
            return;
        }

        try {
            const respuesta = await fetch("/api/admin/clientes-horeca", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "X-Linea-Token": sesionActiva.token
                },
                body: JSON.stringify(datosFormulario)
            });

            if (respuesta.status === 401) {
                cerrarSesionPorExpiracion();
                return;
            }

            const datos = await leerRespuestaJson(respuesta);

            if (!respuesta.ok) {
                throw new Error(datos.error || "No se pudo crear el cliente HORECA.");
            }

            renderizarEstadoHoreca(
                "disponible",
                datos.titulo || "Alta realizada",
                `${datos.mensaje} Correo de acceso: ${datos.correo}.`
            );
            formularioHoreca.reset();
        } catch (error) {
            renderizarEstadoHoreca(
                "aviso",
                "Alta no completada",
                error.message || "No se ha podido registrar el cliente HORECA."
            );
        }
    });
}

if (botonActualizarReservas) {
    botonActualizarReservas.addEventListener("click", () => {
        cargarMisReservas();
    });
}

botonesAgregarProducto.forEach((boton) => {
    boton.addEventListener("click", () => {
        agregarProductoAlCarrito(boton.dataset.producto);
    });
});

if (botonConfirmarCompra) {
    botonConfirmarCompra.addEventListener("click", confirmarCompra);
}

if (botonVaciarCarrito) {
    botonVaciarCarrito.addEventListener("click", () => {
        guardarCarrito([]);
        renderizarCarrito();
        renderizarEstadoCarrito("bloqueado", "Carrito vacio", "El pedido actual se ha vaciado correctamente.");
    });
}

enlacesNavegacionPrivada.forEach((enlace) => {
    enlace.addEventListener("click", () => {
        marcarSeccionActiva(enlace.getAttribute("href"));
    });
});

window.addEventListener("hashchange", () => {
    marcarSeccionActiva(window.location.hash || "#panel");
});

function cargarSesion() {
    try {
        const valorGuardado = localStorage.getItem(CLAVE_ALMACENAMIENTO);
        return valorGuardado ? JSON.parse(valorGuardado) : null;
    } catch {
        return null;
    }
}

async function inicializarPortalPrivado() {
    if (!sesionActiva?.token) {
        window.location.href = "acceso.html";
        return;
    }

    const sesionValidada = await validarSesionEnServidor(sesionActiva.token);
    if (!sesionValidada) {
        cerrarSesionPorExpiracion();
        return;
    }

    sesionActiva = sesionValidada;
    localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(sesionActiva));
    protegerPagina();
    marcarSeccionActiva(window.location.hash || "#panel");
    configurarFechas();
    configurarHistoricoReservas();
    configurarCarrito();
}

function protegerPagina() {
    const tipoPagina = document.body.dataset.paginaPrivada;

    if (!sesionActiva) {
        window.location.href = "acceso.html";
        return;
    }

    if (tipoPagina === "cliente" && sesionActiva.rol !== "registrado" && sesionActiva.rol !== "horeca") {
        redirigirSegunPerfil(sesionActiva.rol);
        return;
    }

    if (tipoPagina === "comercial" && sesionActiva.rol !== "maestro") {
        redirigirSegunPerfil(sesionActiva.rol);
        return;
    }

    nombrePrivado.textContent = sesionActiva.nombreVisible;

    if (sesionActiva.rol === "maestro") {
        tituloPrivado.textContent = "Frontal comercial con disponibilidad real, reserva y base para gestion futura.";
        textoPrivado.textContent = "Desde este entorno comercial puedes validar ocupacion, confirmar reservas y preparar la siguiente capa de operativa.";
        estadoSesion.dataset.state = "maestro";
    } else if (sesionActiva.rol === "horeca") {
        tituloPrivado.textContent = "Frontal profesional HORECA con acceso inicial a disponibilidad y futura capa comercial.";
        textoPrivado.textContent = "Este acceso profesional queda preparado para tarifas, catalogo premium, seguimiento de pedidos y operativa comercial.";
        estadoSesion.dataset.state = "horeca";
    } else {
        tituloPrivado.textContent = "Panel privado para reservar estancia, comprar producto y revisar tu actividad.";
        textoPrivado.textContent = "Desde este entorno puedes confirmar fechas, preparar pedidos de producto y consultar tu historial operativo.";
        estadoSesion.dataset.state = "registrado";
    }
}

function redirigirSegunPerfil(rol) {
    if (rol === "maestro") {
        window.location.href = "comercial.html";
        return;
    }

    if (rol === "registrado" || rol === "horeca") {
        window.location.href = "cliente.html";
        return;
    }

    window.location.href = "acceso.html";
}

function marcarSeccionActiva(hashActivo) {
    if (!enlacesNavegacionPrivada.length) {
        return;
    }

    enlacesNavegacionPrivada.forEach((enlace) => {
        const estaActivo = enlace.getAttribute("href") === hashActivo;
        if (estaActivo) {
            enlace.setAttribute("aria-current", "true");
        } else {
            enlace.removeAttribute("aria-current");
        }
    });
}

function configurarHistoricoReservas() {
    if (!historialReservas || document.body.dataset.paginaPrivada !== "cliente") {
        return;
    }

    cargarMisReservas();
}

async function cargarMisReservas() {
    if (!sesionActiva?.token || !historialReservas || !estadoReservas) {
        return;
    }

    renderizarEstadoReservas("bloqueado", "Cargando reservas", "Consultando el historico real asociado al cliente.");

    try {
        const respuesta = await fetch("/api/reservas/mis-reservas", {
            headers: {
                "Accept": "application/json",
                "X-Linea-Token": sesionActiva.token
            }
        });

        if (respuesta.status === 401) {
            cerrarSesionPorExpiracion();
            return;
        }

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo cargar el historico de reservas.");
        }

        renderizarListaReservas(datos.reservas || []);
        renderizarEstadoReservas(
            "disponible",
            "Historico cargado",
            `Se han localizado ${datos.total || 0} reservas vinculadas a tu usuario.`
        );
    } catch (error) {
        renderizarEstadoReservas(
            "aviso",
            "No se pudo cargar el historico",
            error.message || "Intentalo de nuevo en unos segundos."
        );
        historialReservas.innerHTML = "";
    }
}

function renderizarListaReservas(reservas) {
    if (!historialReservas) {
        return;
    }

    if (!Array.isArray(reservas) || reservas.length === 0) {
        historialReservas.innerHTML = `
            <article class="history-card history-card-empty">
                <strong>No hay reservas registradas</strong>
                <p>Todavia no existen estancias vinculadas a este usuario.</p>
            </article>
        `;
        return;
    }

    historialReservas.innerHTML = reservas.map((reserva) => {
        const estadoNormalizado = String(reserva.estado || "").toLowerCase();
        const puedeCancelar = estadoNormalizado === "confirmada" && esReservaFutura(reserva.fechaEntrada);
        const botonCancelar = puedeCancelar
            ? `<button class="button button-secondary history-action" type="button" data-action="cancelar-reserva" data-id-reserva="${reserva.idReserva}">Cancelar reserva</button>`
            : "";

        return `
            <article class="history-card" data-reserva="${reserva.idReserva}">
                <p class="availability-label">Reserva ${reserva.idReserva}</p>
                <h3>${formatearFecha(reserva.fechaEntrada)} al ${formatearFecha(reserva.fechaSalida)}</h3>
                <p class="history-meta">Estado: ${reserva.estado} · Habitacion: ${reserva.habitacionAsignada ?? "Sin asignar"} · Importe: ${formatearImporte(reserva.importeTotal) || "Pendiente"}</p>
                ${botonCancelar}
            </article>
        `;
    }).join("");

    historialReservas.querySelectorAll("[data-action='cancelar-reserva']").forEach((boton) => {
        boton.addEventListener("click", () => cancelarReservaCliente(boton.dataset.idReserva));
    });
}

function configurarCarrito() {
    if (!listaCarrito || !totalCarrito || !historialCompras) {
        return;
    }

    renderizarCarrito();
    renderizarHistorialCompras();
}

function agregarProductoAlCarrito(idProducto) {
    const producto = CATALOGO_PRODUCTOS[idProducto];
    if (!producto) {
        return;
    }

    const carrito = cargarCarrito();
    const lineaExistente = carrito.find((linea) => linea.id === idProducto);

    if (lineaExistente) {
        lineaExistente.cantidad += 1;
    } else {
        carrito.push({
            id: idProducto,
            cantidad: 1
        });
    }

    guardarCarrito(carrito);
    renderizarCarrito();
    renderizarEstadoCarrito("disponible", "Producto anadido", `${producto.nombre} se ha incorporado al carrito.`);
}

function renderizarCarrito() {
    if (!listaCarrito || !totalCarrito) {
        return;
    }

    const carrito = cargarCarrito();

    if (carrito.length === 0) {
        listaCarrito.innerHTML = `
            <article class="cart-empty">
                <strong>Carrito vacio</strong>
                <p>Selecciona productos de la tienda privada.</p>
            </article>
        `;
        totalCarrito.textContent = formatearImporte(0);
        if (botonConfirmarCompra) {
            botonConfirmarCompra.disabled = true;
        }
        return;
    }

    listaCarrito.innerHTML = carrito.map((linea) => {
        const producto = CATALOGO_PRODUCTOS[linea.id];
        const subtotal = producto.precio * linea.cantidad;

        return `
            <article class="cart-line">
                <div>
                    <span>${producto.categoria}</span>
                    <strong>${producto.nombre}</strong>
                    <small>${formatearImporte(producto.precio)} unidad · ${formatearImporte(subtotal)}</small>
                </div>
                <div class="cart-controls">
                    <button type="button" data-action="restar-producto" data-producto="${linea.id}" aria-label="Restar ${producto.nombre}">-</button>
                    <output>${linea.cantidad}</output>
                    <button type="button" data-action="sumar-producto" data-producto="${linea.id}" aria-label="Sumar ${producto.nombre}">+</button>
                </div>
            </article>
        `;
    }).join("");

    totalCarrito.textContent = formatearImporte(calcularTotalCarrito(carrito));
    if (botonConfirmarCompra) {
        botonConfirmarCompra.disabled = false;
    }

    listaCarrito.querySelectorAll("[data-action='sumar-producto']").forEach((boton) => {
        boton.addEventListener("click", () => cambiarCantidadProducto(boton.dataset.producto, 1));
    });

    listaCarrito.querySelectorAll("[data-action='restar-producto']").forEach((boton) => {
        boton.addEventListener("click", () => cambiarCantidadProducto(boton.dataset.producto, -1));
    });
}

function cambiarCantidadProducto(idProducto, variacion) {
    const carritoActualizado = cargarCarrito()
        .map((linea) => linea.id === idProducto
            ? { ...linea, cantidad: linea.cantidad + variacion }
            : linea
        )
        .filter((linea) => linea.cantidad > 0);

    guardarCarrito(carritoActualizado);
    renderizarCarrito();
}

function confirmarCompra() {
    const carrito = cargarCarrito();

    if (carrito.length === 0) {
        renderizarEstadoCarrito("aviso", "Carrito vacio", "Anade al menos un producto antes de confirmar la compra.");
        return;
    }

    const compra = {
        id: `LC-${Date.now()}`,
        fecha: new Date().toISOString(),
        estado: "Solicitud recibida",
        total: calcularTotalCarrito(carrito),
        lineas: carrito.map((linea) => ({
            ...linea,
            nombre: CATALOGO_PRODUCTOS[linea.id].nombre,
            categoria: CATALOGO_PRODUCTOS[linea.id].categoria,
            precio: CATALOGO_PRODUCTOS[linea.id].precio
        }))
    };

    const historial = cargarHistorialCompras();
    historial.unshift(compra);
    guardarHistorialCompras(historial);
    guardarCarrito([]);
    renderizarCarrito();
    renderizarHistorialCompras();
    renderizarEstadoCarrito("disponible", "Compra registrada", `Pedido ${compra.id} guardado en tu historial de compras.`);
}

function renderizarHistorialCompras() {
    if (!historialCompras) {
        return;
    }

    const historial = cargarHistorialCompras();

    if (historial.length === 0) {
        historialCompras.innerHTML = `
            <article class="history-card history-card-empty">
                <strong>No hay compras registradas</strong>
                <p>Los pedidos confirmados desde el carrito apareceran en este historial.</p>
            </article>
        `;
        return;
    }

    historialCompras.innerHTML = historial.map((compra) => {
        const fechaCompra = formatearFecha(compra.fecha);
        const resumenLineas = compra.lineas
            .map((linea) => `${linea.cantidad} x ${linea.nombre}`)
            .join(" · ");

        return `
            <article class="history-card">
                <p class="availability-label">Pedido ${compra.id}</p>
                <h3>${fechaCompra}</h3>
                <p class="history-meta">Estado: ${compra.estado} · Total: ${formatearImporte(compra.total)}</p>
                <p class="history-meta">${resumenLineas}</p>
            </article>
        `;
    }).join("");
}

function cargarCarrito() {
    return cargarArrayLocal(claveCarrito());
}

function guardarCarrito(carrito) {
    localStorage.setItem(claveCarrito(), JSON.stringify(carrito));
}

function cargarHistorialCompras() {
    return cargarArrayLocal(claveHistorialCompras());
}

function guardarHistorialCompras(historial) {
    localStorage.setItem(claveHistorialCompras(), JSON.stringify(historial));
}

function cargarArrayLocal(clave) {
    try {
        const valorGuardado = localStorage.getItem(clave);
        const datos = valorGuardado ? JSON.parse(valorGuardado) : [];
        return Array.isArray(datos) ? datos : [];
    } catch {
        return [];
    }
}

function claveCarrito() {
    return `linea_cano_carrito_${sesionActiva?.usuario || "anonimo"}`;
}

function claveHistorialCompras() {
    return `linea_cano_compras_${sesionActiva?.usuario || "anonimo"}`;
}

function calcularTotalCarrito(carrito) {
    return carrito.reduce((total, linea) => {
        const producto = CATALOGO_PRODUCTOS[linea.id];
        return producto ? total + producto.precio * linea.cantidad : total;
    }, 0);
}

async function cancelarReservaCliente(idReserva) {
    if (!idReserva || !sesionActiva?.token) {
        return;
    }

    renderizarEstadoReservas("bloqueado", "Cancelando reserva", `Procesando la cancelacion de la reserva ${idReserva}.`);

    try {
        const respuesta = await fetch("/api/reservas/cancelar", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
                "X-Linea-Token": sesionActiva.token
            },
            body: JSON.stringify({
                idReserva: Number(idReserva)
            })
        });

        if (respuesta.status === 401) {
            cerrarSesionPorExpiracion();
            return;
        }

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo cancelar la reserva.");
        }

        renderizarEstadoReservas("disponible", datos.titulo || "Reserva cancelada", datos.mensaje);
        cargarMisReservas();
    } catch (error) {
        renderizarEstadoReservas(
            "aviso",
            "No se pudo cancelar",
            error.message || "La reserva no ha podido cancelarse."
        );
    }
}

async function validarSesionEnServidor(token) {
    try {
        const respuesta = await fetch("/api/sesion/validar", {
            headers: {
                "Accept": "application/json",
                "X-Linea-Token": token
            }
        });

        if (!respuesta.ok) {
            return null;
        }

        return await leerRespuestaJson(respuesta);
    } catch {
        return null;
    }
}

async function leerRespuestaJson(respuesta) {
    const tipoContenido = respuesta.headers.get("Content-Type") || "";
    const cuerpoTexto = await respuesta.text();

    if (!tipoContenido.includes("application/json")) {
        throw new Error("El servidor no ha devuelto una respuesta JSON valida. Recarga la web e intentalo de nuevo.");
    }

    try {
        return JSON.parse(cuerpoTexto);
    } catch {
        throw new Error("La respuesta del servidor no se ha podido interpretar correctamente.");
    }
}

function cerrarSesionPorExpiracion() {
    localStorage.removeItem(CLAVE_ALMACENAMIENTO);
    window.location.href = "acceso.html";
}

function configurarFechas() {
    const hoy = new Date();
    const fechaHoyIso = hoy.toISOString().split("T")[0];
    campoFechaEntrada.min = fechaHoyIso;
    campoFechaSalida.min = fechaHoyIso;

    campoFechaEntrada.addEventListener("change", () => {
        campoFechaSalida.min = campoFechaEntrada.value || fechaHoyIso;

        if (campoFechaSalida.value && campoFechaEntrada.value && campoFechaSalida.value <= campoFechaEntrada.value) {
            const diaSiguiente = new Date(campoFechaEntrada.value);
            diaSiguiente.setDate(diaSiguiente.getDate() + 1);
            campoFechaSalida.value = diaSiguiente.toISOString().split("T")[0];
        }
    });
}

function fechasSonValidas() {
    if (!campoFechaEntrada.value || !campoFechaSalida.value) {
        renderizarDisponibilidad("Faltan fechas", "Selecciona llegada y salida para poder consultar disponibilidad.", "aviso");
        return false;
    }

    const fechaEntrada = new Date(campoFechaEntrada.value);
    const fechaSalida = new Date(campoFechaSalida.value);
    const numeroNoches = Math.round((fechaSalida - fechaEntrada) / 86400000);

    if (numeroNoches <= 0) {
        renderizarDisponibilidad("Rango invalido", "La fecha de salida debe ser posterior a la fecha de llegada.", "aviso");
        return false;
    }

    return true;
}

function renderizarDisponibilidad(titulo, texto, tipo) {
    tarjetaDisponibilidad.dataset.state = tipo;
    tarjetaDisponibilidad.innerHTML = `
        <p class="availability-label">Estado actual</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
}

function actualizarResumenReserva(datos) {
    const tarjetasResumen = document.querySelectorAll(".management-preview article strong");

    if (tarjetasResumen.length < 3) {
        return;
    }

    tarjetasResumen[0].textContent = typeof datos.habitacionesDisponibles === "number"
        ? `${datos.habitacionesDisponibles} disponibles`
        : String(datos.habitacionesDisponibles);
    tarjetasResumen[1].textContent = typeof datos.habitacionesReservadas === "number"
        ? `${datos.habitacionesReservadas} ocupadas`
        : String(datos.habitacionesReservadas);
    tarjetasResumen[2].textContent = typeof datos.totalHabitaciones === "number"
        ? `${datos.totalHabitaciones} total inventario`
        : String(datos.totalHabitaciones);
}

function actualizarBotonReserva() {
    const sePuedeMostrar = Boolean(ultimaDisponibilidad && ultimaDisponibilidad.habitacionesDisponibles > 0);
    botonConfirmarReserva.classList.toggle("is-hidden", !sePuedeMostrar);
    botonConfirmarReserva.disabled = !sePuedeMostrar;
}

function formatearImporte(valor) {
    if (typeof valor !== "number") {
        return "";
    }

    return new Intl.NumberFormat("es-ES", {
        style: "currency",
        currency: "EUR"
    }).format(valor);
}

function formatearFecha(valor) {
    if (!valor) {
        return "";
    }

    const fecha = typeof valor === "string" && /^\d{4}-\d{2}-\d{2}$/.test(valor)
        ? new Date(`${valor}T00:00:00`)
        : new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
        return String(valor);
    }

    return new Intl.DateTimeFormat("es-ES", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    }).format(fecha);
}

function renderizarEstadoHoreca(estado, titulo, texto) {
    if (!estadoHoreca) {
        return;
    }

    estadoHoreca.dataset.state = estado;
    estadoHoreca.innerHTML = `
        <p class="availability-label">Cliente HORECA</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
}

function renderizarEstadoReservas(estado, titulo, texto) {
    if (!estadoReservas) {
        return;
    }

    estadoReservas.dataset.state = estado;
    estadoReservas.innerHTML = `
        <p class="availability-label">Estado de reservas</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
}

function renderizarEstadoCarrito(estado, titulo, texto) {
    if (!estadoCarrito) {
        return;
    }

    estadoCarrito.dataset.state = estado;
    estadoCarrito.innerHTML = `
        <p class="availability-label">Estado del carrito</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
}

function esReservaFutura(fechaEntrada) {
    if (!fechaEntrada) {
        return false;
    }

    const hoyIso = new Date().toISOString().split("T")[0];
    return fechaEntrada > hoyIso;
}
