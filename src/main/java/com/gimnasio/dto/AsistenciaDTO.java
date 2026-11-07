package com.gimnasio.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Una asistencia con los nombres del cliente y del entrenador ya resueltos (JOIN). */
public class AsistenciaDTO {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final int id;
    private final int clienteId;
    private final String clienteNombre;
    private final String entrenadorNombre;
    private final LocalDate fecha;
    private final LocalTime hora;
    private final String turno;

    public AsistenciaDTO(int id, int clienteId, String clienteNombre, String entrenadorNombre,
                         LocalDate fecha, LocalTime hora, String turno) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.entrenadorNombre = entrenadorNombre;
        this.fecha = fecha;
        this.hora = hora;
        this.turno = turno;
    }

    public int getId() {
        return id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getEntrenadorNombre() {
        return entrenadorNombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    /** Hora como "07:15", lista para mostrar. */
    public String getHoraTexto() {
        return hora.format(HORA);
    }

    public String getTurno() {
        return turno;
    }
}