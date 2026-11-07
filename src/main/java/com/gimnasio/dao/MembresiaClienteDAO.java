package com.gimnasio.dao;

import com.gimnasio.dto.MembresiaClienteDTO;
import com.gimnasio.dto.TipoMembresiaDTO;
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
import com.gimnasio.util.Reloj;


/**
 * Consultas de membresías que unen varias tablas con JOIN y devuelven DTOs,
 * más la operación de renovar (que toca dos tablas dentro de una transacción).
 */
public class MembresiaClienteDAO {

    private static final String SELECT_BASE =
            "SELECT c.id, p.nombre, p.documento, tm.nombre AS tipo, m.fecha_pago, m.fecha_vencimiento " +
                    "FROM cliente c " +
                    "JOIN persona p ON p.id = c.id " +
                    "LEFT JOIN membresia m ON m.cliente_id = c.id " +
                    "LEFT JOIN tipo_membresia tm ON tm.id = m.tipo_membresia_id ";

    /** Todos los clientes, con o sin membresía. */
    public List<MembresiaClienteDTO> listarTodos() {
        List<MembresiaClienteDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "ORDER BY p.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(armar(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    /** Un solo cliente, o null si no existe. */
    public MembresiaClienteDTO buscarPorClienteId(int clienteId) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE c.id = ?")) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? armar(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public List<TipoMembresiaDTO> listarTipos() {
        List<TipoMembresiaDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre, precio FROM tipo_membresia ORDER BY precio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new TipoMembresiaDTO(rs.getInt("id"), rs.getString("nombre"), rs.getBigDecimal("precio")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    /**
     * Renueva la membresía del cliente y registra el pago. Las dos cosas pasan juntas
     * o no pasa ninguna (transacción).
     */
    public void renovar(int clienteId, int tipoId, String metodoPago) {
        if (!List.of("efectivo", "transferencia", "tarjeta").contains(metodoPago)) {
            throw new RuntimeException("El método de pago no es válido.");
        }

        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                String nombreTipo;
                BigDecimal precio;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT nombre, precio FROM tipo_membresia WHERE id = ?")) {
                    ps.setInt(1, tipoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new RuntimeException("El tipo de membresía no existe.");
                        }
                        nombreTipo = rs.getString("nombre");
                        precio = rs.getBigDecimal("precio");
                    }
                }

                LocalDate hoy = Reloj.hoy();
                LocalDate vence = hoy.plusDays(diasDe(nombreTipo));

                String upsert = "INSERT INTO membresia (cliente_id, tipo_membresia_id, fecha_pago, fecha_vencimiento) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE tipo_membresia_id = VALUES(tipo_membresia_id), " +
                        "fecha_pago = VALUES(fecha_pago), fecha_vencimiento = VALUES(fecha_vencimiento)";
                try (PreparedStatement ps = con.prepareStatement(upsert)) {
                    ps.setInt(1, clienteId);
                    ps.setInt(2, tipoId);
                    ps.setDate(3, Date.valueOf(hoy));
                    ps.setDate(4, Date.valueOf(vence));
                    ps.executeUpdate();
                }

                String pago = "INSERT INTO pago (cliente_id, tipo_membresia_id, fecha_pago, monto, metodo_pago) " +
                        "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(pago)) {
                    ps.setInt(1, clienteId);
                    ps.setInt(2, tipoId);
                    ps.setDate(3, Date.valueOf(hoy));
                    ps.setBigDecimal(4, precio);
                    ps.setString(5, metodoPago);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    // Mismo criterio que en la versión de escritorio
    private int diasDe(String nombreTipo) {
        String nombre = nombreTipo.toLowerCase();
        if (nombre.contains("trimestral")) return 90;
        if (nombre.contains("anual")) return 365;
        return 30;
    }

    private MembresiaClienteDTO armar(ResultSet rs) throws SQLException {
        Date pago = rs.getDate("fecha_pago");
        Date vence = rs.getDate("fecha_vencimiento");
        return new MembresiaClienteDTO(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("documento"),
                rs.getString("tipo"),
                pago == null ? null : pago.toLocalDate(),
                vence == null ? null : vence.toLocalDate());
    }
}