package com.gimnasio.web;

import com.gimnasio.dto.UsuarioDTO;
import com.gimnasio.util.Permisos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Se ejecuta ANTES de cada petición: si no hay sesión, manda al login; si el rol no tiene
 * permiso sobre el módulo, manda a "acceso denegado".
 */
public class AutenticacionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest solicitud, HttpServletResponse respuesta, Object manejador)
            throws Exception {

        String contexto = solicitud.getContextPath();
        String ruta = solicitud.getRequestURI().substring(contexto.length());

        HttpSession sesion = solicitud.getSession(false);
        UsuarioDTO usuario = sesion == null ? null : (UsuarioDTO) sesion.getAttribute("usuario");

        if (usuario == null) {
            respuesta.sendRedirect(contexto + "/login");
            return false;
        }

        // Las fotos se ven en varias pantallas, aunque el rol no tenga el módulo de esa persona
        if (esFotoDePersona(ruta)) {
            return true;
        }

        // Páginas con datos: que el navegador no las guarde (así "Atrás" no las muestra tras cerrar sesión)
        respuesta.setHeader("Cache-Control", "no-store");

        String modulo = Permisos.moduloDe(ruta);
        if (modulo != null && !usuario.puede(modulo)) {
            respuesta.sendRedirect(contexto + "/acceso-denegado");
            return false;
        }
        return true;
    }

    private boolean esFotoDePersona(String ruta) {
        return ruta.matches("/(clientes|entrenadores|administradores)/\\d+/foto");
    }
}