// Muestra el selector de entrenador solo cuando el rol es "entrenador"
(function () {
    var rol = document.getElementById('rol');
    var bloque = document.getElementById('bloque-entrenador');
    var select = document.getElementById('entrenadorId');
    if (!rol || !bloque || !select) return;

    function actualizar() {
        var esEntrenador = rol.value === 'entrenador';
        bloque.style.display = esEntrenador ? '' : 'none';
        select.disabled = !esEntrenador;
        select.required = esEntrenador;
    }
    rol.addEventListener('change', actualizar);
    actualizar();
})();
