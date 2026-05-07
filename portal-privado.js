const botonCerrarSesion = document.querySelector("#logout-button");
const formularioReserva = document.querySelector("#booking-form");
const campoFechaEntrada = document.querySelector("#checkin");
const campoFechaSalida = document.querySelector("#checkout");
const campoHuespedes = document.querySelector("#guests");
const tarjetaDisponibilidad = document.querySelector("#availability-card");
const botonConfirmarReserva = document.querySelector("#booking-confirm");
const resumenReserva = document.querySelector("#booking-summary");
const estadoSesion = document.querySelector("#session-status");
const tituloPrivado = document.querySelector("#private-page-title");
const nombrePrivado = document.querySelector("#private-user-name");
const textoPrivado = document.querySelector("#private-user-copy");
const formularioHoreca = document.querySelector("#horeca-form");
const estadoHoreca = document.querySelector("#horeca-status");
const botonActualizarReservas = document.querySelector("#refresh-reservations");
const estadoReservas = document.querySelector("#reservations-status");
const historialReservas = document.querySelector("#reservation-history");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";
let sesionActiva = cargarSesion();
let ultimaDisponibilidad = null;

inicializarPortalPrivado();

formularioReserva.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    if (!fechasSonValidas()) {
        return;
    }

    try {
        const respuesta = await fetch(
            `/api/disponibilidad?fechaEntrada=${campoFechaEntrada.value}&fechaSalida=${campoFechaSalida.value}&huespedes=${campoHuespedes.value}`,
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

        const datos = await respuesta.json();

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

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo crear la reserva.");
        }

        renderizarDisponibilidad(
            datos.titulo,
            `${datos.mensaje} Habitacion ${datos.habitacionAsignada}. Reserva ${datos.idReserva}. Importe estimado ${formatearImporte(datos.importeTotal)}.`,
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
            empresa: document.querySelector("#horeca-company").value.trim(),
            cif: document.querySelector("#horeca-cif").value.trim(),
            nombreContacto: document.querySelector("#horeca-name").value.trim(),
            apellidoContacto: document.querySelector("#horeca-surname").value.trim(),
            documento: document.querySelector("#horeca-document").value.trim(),
            correo: document.querySelector("#horeca-email").value.trim(),
            telefono: document.querySelector("#horeca-phone").value.trim(),
            contrasena: document.querySelector("#horeca-password").value.trim()
        };

        if (!/^[A-Za-z0-9]{1,12}$/.test(datosFormulario.contrasena)) {
            renderizarEstadoHoreca(
                "aviso",
                "Contrasena no valida",
                "La contrasena inicial debe tener un maximo de 12 caracteres alfanumericos."
            );
            document.querySelector("#horeca-password").focus();
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

            const datos = await respuesta.json();

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
    configurarFechas();
    configurarHistoricoReservas();
}

function protegerPagina() {
    const tipoPagina = document.body.dataset.privatePage;

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
        tituloPrivado.textContent = "Frontal de cliente con acceso a disponibilidad real y confirmacion de reserva.";
        textoPrivado.textContent = "Este entorno privado esta pensado para preparar la estancia y confirmar fechas directamente sobre el sistema.";
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

function configurarHistoricoReservas() {
    if (!historialReservas || document.body.dataset.privatePage !== "cliente") {
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

        const datos = await respuesta.json();

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
                <h3>${reserva.fechaEntrada} al ${reserva.fechaSalida}</h3>
                <p class="history-meta">Estado: ${reserva.estado} · Habitacion: ${reserva.habitacionAsignada ?? "Sin asignar"} · Importe: ${formatearImporte(reserva.importeTotal) || "Pendiente"}</p>
                ${botonCancelar}
            </article>
        `;
    }).join("");

    historialReservas.querySelectorAll("[data-action='cancelar-reserva']").forEach((boton) => {
        boton.addEventListener("click", () => cancelarReservaCliente(boton.dataset.idReserva));
    });
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

        const datos = await respuesta.json();

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

        return await respuesta.json();
    } catch {
        return null;
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

function esReservaFutura(fechaEntrada) {
    if (!fechaEntrada) {
        return false;
    }

    const hoyIso = new Date().toISOString().split("T")[0];
    return fechaEntrada > hoyIso;
}
