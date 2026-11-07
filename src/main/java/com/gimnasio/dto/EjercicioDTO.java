package com.gimnasio.dto;

/** Un ejercicio dentro de una rutina. descanso (en segundos) puede ser null. */
public class EjercicioDTO {

    private final String nombre;
    private final int series;
    private final int repeticiones;
    private final Integer descanso;

    public EjercicioDTO(String nombre, int series, int repeticiones, Integer descanso) {
        this.nombre = nombre;
        this.series = series;
        this.repeticiones = repeticiones;
        this.descanso = descanso;
    }

    public String getNombre() {
        return nombre;
    }

    public int getSeries() {
        return series;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public Integer getDescanso() {
        return descanso;
    }
}