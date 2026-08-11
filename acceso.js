const formularioAcceso = document.querySelector("#formulario-acceso");
const campoUsuario = document.querySelector("#usuario-acceso");
const campoContrasena = document.querySelector("#contrasena-acceso");
const estadoSesion = document.querySelector("#estado-acceso");
const formularioRegistro = document.querySelector("#formulario-registro");
const estadoRegistro = document.querySelector("#estado-registro");
const campoRegistroDocumento = document.querySelector("#registro-documento");
const campoRegistroNombre = document.querySelector("#registro-nombre");
const campoRegistroApellido = document.querySelector("#registro-apellido");
const campoRegistroCorreo = document.querySelector("#registro-correo");
const campoRegistroTelefono = document.querySelector("#registro-telefono");
const campoRegistroContrasena = document.querySelector("#registro-contrasena");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";

inicializarSesionGuardada();

formularioAcceso.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const contrasena = campoContrasena.value.trim();
    const contrasenaValida = /^[A-Za-z0-9]{1,10}$/.test(contrasena);

    if (!contrasenaValida) {
        renderizarEstadoSesion(
            "aviso",
            "Contraseña no válida",
            "La contraseña debe tener un máximo de 10 caracteres alfanuméricos."
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

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo iniciar sesión.");
        }

        localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(datos));
        renderizarEstadoSesion("disponible", "Acceso concedido", "Redirigiendo al panel correspondiente...");
        formularioAcceso.reset();
        window.setTimeout(() => redirigirSegunPerfil(datos.rol), 450);
    } catch (error) {
        renderizarEstadoSesion(
            "aviso",
            "Acceso denegado",
            error.message || "Usuario o contraseña incorrectos."
        );
    }
});

formularioRegistro.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const contrasena = campoRegistroContrasena.value.trim();
    const contrasenaValida = /^[A-Za-z0-9]{1,10}$/.test(contrasena);

    if (!contrasenaValida) {
        renderizarEstadoRegistro(
            "aviso",
            "Contraseña no válida",
            "La contraseña debe tener un máximo de 10 caracteres alfanuméricos."
        );
        campoRegistroContrasena.focus();
        return;
    }

    try {
        const respuesta = await fetch("/api/usuarios/registro", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                documento: campoRegistroDocumento.value.trim(),
                nombre: campoRegistroNombre.value.trim(),
                apellido: campoRegistroApellido.value.trim(),
                correo: campoRegistroCorreo.value.trim(),
                telefono: campoRegistroTelefono.value.trim(),
                contrasena
            })
        });

        const datos = await leerRespuestaJson(respuesta);

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo completar el registro.");
        }

        renderizarEstadoRegistro(
            "disponible",
            datos.titulo || "Registro completado",
            `${datos.mensaje} Ya puedes iniciar sesión con tu correo y contraseña.`
        );
        formularioRegistro.reset();
    } catch (error) {
        renderizarEstadoRegistro(
            "aviso",
            "No se pudo crear la cuenta",
            error.message || "Revisa los datos y vuelve a intentarlo."
        );
    }
});

function cargarSesion() {
    try {
        const valorGuardado = localStorage.getItem(CLAVE_ALMACENAMIENTO);
        return valorGuardado ? JSON.parse(valorGuardado) : null;
    } catch {
        return null;
    }
}

async function inicializarSesionGuardada() {
    const sesionGuardada = cargarSesion();
    if (!sesionGuardada?.token) {
        return;
    }

    try {
        const respuesta = await fetch("/api/sesion/validar", {
            headers: {
                "Accept": "application/json",
                "X-Linea-Token": sesionGuardada.token
            }
        });

        if (!respuesta.ok) {
            throw new Error("La sesión ya no es válida.");
        }

        const datos = await leerRespuestaJson(respuesta);
        localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(datos));
        redirigirSegunPerfil(datos.rol);
    } catch {
        localStorage.removeItem(CLAVE_ALMACENAMIENTO);
    }
}

async function leerRespuestaJson(respuesta) {
    const tipoContenido = respuesta.headers.get("Content-Type") || "";
    const cuerpoTexto = await respuesta.text();

    if (!tipoContenido.includes("application/json")) {
        throw new Error("No hemos podido completar la solicitud. Recarga la página e inténtalo de nuevo.");
    }

    try {
        return JSON.parse(cuerpoTexto);
    } catch {
        throw new Error("No hemos podido completar la solicitud. Inténtalo de nuevo.");
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

    if (rol === "horeca") {
        window.location.href = "horeca-privado.html";
        return;
    }

    window.location.href = "index.html";
}

function renderizarEstadoRegistro(estado, titulo, texto) {
    estadoRegistro.dataset.state = estado;
    estadoRegistro.innerHTML = `
        <p class="availability-label">Registro</p>
        <strong class="availability-title">${titulo}</strong>
        <p class="availability-text">${texto}</p>
    `;
}
