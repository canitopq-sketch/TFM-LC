const cabeceraSitio = document.querySelector(".site-header");
const formularioAcceso = document.querySelector("#auth-form");
const campoUsuario = document.querySelector("#auth-username");
const campoContrasena = document.querySelector("#auth-password");
const estadoSesion = document.querySelector("#session-status");
const botonCerrarSesion = document.querySelector("#logout-button");
const formularioReserva = document.querySelector("#booking-form");
const campoFechaEntrada = document.querySelector("#checkin");
const campoFechaSalida = document.querySelector("#checkout");
const campoHuespedes = document.querySelector("#guests");
const tarjetaDisponibilidad = document.querySelector("#availability-card");
const botonConfirmarReserva = document.querySelector("#booking-confirm");
const resumenReserva = document.querySelector("#booking-summary");
const portalPublico = document.querySelector("#public-portal");
const portalPrivado = document.querySelector("#private-portal");
const tituloRolPrivado = document.querySelector("#private-role-title");
const textoRolPrivado = document.querySelector("#private-role-copy");
const contenedorFuncionesPrivadas = document.querySelector("#private-role-features");
const nodosImagenDinamica = document.querySelectorAll(".dynamic-image");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";
const DESPLAZAMIENTO_PARA_COMPACTAR = 260;
const DESPLAZAMIENTO_PARA_EXPANDIR = 40;

let sesionActiva = cargarSesion();
let cabeceraCompacta = false;
let animacionPendiente = false;
let ultimaDisponibilidad = null;

window.addEventListener("scroll", () => {
    if (animacionPendiente) {
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

formularioAcceso.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    try {
        const respuesta = await fetch("/api/sesion/iniciar", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                usuario: campoUsuario.value.trim(),
                contrasena: campoContrasena.value
            })
        });

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo iniciar sesion.");
        }

        sesionActiva = datos;
        guardarSesion(sesionActiva);
        formularioAcceso.reset();
        sincronizarInterfazSegunSesion();
    } catch (error) {
        renderizarEstadoSesion("publico", "Acceso denegado", error.message || "Credenciales incorrectas.");
    }
});

botonCerrarSesion.addEventListener("click", () => {
    sesionActiva = null;
    ultimaDisponibilidad = null;
    localStorage.removeItem(CLAVE_ALMACENAMIENTO);
    sincronizarInterfazSegunSesion();
});

formularioReserva.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    if (!usuarioPuedeReservar()) {
        renderizarDisponibilidad(
            "Acceso privado requerido",
            "Solo los clientes registrados y el equipo comercial pueden consultar disponibilidad real.",
            "bloqueado"
        );
        return;
    }

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
    if (!usuarioPuedeReservar()) {
        return;
    }

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

sincronizarInterfazSegunSesion();
hidratarImagenesDinamicas();

function cargarSesion() {
    try {
        const valorGuardado = localStorage.getItem(CLAVE_ALMACENAMIENTO);
        return valorGuardado ? JSON.parse(valorGuardado) : null;
    } catch {
        return null;
    }
}

function guardarSesion(sesion) {
    localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(sesion));
}

