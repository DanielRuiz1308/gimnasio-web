package com.gimnasio.util;

import java.util.Map;
import java.util.Set;

/**
 * Qué módulos puede usar cada rol. Para cambiar los permisos, solo se edita POR_ROL.
 * Los nombres de módulo son los mismos que usa el menú: clientes, entrenadores, administradores,
 * membresias, rutinas, asistencia, registro, reportes, usuarios.
 */
public final class Permisos {

    private static final Map<String, Set<String>> POR_ROL = Map.of(
            "admin", Set.of("clientes", "entrenadores", "administradores", "membresias",
                    "rutinas", "asistencia", "registro", "reportes", "usuarios"),
            "recepcion", Set.of("clientes", "membresias", "asistencia", "registro"),
            "entrenador", Set.of("asistencia", "rutinas", "registro")
    );

    private Permisos() {
    }

    public static boolean permite(String rol, String modulo) {
        return POR_ROL.getOrDefault(rol, Set.of()).contains(modulo);
    }

    /**
     * Módulo al que pertenece una ruta ("/clientes/5/editar" -> "clientes").
     * Devuelve null si la ruta no es de ningún módulo (por ejemplo "/" o "/acceso-denegado").
     */
    public static String moduloDe(String ruta) {
        String[] partes = ruta.split("/");
        if (partes.length < 2) {
            return null;
        }
        switch (partes[1]) {
            case "clientes": return "clientes";
            case "entrenadores": return "entrenadores";
            case "administradores": return "administradores";
            case "membresias": return "membresias";
            case "rutinas": return "rutinas";
            case "asistencia": return "asistencia";
            case "registro-fisico": return "registro";
            case "reportes": return "reportes";
            case "usuarios": return "usuarios";
            default: return null;
        }
    }
}