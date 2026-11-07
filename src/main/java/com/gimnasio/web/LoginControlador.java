package com.gimnasio.web;

import com.gimnasio.dao.UsuarioWebDAO;
import com.gimnasio.dto.UsuarioDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class LoginControlador {

    private static final int SEGUNDOS_DE_INACTIVIDAD = 60 * 60; // la sesión se cierra tras 1 hora sin usar

    private final UsuarioWebDAO usuarioDAO = new UsuarioWebDAO();
    private final IntentosLogin intentos = new IntentosLogin();

    // GET /login -> formulario (si ya hay sesión, al inicio)
    @GetMapping("/login")
    public String formulario(HttpSession sesion) {
        if (sesion.getAttribute("usuario") != null) {
            return "redirect:/";
        }
        return "login";
    }

    // POST /login -> revisar usuario y contraseña
    @PostMapping("/login")
    public String entrar(@RequestParam String usuario, @RequestParam String clave,
                         HttpServletRequest solicitud, Model modelo) {

        String nombre = usuario.trim();
        modelo.addAttribute("usuario", nombre);

        String claveIntentos = nombre.toLowerCase() + "|" + solicitud.getRemoteAddr();
        if (intentos.bloqueado(claveIntentos)) {
            modelo.addAttribute("error", "Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.");
            return "login";
        }

        UsuarioDTO autenticado;
        try {
            autenticado = usuarioDAO.autenticar(nombre, clave);
        } catch (RuntimeException e) {
            modelo.addAttribute("error", "No se pudo consultar la base de datos: " + e.getMessage());
            return "login";
        }

        if (autenticado == null) {
            intentos.fallo(claveIntentos);
            // Mismo mensaje si falla el usuario o la contraseña: no se le dice al atacante cuál de los dos
            modelo.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "login";
        }

        intentos.exito(claveIntentos);

        HttpSession sesion = solicitud.getSession(true);
        solicitud.changeSessionId(); // sesión nueva al iniciar: evita que alguien reutilice un identificador viejo
        sesion.setAttribute("usuario", autenticado);
        sesion.setMaxInactiveInterval(SEGUNDOS_DE_INACTIVIDAD);
        return "redirect:/";
    }

    // POST /logout -> cerrar sesión
    @PostMapping("/logout")
    public String salir(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }

    // GET /acceso-denegado -> el rol no tiene permiso sobre ese módulo
    @GetMapping("/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}