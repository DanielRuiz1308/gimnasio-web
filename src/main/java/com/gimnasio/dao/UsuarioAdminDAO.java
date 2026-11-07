package com.gimnasio.dao;

import com.gimnasio.dto.UsuarioListadoDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UsuarioAdminDAO {

    private static final String SELECT_BASE =
            "SELECT u.id, u.nombre_completo, u.nombre_usuario, u.rol, u.entrenador_id, pe.nombre AS entrenador " +
                    "FROM usuario u LEFT JOIN persona pe ON pe.id = u.entrenador_id ";

    private final SecureRandom azar = new SecureRandom();

    public List<UsuarioListadoDTO> listar() {
        List<UsuarioListadoDTO> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "ORDER BY u.nombre_completo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(armar(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
        return lista;
    }

    public UsuarioListadoDTO buscarPorId(int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE u.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? armar(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** ¿Ya hay otro usuario con ese nombre? excluirId es el que se edita (0 si es nuevo). */
    public boolean existeNombreUsuario(String nombreUsuario, int excluirId) {
        String sql = "SELECT 1 FROM usuario WHERE nombre_usuario = ? AND id <> ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public int contarAdministradores() {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM usuario WHERE rol = 'admin'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public void crear(String nombreCompleto, String nombreUsuario, String clave, String rol, Integer entrenadorId) {
        String sql = "INSERT INTO usuario (nombre_completo, nombre_usuario, salt, password_hash, rol, entrenador_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String salt = nuevoSalt();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, nombreUsuario);
            ps.setString(3, salt);
            ps.setString(4, UsuarioWebDAO.hash(salt, clave));
            ps.setString(5, rol);
            asignarEntrenador(ps, 6, entrenadorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public void actualizar(int id, String nombreCompleto, String nombreUsuario, String rol, Integer entrenadorId) {
        String sql = "UPDATE usuario SET nombre_completo = ?, nombre_usuario = ?, rol = ?, entrenador_id = ? WHERE id = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, nombreUsuario);
            ps.setString(3, rol);
            asignarEntrenador(ps, 4, entrenadorId);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    /** Contraseña nueva, con salt nuevo. */
    public void cambiarClave(int id, String clave) {
        String salt = nuevoSalt();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE usuario SET salt = ?, password_hash = ? WHERE id = ?")) {
            ps.setString(1, salt);
            ps.setString(2, UsuarioWebDAO.hash(salt, clave));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public void eliminar(int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    // 16 bytes al azar -> 32 caracteres hex (cabe justo en VARCHAR(32))
    private String nuevoSalt() {
        byte[] bytes = new byte[16];
        azar.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private void asignarEntrenador(PreparedStatement ps, int posicion, Integer entrenadorId) throws SQLException {
        if (entrenadorId == null) {
            ps.setNull(posicion, Types.INTEGER);
        } else {
            ps.setInt(posicion, entrenadorId);
        }
    }

    private UsuarioListadoDTO armar(ResultSet rs) throws SQLException {
        int entrenador = rs.getInt("entrenador_id");
        Integer entrenadorId = rs.wasNull() ? null : entrenador;
        return new UsuarioListadoDTO(rs.getInt("id"), rs.getString("nombre_completo"),
                rs.getString("nombre_usuario"), rs.getString("rol"), entrenadorId, rs.getString("entrenador"));
    }
}