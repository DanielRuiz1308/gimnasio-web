package com.gimnasio.dao;

import com.gimnasio.dto.UsuarioDTO;
import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Consulta de usuarios para el inicio de sesión. */
public class UsuarioWebDAO {

    /**
     * Devuelve el usuario si el nombre y la contraseña son correctos; si no, null.
     * Mismo esquema que el script SQL: password_hash = SHA-256(salt + contraseña) en hexadecimal.
     */
    public UsuarioDTO autenticar(String nombreUsuario, String clave) {
        String sql = "SELECT id, nombre_completo, nombre_usuario, salt, password_hash, rol, entrenador_id " +
                "FROM usuario WHERE nombre_usuario = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String esperado = rs.getString("password_hash");
                String calculado = hash(rs.getString("salt"), clave);
                if (!iguales(esperado, calculado)) {
                    return null;
                }
                int entrenador = rs.getInt("entrenador_id");
                Integer entrenadorId = rs.wasNull() ? null : entrenador;
                return new UsuarioDTO(rs.getInt("id"), rs.getString("nombre_completo"),
                        rs.getString("nombre_usuario"), rs.getString("rol"), entrenadorId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    static String hash(String salt, String clave) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] bytes = sha.digest((salt + clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    // Comparación que tarda lo mismo acierte o falle (evita filtrar información por el tiempo)
    private boolean iguales(String a, String b) {
        return MessageDigest.isEqual(
                a.toLowerCase().getBytes(StandardCharsets.UTF_8),
                b.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}