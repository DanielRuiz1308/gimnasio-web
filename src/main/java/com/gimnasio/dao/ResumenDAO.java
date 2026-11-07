package com.gimnasio.dao;

import com.gimnasio.dto.MembresiaClienteDTO;
import com.gimnasio.dto.ResumenDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;
import com.gimnasio.util.Reloj;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas de resumen para la pantalla de inicio. */
public class ResumenDAO {

    /** Una membresía "por vencer" es la que vence dentro de este número de días. */
    public static final int DIAS_POR_VENCER = MembresiaClienteDTO.DIAS_POR_VENCER;

    public ResumenDTO generar() {
        LocalDate hoy = Reloj.hoy();
        LocalDate limite = hoy.plusDays(DIAS_POR_VENCER);
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        try (Connection con = ConexionBD.getConnection()) {
            int asistenciasHoy = contar(con, "SELECT COUNT(*) FROM asistencia WHERE fecha = ?", hoy);
            // vigentes = vencen después del límite; porVencer = entre hoy y el límite; vencidas = antes de hoy
            int vigentes = contar(con, "SELECT COUNT(*) FROM membresia WHERE fecha_vencimiento > ?", limite);
            int porVencer = contar(con,
                    "SELECT COUNT(*) FROM membresia WHERE fecha_vencimiento >= ? AND fecha_vencimiento <= ?",
                    hoy, limite);
            int vencidas = contar(con, "SELECT COUNT(*) FROM membresia WHERE fecha_vencimiento < ?", hoy);

            BigDecimal ingresos = BigDecimal.ZERO;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE fecha_pago >= ? AND fecha_pago <= ?")) {
                ps.setDate(1, Date.valueOf(inicioMes));
                ps.setDate(2, Date.valueOf(hoy));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ingresos = rs.getBigDecimal(1);
                }
            }

            return new ResumenDTO(asistenciasHoy, vigentes, porVencer, vencidas, ingresos,
                    proximas(con, hoy, limite));
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    // Los que vencen pronto (o ya vencieron en la última semana), los más urgentes primero
    private List<MembresiaClienteDTO> proximas(Connection con, LocalDate hoy, LocalDate limite) throws SQLException {
        String sql = "SELECT c.id, p.nombre, p.documento, tm.nombre AS tipo, m.fecha_pago, m.fecha_vencimiento " +
                "FROM membresia m " +
                "JOIN cliente c ON c.id = m.cliente_id " +
                "JOIN persona p ON p.id = c.id " +
                "JOIN tipo_membresia tm ON tm.id = m.tipo_membresia_id " +
                "WHERE m.fecha_vencimiento >= ? AND m.fecha_vencimiento <= ? " +
                "ORDER BY m.fecha_vencimiento, p.nombre LIMIT 8";
        List<MembresiaClienteDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(hoy.minusDays(7)));
            ps.setDate(2, Date.valueOf(limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MembresiaClienteDTO(
                            rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"),
                            rs.getString("tipo"),
                            rs.getDate("fecha_pago").toLocalDate(),
                            rs.getDate("fecha_vencimiento").toLocalDate()));
                }
            }
        }
        return lista;
    }

    private int contar(Connection con, String sql, LocalDate... fechas) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < fechas.length; i++) {
                ps.setDate(i + 1, Date.valueOf(fechas[i]));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}