// Vista previa de la foto y reducción de tamaño antes de enviarla.
// Recorta al centro en cuadrado, la achica a máximo 480 px y la convierte a JPG,
// así una foto de 6 MB del celular termina pesando unos 40 KB.
document.addEventListener('DOMContentLoaded', function () {
    var entrada = document.getElementById('foto');
    var marco = document.getElementById('foto-marco');
    if (!entrada || !marco) return;

    var LADO_MAXIMO = 480;

    entrada.addEventListener('change', function () {
        var archivo = entrada.files[0];
        if (!archivo) return;

        if (archivo.type.indexOf('image/') !== 0) {
            alert('Elige un archivo de imagen (JPG, PNG o WebP).');
            entrada.value = '';
            return;
        }

        var urlTemporal = URL.createObjectURL(archivo);
        var imagen = new Image();

        imagen.onload = function () {
            var lado = Math.min(imagen.width, imagen.height);
            var origenX = (imagen.width - lado) / 2;
            var origenY = (imagen.height - lado) / 2;
            var destino = Math.min(LADO_MAXIMO, lado);

            var lienzo = document.createElement('canvas');
            lienzo.width = destino;
            lienzo.height = destino;
            var ctx = lienzo.getContext('2d');
            ctx.fillStyle = '#1A1A1A'; // fondo para PNG con transparencia
            ctx.fillRect(0, 0, destino, destino);
            ctx.drawImage(imagen, origenX, origenY, lado, lado, 0, 0, destino, destino);
            URL.revokeObjectURL(urlTemporal);

            lienzo.toBlob(function (blob) {
                var reducida = new File([blob], 'foto.jpg', { type: 'image/jpeg' });
                var lista = new DataTransfer();
                lista.items.add(reducida);
                entrada.files = lista.files;
                mostrar(URL.createObjectURL(blob));
            }, 'image/jpeg', 0.85);
        };

        imagen.onerror = function () {
            URL.revokeObjectURL(urlTemporal);
            alert('No se pudo leer esa imagen. Prueba con otra.');
            entrada.value = '';
        };

        imagen.src = urlTemporal;
    });

    function mostrar(src) {
        var img = marco.querySelector('img');
        if (!img) {
            img = document.createElement('img');
            img.alt = 'Foto';
            marco.appendChild(img);
        }
        img.src = src;
        var quitar = document.getElementById('quitarFoto');
        if (quitar) quitar.checked = false;
    }
});