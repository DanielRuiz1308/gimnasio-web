package com.gimnasio.dao;

import com.gimnasio.util.ConexionBD;
import com.gimnasio.util.ErrorBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Guarda y lee la foto de una persona (cliente, entrenador o administrador).
 * La foto vive en la tabla persona (columnas foto y foto_tipo).
 */
public class FotoDAO {

    /** Foto lista para enviar al navegador. */
    public static class Foto {
        public final byte[] datos;
        public final String tipo;

        public Foto(byte[] datos, String tipo) {
            this.datos = datos;
            this.tipo = tipo;
        }
    }

    public void guardar(int idPersona, byte[] datos, String tipo) {
        ejecutar("UPDATE persona SET foto = ?, foto_tipo = ? WHERE id = ?", datos, tipo, idPersona);
    }

    /** Sirve para un cliente recién creado: el documento es único, así que lo identifica. */
    public void guardarPorDocumento(String documento, byte[] datos, String tipo) {
        String sql = "UPDATE persona SET foto = ?, foto_tipo = ? WHERE documento = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBytes(1, datos);
            ps.setString(2, tipo);
            ps.setString(3, documento);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public void quitar(int idPersona) {
        ejecutar("UPDATE persona SET foto = ?, foto_tipo = ? WHERE id = ?", null, null, idPersona);
    }

    /** Devuelve la foto o null si la persona no tiene. */
    public Foto obtener(int idPersona) {
        String sql = "SELECT foto, foto_tipo FROM persona WHERE id = ? AND foto IS NOT NULL";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Foto(rs.getBytes("foto"), rs.getString("foto_tipo"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    public boolean existe(int idPersona) {
        String sql = "SELECT 1 FROM persona WHERE id = ? AND foto IS NOT NULL";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }

    private void ejecutar(String sql, byte[] datos, String tipo, int id) {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (datos == null) {
                ps.setNull(1, Types.BLOB);
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setBytes(1, datos);
                ps.setString(2, tipo);
            }
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(ErrorBD.mensaje(e), e);
        }
    }
}