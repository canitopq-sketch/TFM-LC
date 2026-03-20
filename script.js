const nodosImagenDinamica = document.querySelectorAll(".dynamic-image");

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
