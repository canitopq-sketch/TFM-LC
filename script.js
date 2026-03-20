const cabeceraSitio = document.querySelector(".site-header");
const nodosImagenDinamica = document.querySelectorAll(".dynamic-image");

const DESPLAZAMIENTO_PARA_COMPACTAR = 260;
const DESPLAZAMIENTO_PARA_EXPANDIR = 40;

let cabeceraCompacta = false;
let animacionPendiente = false;

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

hidratarImagenesDinamicas();

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
