const cabeceraSitio = document.querySelector(".site-header");
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

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";
const DESPLAZAMIENTO_PARA_COMPACTAR = 260;
const DESPLAZAMIENTO_PARA_EXPANDIR = 40;

let sesionActiva = cargarSesion();
let cabeceraCompacta = false;
let animacionPendiente = false;
let ultimaDisponibilidad = null;

protegerPagina();
configurarCabecera();
configurarFechas();

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

function cargarSesion() {
    try {
        const valorGuardado = localStorage.getItem(CLAVE_ALMACENAMIENTO);
        return valorGuardado ? JSON.parse(valorGuardado) : null;
    } catch {
        return null;
    }
}

function protegerPagina() {
    const tipoPagina = document.body.dataset.privatePage;

    if (!sesionActiva) {
        window.location.href = "acceso.html";
        return;
    }

    if (tipoPagina === "cliente" && sesionActiva.rol !== "registrado") {
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

    if (rol === "registrado") {
        window.location.href = "cliente.html";
        return;
    }

    window.location.href = "acceso.html";
}

function configurarCabecera() {
    window.addEventListener("scroll", () => {
        if (!cabeceraSitio || animacionPendiente) {
            return;
        }

        animacionPendiente = true;
        window.requestAnimationFrame(() => {
            const desplazamientoActual = window.scrollY;
            let siguienteEstadoCompacto = cabeceraCompacta;

            if (!cabeceraCompacta && desplazamientoActual > DESPLAZAMIENTO_PARA_COMPACTAR) {
                siguienteEstadoCompacto = true;
            } else if (cabeceraCompacta && desplazamientoActual < DESPLAZAMIENTO_PARA_EXPANDIR) {
                siguienteEstadoCompacto = false;
            }

            if (siguienteEstadoCompacto !== cabeceraCompacta) {
                cabeceraCompacta = siguienteEstadoCompacto;
                cabeceraSitio.classList.toggle("is-compact", cabeceraCompacta);
            }

            animacionPendiente = false;
        });
    }, { passive: true });
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
