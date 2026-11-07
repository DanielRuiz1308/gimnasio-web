package com.gimnasio.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.gimnasio.util.Reloj;

/**
 * DTO: datos de varias tablas (persona + cliente + membresia + tipo_membresia)
 * juntos en un solo objeto, pensado para mostrarse en pantalla.
 * Si el cliente no tiene membresía, tipo y fechas son null.
 */
public class MembresiaClienteDTO {

    private final int clienteId;
    private final String nombre;
    private final String documento;
    private final String tipo;
    private final LocalDate fechaPago;
    private final LocalDate fechaVencimiento;
    /** Una membresía vigente se considera "por vencer" si vence dentro de este número de días. */
    public static final int DIAS_POR_VENCER = 7;


    public MembresiaClienteDTO(int clienteId, String nombre, String documento, String tipo,
                               LocalDate fechaPago, LocalDate fechaVencimiento) {
        this.clienteId = clienteId;
        this.nombre = nombre;
        this.documento = documento;
        this.tipo = tipo;
        this.fechaPago = fechaPago;
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getClienteId() {
        return clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    /** "Vigente", "Vencida" o "Sin membresía". */
    public String getEstado() {
        if (fechaVencimiento == null) {
            return "Sin membresía";
        }
        return fechaVencimiento.isBefore(Reloj.hoy()) ? "Vencida" : "Vigente";
    }

    /** Días que faltan para vencer (negativo si ya venció). null si no tiene membresía. */
    public Long getDiasRestantes() {
        if (fechaVencimiento == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(Reloj.hoy(), fechaVencimiento);
    }
    /** Igual que getEstado(), pero separa las vigentes que vencen pronto: "Por vencer". Solo para mostrar. */
    public String getEstadoVisual() {
        String estado = getEstado();
        if (estado.equals("Vigente") && getDiasRestantes() <= DIAS_POR_VENCER) {
            return "Por vencer";
        }
        return estado;
    }
}

