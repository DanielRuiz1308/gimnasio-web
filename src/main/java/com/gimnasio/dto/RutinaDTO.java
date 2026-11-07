package com.gimnasio.dto;

import java.util.List;

/**
 * Una rutina con el nombre del cliente y del entrenador ya resueltos (JOIN).
 * En la lista, "ejercicios" viene vacía y totalEjercicios trae la cantidad;
 * al abrir una rutina, "ejercicios" trae el detalle.
 */
public class RutinaDTO {

    private final int id;
    private final int clienteId;
    private final String clienteNombre;
    private final int entrenadorId;
    private final String entrenadorNombre;
    private final String nombre;
    private final String nivel;
    private final String objetivo;
    private final int totalEjercicios;
    private final List<EjercicioDTO> ejercicios;

    public RutinaDTO(int id, int clienteId, String clienteNombre, int entrenadorId, String entrenadorNombre,
                     String nombre, String nivel, String objetivo, int totalEjercicios,
                     List<EjercicioDTO> ejercicios) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.entrenadorId = entrenadorId;
        this.entrenadorNombre = entrenadorNombre;
        this.nombre = nombre;
        this.nivel = nivel;
        this.objetivo = objetivo;
        this.totalEjercicios = totalEjercicios;
        this.ejercicios = ejercicios;
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

    public int getEntrenadorId() {
        return entrenadorId;
    }

    public String getEntrenadorNombre() {
        return entrenadorNombre;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNivel() {
        return nivel;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public int getTotalEjercicios() {
        return totalEjercicios;
    }

    public List<EjercicioDTO> getEjercicios() {
        return ejercicios;
    }
}