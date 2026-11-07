package com.gimnasio.dto;

/** Un usuario tal como se muestra en la lista de administración (sin contraseña, hash ni salt). */
public class UsuarioListadoDTO {

    private final int id;
    private final String nombreCompleto;
    private final String nombreUsuario;
    private final String rol;
    private final Integer entrenadorId;
    private final String entrenadorNombre; // null si el usuario no es un entrenador

    public UsuarioListadoDTO(int id, String nombreCompleto, String nombreUsuario, String rol,
                             Integer entrenadorId, String entrenadorNombre) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.entrenadorId = entrenadorId;
        this.entrenadorNombre = entrenadorNombre;
    }

    public int getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getRol() { return rol; }
    public Integer getEntrenadorId() { return entrenadorId; }
    public String getEntrenadorNombre() { return entrenadorNombre; }

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