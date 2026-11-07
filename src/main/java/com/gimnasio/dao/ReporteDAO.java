package com.gimnasio.dao;

import com.gimnasio.dto.ReporteDTO;
import com.gimnasio.dto.SerieDTO;
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
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Consultas de resumen (COUNT, SUM, GROUP BY) para la pantalla de reportes. */
public class ReporteDAO {

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    public ReporteDTO generar() {
        LocalDate hoy = Reloj.hoy();
        try (Connection con = ConexionBD.getConnection()) {
            int total = contar(con, "SELECT COUNT(*) FROM cliente");
            int vigentes = contar(con, "SELECT COUNT(*) FROM membresia WHERE fecha_vencimiento >= ?", hoy);
            int vencidas = contar(con, "SELECT COUNT(*) FROM membresia WHERE fecha_vencimiento < ?", hoy);
            int sin = Math.max(0, total - vigentes - vencidas);

            BigDecimal ingresosMes = sumar(con,
                    "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE fecha_pago >= ? AND fecha_pago <= ?",
                    hoy.withDayOfMonth(1), hoy);

            return new ReporteDTO(total, vigentes, vencidas, sin, ingresosMes,
                    ingresosPorMes(con, hoy),
                    clientesPorGenero(con),
                    clientesPorEdad(con, hoy),
                    asistenciasPorTurno(con));
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    // Ingresos de los últimos 12 meses; los meses sin pagos salen en 0
    private SerieDTO ingresosPorMes(Connection con, LocalDate hoy) throws SQLException {
        YearMonth ultimo = YearMonth.from(hoy);
        YearMonth primero = ultimo.minusMonths(11);

        String sql = "SELECT DATE_FORMAT(fecha_pago, '%Y-%m') AS mes, SUM(monto) AS total " +
                "FROM pago WHERE fecha_pago >= ? GROUP BY mes";
        Map<String, BigDecimal> porMes = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(primero.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porMes.put(rs.getString("mes"), rs.getBigDecimal("total"));
                }
            }
        }

        SerieDTO serie = new SerieDTO();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            String etiqueta = mes.getMonth().getDisplayName(TextStyle.SHORT, ES) + " " + mes.getYear();
            serie.agregar(etiqueta, porMes.getOrDefault(mes.toString(), BigDecimal.ZERO));
        }
        return serie;
    }

    private SerieDTO clientesPorGenero(Connection con) throws SQLException {
        String sql = "SELECT p.genero, COUNT(*) AS cantidad FROM cliente c " +
                "JOIN persona p ON p.id = c.id GROUP BY p.genero";
        Map<String, Integer> cuenta = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cuenta.put(rs.getString("genero"), rs.getInt("cantidad"));
            }
        }
        return new SerieDTO()
                .agregar("Masculino", cuenta.getOrDefault("M", 0))
                .agregar("Femenino", cuenta.getOrDefault("F", 0))
                .agregar("Otro", cuenta.getOrDefault("Otro", 0));
    }

    // Se trae la edad de cada cliente y se reparte en rangos aquí
    private SerieDTO clientesPorEdad(Connection con, LocalDate hoy) throws SQLException {
        String sql = "SELECT TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, ?) AS edad FROM cliente c " +
                "JOIN persona p ON p.id = c.id";
        String[] rangos = {"Menos de 18", "18 a 25", "26 a 35", "36 a 45", "46 a 60", "Más de 60"};
        int[] cuenta = new int[rangos.length];

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(hoy));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cuenta[indiceRango(rs.getInt("edad"))]++;
                }
            }
        }

        SerieDTO serie = new SerieDTO();
        for (int i = 0; i < rangos.length; i++) {
            serie.agregar(rangos[i], cuenta[i]);
        }
        return serie;
    }

    private int indiceRango(int edad) {
        if (edad < 18) return 0;
        if (edad <= 25) return 1;
        if (edad <= 35) return 2;
        if (edad <= 45) return 3;
        if (edad <= 60) return 4;
        return 5;
    }

    private SerieDTO asistenciasPorTurno(Connection con) throws SQLException {
        Map<String, Integer> cuenta = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT turno, COUNT(*) AS cantidad FROM asistencia GROUP BY turno");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cuenta.put(rs.getString("turno"), rs.getInt("cantidad"));
            }
        }
        return new SerieDTO()
                .agregar("Mañana", cuenta.getOrDefault("mañana", 0))
                .agregar("Noche", cuenta.getOrDefault("noche", 0));
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

    private BigDecimal sumar(Connection con, String sql, LocalDate... fechas) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < fechas.length; i++) {
                ps.setDate(i + 1, Date.valueOf(fechas[i]));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}