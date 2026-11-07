package com.gimnasio.dao;

import com.gimnasio.dto.AsistenciaDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;
import com.gimnasio.util.Reloj;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Asistencias con JOIN (nombres de cliente y entrenador) y registro automático de fecha, hora y turno. */
public class AsistenciaWebDAO {

    /** Las asistencias más recientes primero. */
    public List<AsistenciaDTO> listarRecientes(int limite) {
        String sql = "SELECT a.id, a.cliente_id, pc.nombre AS cliente, pe.nombre AS entrenador, " +
                "a.fecha, a.hora, a.turno " +
                "FROM asistencia a " +
                "JOIN persona pc ON pc.id = a.cliente_id " +
                "JOIN persona pe ON pe.id = a.entrenador_id " +
                "ORDER BY a.fecha DESC, a.hora DESC, a.id DESC LIMIT ?";
        List<AsistenciaDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new AsistenciaDTO(
                            rs.getInt("id"),
                            rs.getInt("cliente_id"),
                            rs.getString("cliente"),
                            rs.getString("entrenador"),
                            rs.getDate("fecha").toLocalDate(),
                            rs.getTime("hora").toLocalTime(),
                            rs.getString("turno")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    /** Registra la asistencia de ahora mismo. El turno se calcula con la hora (antes de las 12 = mañana). */
    public void registrar(int clienteId, int entrenadorId) {
        LocalDateTime ahora = Reloj.ahora();
        String turno = ahora.getHour() < 12 ? "mañana" : "noche";

        String sql = "INSERT INTO asistencia (cliente_id, entrenador_id, fecha, hora, turno) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, entrenadorId);
            ps.setDate(3, Date.valueOf(ahora.toLocalDate()));
            ps.setTime(4, Time.valueOf(ahora.toLocalTime().withNano(0)));
            ps.setString(5, turno);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public void eliminar(int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM asistencia WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }
}