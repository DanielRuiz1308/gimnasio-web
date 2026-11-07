package com.gimnasio.dto;

import java.math.BigDecimal;

/** Todos los números de la pantalla de reportes en un solo objeto. */
public class ReporteDTO {

    private final int totalClientes;
    private final int membresiasVigentes;
    private final int membresiasVencidas;
    private final int sinMembresia;
    private final BigDecimal ingresosMes;
    private final SerieDTO ingresosPorMes;
    private final SerieDTO clientesPorGenero;
    private final SerieDTO clientesPorEdad;
    private final SerieDTO asistenciasPorTurno;

    public ReporteDTO(int totalClientes, int membresiasVigentes, int membresiasVencidas, int sinMembresia,
                      BigDecimal ingresosMes, SerieDTO ingresosPorMes, SerieDTO clientesPorGenero,
                      SerieDTO clientesPorEdad, SerieDTO asistenciasPorTurno) {
        this.totalClientes = totalClientes;
        this.membresiasVigentes = membresiasVigentes;
        this.membresiasVencidas = membresiasVencidas;
        this.sinMembresia = sinMembresia;
        this.ingresosMes = ingresosMes;
        this.ingresosPorMes = ingresosPorMes;
        this.clientesPorGenero = clientesPorGenero;
        this.clientesPorEdad = clientesPorEdad;
        this.asistenciasPorTurno = asistenciasPorTurno;
    }

    public int getTotalClientes() {
        return totalClientes;
    }

    public int getMembresiasVigentes() {
        return membresiasVigentes;
    }

    public int getMembresiasVencidas() {
        return membresiasVencidas;
    }

    public int getSinMembresia() {
        return sinMembresia;
    }

    public BigDecimal getIngresosMes() {
        return ingresosMes;
    }

    public SerieDTO getIngresosPorMes() {
        return ingresosPorMes;
    }

    public SerieDTO getClientesPorGenero() {
        return clientesPorGenero;
    }

    public SerieDTO getClientesPorEdad() {
        return clientesPorEdad;
    }

    public SerieDTO getAsistenciasPorTurno() {
        return asistenciasPorTurno;
    }
}
