// Agregar y quitar filas de ejercicios en el formulario de rutinas.
document.addEventListener('DOMContentLoaded', function () {
    var cuerpo = document.getElementById('ejercicios-cuerpo');
    var modelo = document.getElementById('fila-modelo');
    var boton = document.getElementById('agregar-ejercicio');
    if (!cuerpo || !modelo || !boton) return;

    function agregar(enfocar) {
        var fila = modelo.content.firstElementChild.cloneNode(true);
        cuerpo.appendChild(fila);
        if (enfocar) fila.querySelector('input').focus();
    }

    boton.addEventListener('click', function () {
        agregar(true);
    });

    cuerpo.addEventListener('click', function (evento) {
        var quitar = evento.target.closest('.quitar-fila');
        if (!quitar) return;
        quitar.closest('tr').remove();
        if (cuerpo.children.length === 0) agregar(false); // siempre queda al menos una fila
    });

    if (cuerpo.children.length === 0) agregar(false);
});