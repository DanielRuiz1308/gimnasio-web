package com.gimnasio.dto;

import java.math.BigDecimal;

/** Un tipo de membresía con su precio, para mostrar en el formulario de renovación. */
public class TipoMembresiaDTO {

    private final int id;
    private final String nombre;
    private final BigDecimal precio;

    public TipoMembresiaDTO(int id, String nombre, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }
}
