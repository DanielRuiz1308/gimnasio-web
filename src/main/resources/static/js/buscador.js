// Buscador para tablas: filtra las filas mientras se escribe.
// - Ignora mayúsculas y tildes ("jose" encuentra "José").
// - Si hay varias palabras, todas deben aparecer en la fila ("ana 300").
// - No busca dentro de la columna de acciones (Editar / Eliminar).
// Uso: un <input id="buscador"> y una tabla con class="tabla".
document.addEventListener("DOMContentLoaded", function () {
    var campo = document.getElementById("buscador");
    if (!campo) {
        return;
    }

    var tabla = document.querySelector(campo.getAttribute("data-tabla") || "table.tabla");
    var contador = document.getElementById("contador");
    var sinResultados = document.getElementById("sin-resultados");

    // Solo las filas con datos (no el mensaje "todavía no hay registros")
    var filas = Array.prototype.slice.call(tabla.querySelectorAll("tbody tr")).filter(function (fila) {
        return !fila.querySelector("td.vacio");
    });

    function normalizar(texto) {
        return texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    }

    function textoDeFila(fila) {
        var partes = [];
        Array.prototype.forEach.call(fila.cells, function (celda) {
            if (!celda.classList.contains("col-acciones")) {
                partes.push(celda.textContent);
            }
        });
        return normalizar(partes.join(" "));
    }

    // El texto de cada fila se calcula una sola vez
    var textos = filas.map(textoDeFila);

    function filtrar() {
        var palabras = normalizar(campo.value).trim().split(/\s+/).filter(Boolean);
        var visibles = 0;

        filas.forEach(function (fila, i) {
            var coincide = palabras.every(function (palabra) {
                return textos[i].indexOf(palabra) !== -1;
            });
            fila.hidden = !coincide;
            if (coincide) {
                visibles++;
            }
        });

        if (contador) {
            if (palabras.length === 0) {
                contador.textContent = filas.length + (filas.length === 1 ? " registro" : " registros");
            } else {
                contador.textContent = visibles + " de " + filas.length + " resultados";
            }
        }

        if (sinResultados) {
            sinResultados.hidden = !(filas.length > 0 && visibles === 0);
        }
    }

    campo.addEventListener("input", filtrar);
    filtrar();
});