function sincronizarInterfazSegunSesion() {
    const rol = sesionActiva?.rol || "publico";
    const portalPrivadoActivo = rol === "registrado" || rol === "maestro";

    portalPublico.classList.toggle("is-hidden", portalPrivadoActivo);
    portalPrivado.classList.toggle("is-hidden", !portalPrivadoActivo);
    resumenReserva.classList.toggle("is-hidden", !portalPrivadoActivo);
    botonCerrarSesion.classList.toggle("is-hidden", !sesionActiva);
    bloquearFormularioReserva(!portalPrivadoActivo);
    actualizarBotonReserva();

    if (!sesionActiva) {
        ultimaDisponibilidad = null;
        renderizarEstadoSesion(
            "publico",
            "Vista publica activa",
            "Accede con un usuario valido para desbloquear disponibilidad, reserva y seguimiento de estancia."
        );
        renderizarDisponibilidad(
            "Acceso privado requerido",
            "Inicia sesion para consultar disponibilidad real y crear una reserva.",
            "bloqueado"
        );
        restablecerResumenReserva();
        return;
    }

    if (rol === "maestro") {
        tituloRolPrivado.textContent = "Sesion comercial activa con acceso a disponibilidad y reserva.";
        textoRolPrivado.textContent = "El perfil comercial puede validar ocupacion real, confirmar reservas y preparar futuras pantallas de gestion.";
        contenedorFuncionesPrivadas.innerHTML = `
            <article>
                <strong>Disponibilidad real</strong>
                <span>Consulta el inventario vivo de habitaciones para cualquier rango de fechas.</span>
            </article>
            <article>
                <strong>Reserva de prueba</strong>
                <span>Puedes confirmar una reserva para validar el flujo tecnico completo del TFM.</span>
            </article>
            <article>
                <strong>Base para gestion</strong>
                <span>La sesion comercial queda preparada para evolucionar a paneles de operacion y seguimiento.</span>
            </article>
        `;
        renderizarEstadoSesion(
            "maestro",
            "Sesion comercial activa",
            `Has accedido como ${sesionActiva.nombreVisible}. La zona privada y las llamadas JSON ya estan habilitadas.`
        );
    } else {
        tituloRolPrivado.textContent = "Cliente registrado con acceso a disponibilidad y reserva.";
        textoRolPrivado.textContent = "Desde esta vista privada puedes consultar fechas reales, revisar ocupacion y crear una reserva para validar el sistema.";
        contenedorFuncionesPrivadas.innerHTML = `
            <article>
                <strong>Disponibilidad real</strong>
                <span>Consulta si hay habitaciones libres para tus fechas sobre la base de datos del proyecto.</span>
            </article>
            <article>
                <strong>Confirmacion inmediata</strong>
                <span>Si existe disponibilidad, la reserva se registra y queda vinculada al cliente autenticado.</span>
            </article>
            <article>
                <strong>Experiencia personalizada</strong>
                <span>El siguiente paso sera asociar servicios de mesa, bienestar y preferencias de estancia.</span>
            </article>
        `;
        renderizarEstadoSesion(
            "registrado",
            "Sesion de cliente activa",
            `Has accedido como ${sesionActiva.nombreVisible}. Ya puedes consultar disponibilidad real y crear una reserva.`
        );
    }

    renderizarDisponibilidad(
        "Zona privada lista",
        "Selecciona fechas y huespedes para consultar la disponibilidad real de la finca.",
        "disponible"
    );
    restablecerResumenReserva();
}

function usuarioPuedeReservar() {
    return Boolean(sesionActiva && (sesionActiva.rol === "registrado" || sesionActiva.rol === "maestro"));
}

function bloquearFormularioReserva(estaBloqueado) {
    formularioReserva.classList.toggle("is-locked", estaBloqueado);
    formularioReserva.querySelectorAll("input, select").forEach((campo) => {
        campo.disabled = false;
    });

    const botonConsulta = formularioReserva.querySelector('button[type="submit"]');
    if (botonConsulta) {
        botonConsulta.disabled = false;
    }

    botonConfirmarReserva.disabled = estaBloqueado;
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

function renderizarEstadoSesion(estado, titulo, texto) {
    estadoSesion.dataset.state = estado;
    estadoSesion.innerHTML = `
        <p class="availability-label">Estado del portal</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
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

function restablecerResumenReserva() {
    actualizarResumenReserva({
        habitacionesDisponibles: "Sin consulta",
        habitacionesReservadas: "Sin consulta",
        totalHabitaciones: "Sin consulta"
    });
}

function actualizarBotonReserva() {
    const sePuedeMostrar = usuarioPuedeReservar() && ultimaDisponibilidad && ultimaDisponibilidad.habitacionesDisponibles > 0;
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

function hidratarImagenesDinamicas() {
    nodosImagenDinamica.forEach((nodo) => {
        const rutaLocal = nodo.dataset.imageLocal;
        const rutaFallback = nodo.dataset.imageFallback;
        const superposicion = nodo.dataset.imageOverlay || "";

        cargarImagen(rutaLocal)
            .then(() => {
                nodo.style.backgroundImage = `${superposicion}, url("${rutaLocal}")`;
            })
            .catch(() => {
                if (rutaFallback) {
                    nodo.style.backgroundImage = `${superposicion}, url("${rutaFallback}")`;
                }
            });
    });
}

function cargarImagen(ruta) {
    return new Promise((resolve, reject) => {
        if (!ruta) {
            reject(new Error("No se ha indicado una ruta de imagen."));
            return;
        }

        const imagen = new Image();
        imagen.onload = () => resolve(ruta);
        imagen.onerror = () => reject(new Error(`No se ha podido cargar la imagen ${ruta}`));
        imagen.src = ruta;
    });
}
