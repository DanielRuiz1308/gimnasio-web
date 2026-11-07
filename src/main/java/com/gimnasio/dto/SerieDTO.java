package com.gimnasio.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Una serie de datos para un gráfico: pares (etiqueta, valor).
 * Entrega las etiquetas y los valores como texto simple para pasarlos a la página
 * sin depender de ninguna librería de JSON.
 */
public class SerieDTO {

    private final List<String> etiquetas = new ArrayList<>();
    private final List<BigDecimal> valores = new ArrayList<>();

    public SerieDTO agregar(String etiqueta, BigDecimal valor) {
        etiquetas.add(etiqueta);
        valores.add(valor);
        return this;
    }

    public SerieDTO agregar(String etiqueta, long valor) {
        return agregar(etiqueta, BigDecimal.valueOf(valor));
    }

    /** "Masculino|Femenino|Otro" */
    public String getEtiquetas() {
        return String.join("|", etiquetas);
    }

    /** "12,8,1" */
    public String getValores() {
        List<String> textos = new ArrayList<>();
        for (BigDecimal v : valores) {
            textos.add(v.stripTrailingZeros().toPlainString());
        }
        return String.join(",", textos);
    }

    /** true si no hay nada que graficar (sin puntos o todos en cero). */
    public boolean isVacia() {
        for (BigDecimal v : valores) {
            if (v.signum() != 0) {
                return false;
            }
        }
        return true;
    }
}