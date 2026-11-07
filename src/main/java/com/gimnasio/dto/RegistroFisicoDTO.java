package com.gimnasio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Una medición física (peso y altura) de un cliente, con su nombre, el IMC calculado
 * y la variación de peso respecto a la medición anterior de ese mismo cliente.
 */
public class RegistroFisicoDTO {

    private final int id;
    private final int clienteId;
    private final String clienteNombre;
    private final LocalDate fecha;
    private final BigDecimal peso;      // kilogramos
    private final BigDecimal altura;    // metros
    private final BigDecimal variacion; // kg respecto a la medición anterior; null si es la primera

    public RegistroFisicoDTO(int id, int clienteId, String clienteNombre, LocalDate fecha,
                             BigDecimal peso, BigDecimal altura, BigDecimal variacion) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.fecha = fecha;
        this.peso = peso;
        this.altura = altura;
        this.variacion = variacion;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public BigDecimal getAltura() {
        return altura;
    }

    /** IMC = peso / altura², con un decimal. */
    public BigDecimal getImc() {
        if (altura.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return peso.divide(altura.multiply(altura), 1, RoundingMode.HALF_UP);
    }

    /** Clasificación de la OMS. */
    public String getClasificacion() {
        double imc = getImc().doubleValue();
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25) return "Normal";
        if (imc < 30) return "Sobrepeso";
        return "Obesidad";
    }

    /** "ok", "alerta" o "riesgo": sirve para elegir el color de la etiqueta. */
    public String getNivelImc() {
        String c = getClasificacion();
        if (c.equals("Normal")) return "ok";
        if (c.equals("Obesidad")) return "riesgo";
        return "alerta";
    }

    /** "+1.50 kg", "-0.80 kg", "0.00 kg" o "—" si es la primera medición. */
    public String getVariacionTexto() {
        if (variacion == null) {
            return "—";
        }
        String signo = variacion.signum() > 0 ? "+" : "";
        return signo + variacion.toPlainString() + " kg";
    }
}