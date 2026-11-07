// Dibuja los gráficos de la pantalla de reportes con Chart.js.
// Cada <canvas> trae sus datos en data-etiquetas ("a|b|c") y data-valores ("1,2,3").
document.addEventListener('DOMContentLoaded', function () {
    var ROJO = '#E31E24';
    var COLORES = ['#E31E24', '#F5F5F5', '#A0A0A0', '#7A1115', '#5C5C5C'];

    if (typeof Chart === 'undefined') {
        var avisos = document.querySelectorAll('.sin-datos');
        for (var i = 0; i < avisos.length; i++) {
            avisos[i].textContent = 'No se pudo cargar la librería de gráficos (falta static/js/chart.umd.js).';
            avisos[i].hidden = false;
        }
        return;
    }

    Chart.defaults.color = '#A0A0A0';
    Chart.defaults.borderColor = '#2A2A2A';
    Chart.defaults.font.family = '"Segoe UI", Arial, sans-serif';

    function pesos(valor) {
        return '$' + Number(valor).toLocaleString('es-CO');
    }

    // Lee los datos del canvas. Devuelve null (y muestra "sin datos") si todo está en cero.
    function leer(id) {
        var canvas = document.getElementById(id);
        if (!canvas) return null;

        var etiquetas = canvas.getAttribute('data-etiquetas').split('|');
        var valores = canvas.getAttribute('data-valores').split(',').map(Number);
        var hayDatos = valores.some(function (v) { return v !== 0; });

        if (!hayDatos) {
            canvas.hidden = true;
            canvas.parentNode.querySelector('.sin-datos').hidden = false;
            return null;
        }
        return { canvas: canvas, etiquetas: etiquetas, valores: valores };
    }

    function barras(id, nombreSerie, formato) {
        var d = leer(id);
        if (!d) return;
        new Chart(d.canvas, {
            type: 'bar',
            data: {
                labels: d.etiquetas,
                datasets: [{ label: nombreSerie, data: d.valores, backgroundColor: ROJO, borderRadius: 4 }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return formato ? formato(ctx.parsed.y) : ctx.parsed.y + ' ' + nombreSerie.toLowerCase();
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: formato ? { callback: function (v) { return formato(v); } } : { precision: 0 }
                    },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    function dona(id) {
        var d = leer(id);
        if (!d) return;
        new Chart(d.canvas, {
            type: 'doughnut',
            data: {
                labels: d.etiquetas,
                datasets: [{ data: d.valores, backgroundColor: COLORES, borderColor: '#1A1A1A', borderWidth: 3 }]
            },
            options: {
                maintainAspectRatio: false,
                cutout: '60%',
                plugins: { legend: { position: 'bottom', labels: { padding: 16 } } }
            }
        });
    }

    barras('grafico-ingresos', 'Ingresos', pesos);
    dona('grafico-genero');
    barras('grafico-edad', 'Clientes');
    dona('grafico-turno');
});