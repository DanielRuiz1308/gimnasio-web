package com.gimnasio.dto;

import com.gimnasio.util.Permisos;

import java.io.Serializable;

/**
 * El usuario que inició sesión. Es un DTO a propósito: NO trae la contraseña, ni el hash,
 * ni el salt; solo lo que la aplicación necesita saber de él.
 */
public class UsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombreCompleto;
    private final String nombreUsuario;
    private final String rol;           // admin, recepcion o entrenador
    private final Integer entrenadorId; // solo si el rol es entrenador

    public UsuarioDTO(int id, String nombreCompleto, String nombreUsuario, String rol, Integer entrenadorId) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.entrenadorId = entrenadorId;
    }

    public int getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public Integer getEntrenadorId() {
        return entrenadorId;
    }

    /** Lo usan las plantillas: ${session.usuario.puede('reportes')} */
    public boolean puede(String modulo) {
        return Permisos.permite(rol, modulo);
    }

    public boolean isAdmin() {
        return "admin".equals(rol);
    }

    /** "Administrador", "Recepción" o "Entrenador", para mostrar. */
    public String getRolTexto() {
        switch (rol) {
            case "admin": return "Administrador";
            case "recepcion": return "Recepción";
            case "entrenador": return "Entrenador";
            default: return rol;
        }
    }
}