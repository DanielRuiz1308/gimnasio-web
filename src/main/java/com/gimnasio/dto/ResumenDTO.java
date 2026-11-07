package com.gimnasio.dto;

import java.math.BigDecimal;
import java.util.List;

/** Números del día para la pantalla de inicio. */
public class ResumenDTO {

    private final int asistenciasHoy;
    private final int vigentes;
    private final int porVencer;
    private final int vencidas;
    private final BigDecimal ingresosMes;
    private final List<MembresiaClienteDTO> proximasAVencer;

    public ResumenDTO(int asistenciasHoy, int vigentes, int porVencer, int vencidas,
                      BigDecimal ingresosMes, List<MembresiaClienteDTO> proximasAVencer) {
        this.asistenciasHoy = asistenciasHoy;
        this.vigentes = vigentes;
        this.porVencer = porVencer;
        this.vencidas = vencidas;
        this.ingresosMes = ingresosMes;
        this.proximasAVencer = proximasAVencer;
    }

    public int getAsistenciasHoy() { return asistenciasHoy; }
    public int getVigentes() { return vigentes; }
    public int getPorVencer() { return porVencer; }
    public int getVencidas() { return vencidas; }
    public BigDecimal getIngresosMes() { return ingresosMes; }
    public List<MembresiaClienteDTO> getProximasAVencer() { return proximasAVencer; }
}