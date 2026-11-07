// Une el cuadro de búsqueda de cliente con el campo oculto clienteId.
// El usuario escribe nombre o documento, elige una opción de la lista y aquí guardamos su id.
// Se usa en Asistencia y en Rutinas.
document.addEventListener('DOMContentLoaded', function () {
    var texto = document.getElementById('cliente-texto');
    var oculto = document.getElementById('clienteId');
    var lista = document.getElementById('lista-clientes');
    var aviso = document.getElementById('cliente-aviso');
    if (!texto || !oculto || !lista || !aviso || !texto.form) return;
    var formulario = texto.form;

    function resolver() {
        var opcion = null;
        for (var i = 0; i < lista.options.length; i++) {
            if (lista.options[i].value === texto.value) {
                opcion = lista.options[i];
                break;
            }
        }
        oculto.value = opcion ? opcion.getAttribute('data-id') : '';

        // Solo Asistencia trae el estado de la membresía en cada opción
        var estado = opcion ? opcion.getAttribute('data-estado') : null;
        if (opcion && estado !== null && estado !== 'Vigente') {
            aviso.textContent = estado === 'Vencida'
                ? 'Atención: la membresía de este cliente está vencida.'
                : 'Atención: este cliente no tiene membresía.';
            aviso.hidden = false;
        } else {
            aviso.hidden = true;
        }
    }

    texto.addEventListener('input', resolver);
    texto.addEventListener('change', resolver);

    formulario.addEventListener('submit', function (evento) {
        resolver();
        if (!oculto.value) {
            evento.preventDefault();
            aviso.textContent = 'Elige un cliente de la lista de sugerencias.';
            aviso.hidden = false;
            texto.focus();
        }
    });
});