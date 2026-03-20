const formularioAcceso = document.querySelector("#auth-form");
const campoUsuario = document.querySelector("#auth-username");
const campoContrasena = document.querySelector("#auth-password");
const estadoSesion = document.querySelector("#session-status");
const formularioRegistro = document.querySelector("#register-form");
const estadoRegistro = document.querySelector("#register-status");
const campoRegistroDocumento = document.querySelector("#register-document");
const campoRegistroNombre = document.querySelector("#register-name");
const campoRegistroApellido = document.querySelector("#register-surname");
const campoRegistroCorreo = document.querySelector("#register-email");
const campoRegistroTelefono = document.querySelector("#register-phone");
const campoRegistroContrasena = document.querySelector("#register-password");

const CLAVE_ALMACENAMIENTO = "linea_cano_sesion";

inicializarSesionGuardada();

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

formularioRegistro.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const contrasena = campoRegistroContrasena.value.trim();
    const contrasenaValida = /^[A-Za-z0-9]{1,12}$/.test(contrasena);

    if (!contrasenaValida) {
        renderizarEstadoRegistro(
            "aviso",
            "Contrasena no valida",
            "La contrasena debe tener un maximo de 12 caracteres alfanumericos."
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

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo completar el registro.");
        }

        renderizarEstadoRegistro(
            "disponible",
            datos.titulo || "Registro completado",
            `${datos.mensaje} Ya puedes iniciar sesion con tu correo y contrasena.`
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
            throw new Error("La sesion ya no es valida.");
        }

        const datos = await respuesta.json();
        localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(datos));
        redirigirSegunPerfil(datos.rol);
    } catch {
        localStorage.removeItem(CLAVE_ALMACENAMIENTO);
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

    if (rol === "registrado" || rol === "horeca") {
        window.location.href = "cliente.html";
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
