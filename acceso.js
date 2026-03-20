const formularioAcceso = document.querySelector("#auth-form");
const campoUsuario = document.querySelector("#auth-username");
const campoContrasena = document.querySelector("#auth-password");
const estadoSesion = document.querySelector("#session-status");
const formularioRegistro = document.querySelector("#register-form");
const estadoRegistro = document.querySelector("#register-status");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";

const sesionGuardada = cargarSesion();
if (sesionGuardada) {
    redirigirSegunPerfil(sesionGuardada.rol);
}

formularioAcceso.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const contrasena = campoContrasena.value.trim();
    const contrasenaValida = /^[A-Za-z0-9]{1,12}$/.test(contrasena);

    if (!contrasenaValida) {
        renderizarEstadoSesion(
            "aviso",
            "Contrasena no valida",
            "La contrasena debe tener un maximo de 12 caracteres alfanumericos."
        );
        campoContrasena.focus();
        return;
    }

    try {
        const respuesta = await fetch("/api/sesion/iniciar", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                usuario: campoUsuario.value.trim(),
                contrasena
            })
        });

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo iniciar sesion.");
        }

        localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(datos));
        renderizarEstadoSesion("disponible", "Acceso concedido", "Redirigiendo al frontal correspondiente...");
        formularioAcceso.reset();
        window.setTimeout(() => redirigirSegunPerfil(datos.rol), 450);
    } catch (error) {
        renderizarEstadoSesion(
            "aviso",
            "Acceso denegado",
            error.message || "Usuario o contrasena incorrectos."
        );
    }
});

formularioRegistro.addEventListener("submit", (evento) => {
    evento.preventDefault();

    const nombre = document.querySelector("#register-name").value.trim();
    const correo = document.querySelector("#register-email").value.trim();
    const tipo = document.querySelector("#register-type").value;

    estadoRegistro.dataset.state = "disponible";
    estadoRegistro.innerHTML = `
        <p class="availability-label">Solicitud</p>
        <strong class="availability-title">Registro preparado</strong>
        <p class="availability-text">Solicitud registrada para ${nombre || correo} en la linea ${tipo}. Este frontal ya queda listo para conectarse a captacion real.</p>
    `;

    formularioRegistro.reset();
});

function cargarSesion() {
    try {
        const valorGuardado = localStorage.getItem(CLAVE_ALMACENAMIENTO);
        return valorGuardado ? JSON.parse(valorGuardado) : null;
    } catch {
        return null;
    }
}

function renderizarEstadoSesion(estado, titulo, texto) {
    estadoSesion.dataset.state = estado;
    estadoSesion.innerHTML = `
        <p class="availability-label">Estado de acceso</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
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

    window.location.href = "index.html";
}
