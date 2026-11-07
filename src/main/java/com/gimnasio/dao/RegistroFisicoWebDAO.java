package com.gimnasio.dao;

import com.gimnasio.dto.RegistroFisicoDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mediciones físicas con JOIN al nombre del cliente. La variación de peso se calcula en SQL
 * con LAG (función de ventana, MySQL 8+): toma el peso de la fila anterior del mismo cliente.
 */
public class RegistroFisicoWebDAO {

    // {FILTRO} se reemplaza por "WHERE r.cliente_id = ?" o por nada
    private static final String SELECT_BASE =
            "SELECT * FROM (" +
                    "  SELECT r.id, r.cliente_id, p.nombre, r.fecha, r.peso, r.altura, " +
                    "         r.peso - LAG(r.peso) OVER (PARTITION BY r.cliente_id ORDER BY r.fecha, r.id) AS variacion " +
                    "  FROM registro_fisico r " +
                    "  JOIN persona p ON p.id = r.cliente_id " +
                    "  {FILTRO}" +
                    ") t ORDER BY fecha DESC, id DESC ";

    /** Las mediciones más recientes de todos los clientes. */
    public List<RegistroFisicoDTO> listarRecientes(int limite) {
        String sql = SELECT_BASE.replace("{FILTRO}", "") + "LIMIT ?";
        List<RegistroFisicoDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(armar(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    /** Todo el historial de un cliente, de la más reciente a la más antigua. */
    public List<RegistroFisicoDTO> listarPorCliente(int clienteId) {
        String sql = SELECT_BASE.replace("{FILTRO}", "WHERE r.cliente_id = ? ");
        List<RegistroFisicoDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(armar(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    public void registrar(int clienteId, LocalDate fecha, BigDecimal peso, BigDecimal altura) {
        String sql = "INSERT INTO registro_fisico (cliente_id, fecha, peso, altura) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setBigDecimal(3, peso);
            ps.setBigDecimal(4, altura);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** Devuelve el id del cliente al que pertenecía el registro (0 si no existía). */
    public int eliminar(int id) {
        try (Connection con = ConexionBD.getConnection()) {
            int clienteId = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT cliente_id FROM registro_fisico WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        clienteId = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM registro_fisico WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return clienteId;
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    private RegistroFisicoDTO armar(ResultSet rs) throws SQLException {
        return new RegistroFisicoDTO(
                rs.getInt("id"),
                rs.getInt("cliente_id"),
                rs.getString("nombre"),
                rs.getDate("fecha").toLocalDate(),
                rs.getBigDecimal("peso"),
                rs.getBigDecimal("altura"),
                rs.getBigDecimal("variacion"));
    }
}