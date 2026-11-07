package com.gimnasio.dao;

import com.gimnasio.dto.EjercicioDTO;
import com.gimnasio.dto.RutinaDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Rutinas con sus ejercicios. Guardar una rutina toca dos tablas (rutina y ejercicio),
 * por eso crear y actualizar usan una transacción: o se guarda todo o no se guarda nada.
 */
public class RutinaWebDAO {

    private static final String SELECT_BASE =
            "SELECT r.id, r.cliente_id, pc.nombre AS cliente, r.entrenador_id, pe.nombre AS entrenador, " +
                    "r.nombre, r.nivel, r.objetivo, " +
                    "(SELECT COUNT(*) FROM ejercicio e WHERE e.rutina_id = r.id) AS total " +
                    "FROM rutina r " +
                    "JOIN persona pc ON pc.id = r.cliente_id " +
                    "JOIN persona pe ON pe.id = r.entrenador_id ";

    /** Todas las rutinas, sin el detalle de ejercicios (solo la cantidad). */
    public List<RutinaDTO> listarTodas() {
        List<RutinaDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "ORDER BY pc.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(armar(rs, new ArrayList<>()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    /** Una rutina con todos sus ejercicios, o null si no existe. */
    public RutinaDTO buscarPorId(int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE r.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return armar(rs, ejerciciosDe(con, id));
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** Crea la rutina con sus ejercicios. Devuelve el id nuevo. */
    public int crear(int clienteId, int entrenadorId, String nombre, String nivel, String objetivo,
                     List<EjercicioDTO> ejercicios) {
        String sql = "INSERT INTO rutina (cliente_id, entrenador_id, nombre, nivel, objetivo) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, clienteId);
                    ps.setInt(2, entrenadorId);
                    ps.setString(3, nombre);
                    ps.setString(4, vacioANull(nivel));
                    ps.setString(5, vacioANull(objetivo));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                insertarEjercicios(con, id, ejercicios);
                con.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** Actualiza la rutina y reemplaza toda su lista de ejercicios. El cliente no cambia. */
    public void actualizar(int id, int entrenadorId, String nombre, String nivel, String objetivo,
                           List<EjercicioDTO> ejercicios) {
        String sql = "UPDATE rutina SET entrenador_id = ?, nombre = ?, nivel = ?, objetivo = ? WHERE id = ?";
        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, entrenadorId);
                    ps.setString(2, nombre);
                    ps.setString(3, vacioANull(nivel));
                    ps.setString(4, vacioANull(objetivo));
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM ejercicio WHERE rutina_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                insertarEjercicios(con, id, ejercicios);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** Borra la rutina; sus ejercicios se borran solos (ON DELETE CASCADE). */
    public void eliminar(int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM rutina WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    private void insertarEjercicios(Connection con, int rutinaId, List<EjercicioDTO> ejercicios) throws SQLException {
        String sql = "INSERT INTO ejercicio (rutina_id, nombre, series, repeticiones, descanso) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (EjercicioDTO e : ejercicios) {
                ps.setInt(1, rutinaId);
                ps.setString(2, e.getNombre());
                ps.setInt(3, e.getSeries());
                ps.setInt(4, e.getRepeticiones());
                if (e.getDescanso() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, e.getDescanso());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<EjercicioDTO> ejerciciosDe(Connection con, int rutinaId) throws SQLException {
        List<EjercicioDTO> lista = new ArrayList<>();
        String sql = "SELECT nombre, series, repeticiones, descanso FROM ejercicio WHERE rutina_id = ? ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rutinaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int descanso = rs.getInt("descanso");
                    Integer descansoOpcional = rs.wasNull() ? null : descanso;
                    lista.add(new EjercicioDTO(rs.getString("nombre"), rs.getInt("series"),
                            rs.getInt("repeticiones"), descansoOpcional));
                }
            }
        }
        return lista;
    }

    private RutinaDTO armar(ResultSet rs, List<EjercicioDTO> ejercicios) throws SQLException {
        return new RutinaDTO(
                rs.getInt("id"),
                rs.getInt("cliente_id"),
                rs.getString("cliente"),
                rs.getInt("entrenador_id"),
                rs.getString("entrenador"),
                rs.getString("nombre"),
                rs.getString("nivel"),
                rs.getString("objetivo"),
                rs.getInt("total"),
                ejercicios);
    }

    private String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}