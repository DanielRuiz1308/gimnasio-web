package com.gimnasio.web;

import com.gimnasio.dao.EntrenadorDAO;
import com.gimnasio.dao.UsuarioAdminDAO;
import com.gimnasio.dao.impl.EntrenadorDAOImpl;
import com.gimnasio.dto.UsuarioDTO;
import com.gimnasio.dto.UsuarioListadoDTO;
import com.gimnasio.modelo.Entrenador;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/usuarios")
public class UsuarioWebControlador {

    private static final Set<String> ROLES = Set.of("admin", "recepcion", "entrenador");
    private static final Pattern NOMBRE_USUARIO = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final int LARGO_MINIMO_CLAVE = 8;

    private final UsuarioAdminDAO usuarioDAO = new UsuarioAdminDAO();
    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAOImpl();

    // GET /usuarios -> lista
    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("usuarios", usuarioDAO.listar());
        return "usuarios";
    }

    // GET /usuarios/nuevo -> formulario vacío
    @GetMapping("/nuevo")
    public String formularioNuevo(Model modelo) {
        modelo.addAttribute("rol", "recepcion");
        cargarEntrenadores(modelo, null);
        return "usuario-form";
    }

    // GET /usuarios/5/editar
    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        UsuarioListadoDTO u = usuarioDAO.buscarPorId(id);
        if (u == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el usuario.");
            return "redirect:/usuarios";
        }
        modelo.addAttribute("id", u.getId());
        modelo.addAttribute("nombreCompleto", u.getNombreCompleto());
        modelo.addAttribute("nombreUsuario", u.getNombreUsuario());
        modelo.addAttribute("rol", u.getRol());
        modelo.addAttribute("entrenadorId", u.getEntrenadorId());
        cargarEntrenadores(modelo, id);
        return "usuario-form";
    }

    // POST /usuarios -> crear
    @PostMapping
    public String crear(@RequestParam String nombreCompleto,
                        @RequestParam String nombreUsuario,
                        @RequestParam String rol,
                        @RequestParam(required = false) Integer entrenadorId,
                        @RequestParam String clave,
                        @RequestParam String confirmacion,
                        Model modelo, RedirectAttributes flash) {

        Integer entrenador = "entrenador".equals(rol) ? entrenadorId : null;

        String error = validarDatos(null, nombreCompleto, nombreUsuario, rol, entrenador);
        if (error == null) {
            error = validarClave(clave, confirmacion);
        }
        if (error != null) {
            return volverAlFormulario(modelo, null, nombreCompleto, nombreUsuario, rol, entrenador, error);
        }

        try {
            usuarioDAO.crear(nombreCompleto.trim(), nombreUsuario.trim(), clave, rol, entrenador);
        } catch (RuntimeException e) {
            return volverAlFormulario(modelo, null, nombreCompleto, nombreUsuario, rol, entrenador, e.getMessage());
        }
        flash.addFlashAttribute("mensajeOk", "Usuario creado.");
        return "redirect:/usuarios";
    }

    // POST /usuarios/5 -> actualizar (sin tocar la contraseña)
    @PostMapping("/{id}")
    public String actualizar(@PathVariable int id,
                             @RequestParam String nombreCompleto,
                             @RequestParam String nombreUsuario,
                             @RequestParam String rol,
                             @RequestParam(required = false) Integer entrenadorId,
                             HttpSession sesion, Model modelo, RedirectAttributes flash) {

        UsuarioListadoDTO actual = usuarioDAO.buscarPorId(id);
        if (actual == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el usuario.");
            return "redirect:/usuarios";
        }

        Integer entrenador = "entrenador".equals(rol) ? entrenadorId : null;
        UsuarioDTO quienEdita = (UsuarioDTO) sesion.getAttribute("usuario");

        String error = validarDatos(id, nombreCompleto, nombreUsuario, rol, entrenador);
        if (error == null && quienEdita.getId() == id && !rol.equals(actual.getRol())) {
            error = "No puedes cambiar tu propio rol (otro administrador puede hacerlo).";
        }
        if (error == null && actual.getRol().equals("admin") && !rol.equals("admin")
                && usuarioDAO.contarAdministradores() <= 1) {
            error = "Debe quedar al menos un administrador.";
        }
        if (error != null) {
            return volverAlFormulario(modelo, id, nombreCompleto, nombreUsuario, rol, entrenador, error);
        }

        try {
            usuarioDAO.actualizar(id, nombreCompleto.trim(), nombreUsuario.trim(), rol, entrenador);
        } catch (RuntimeException e) {
            return volverAlFormulario(modelo, id, nombreCompleto, nombreUsuario, rol, entrenador, e.getMessage());
        }

        // Si se editó a sí mismo, la sesión debe reflejar el nombre nuevo
        if (quienEdita.getId() == id) {
            sesion.setAttribute("usuario",
                    new UsuarioDTO(id, nombreCompleto.trim(), nombreUsuario.trim(), rol, entrenador));
        }
        flash.addFlashAttribute("mensajeOk", "Usuario actualizado.");
        return "redirect:/usuarios";
    }

    // GET /usuarios/5/clave -> formulario de contraseña nueva
    @GetMapping("/{id}/clave")
    public String formularioClave(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        UsuarioListadoDTO u = usuarioDAO.buscarPorId(id);
        if (u == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el usuario.");
            return "redirect:/usuarios";
        }
        modelo.addAttribute("usuario", u);
        return "usuario-clave";
    }

    // POST /usuarios/5/clave -> guardar la contraseña nueva
    @PostMapping("/{id}/clave")
    public String cambiarClave(@PathVariable int id,
                               @RequestParam String clave,
                               @RequestParam String confirmacion,
                               Model modelo, RedirectAttributes flash) {
        UsuarioListadoDTO u = usuarioDAO.buscarPorId(id);
        if (u == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el usuario.");
            return "redirect:/usuarios";
        }

        String error = validarClave(clave, confirmacion);
        if (error != null) {
            modelo.addAttribute("usuario", u);
            modelo.addAttribute("error", error);
            return "usuario-clave";
        }

        try {
            usuarioDAO.cambiarClave(id, clave);
        } catch (RuntimeException e) {
            modelo.addAttribute("usuario", u);
            modelo.addAttribute("error", e.getMessage());
            return "usuario-clave";
        }
        flash.addFlashAttribute("mensajeOk", "Contraseña de " + u.getNombreUsuario() + " actualizada.");
        return "redirect:/usuarios";
    }

    // POST /usuarios/5/eliminar
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, HttpSession sesion, RedirectAttributes flash) {
        UsuarioDTO quienElimina = (UsuarioDTO) sesion.getAttribute("usuario");
        UsuarioListadoDTO objetivo = usuarioDAO.buscarPorId(id);

        if (objetivo == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el usuario.");
        } else if (quienElimina.getId() == id) {
            flash.addFlashAttribute("mensajeError", "No puedes eliminar tu propio usuario.");
        } else if (objetivo.getRol().equals("admin") && usuarioDAO.contarAdministradores() <= 1) {
            flash.addFlashAttribute("mensajeError", "Debe quedar al menos un administrador.");
        } else {
            try {
                usuarioDAO.eliminar(id);
                flash.addFlashAttribute("mensajeOk", "Usuario eliminado.");
            } catch (RuntimeException e) {
                flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
            }
        }
        return "redirect:/usuarios";
    }

    // Devuelve el mensaje de error, o null si los datos están bien
    private String validarDatos(Integer id, String nombreCompleto, String nombreUsuario, String rol,
                                Integer entrenadorId) {
        if (nombreCompleto.isBlank()) {
            return "El nombre completo es obligatorio.";
        }
        if (!NOMBRE_USUARIO.matcher(nombreUsuario.trim()).matches()) {
            return "El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo.";
        }
        if (!ROLES.contains(rol)) {
            return "El rol no es válido.";
        }
        if (rol.equals("entrenador") && entrenadorId == null) {
            return "Elige el entrenador al que pertenece este usuario.";
        }
        if (usuarioDAO.existeNombreUsuario(nombreUsuario.trim(), id == null ? 0 : id)) {
            return "Ya existe un usuario con ese nombre.";
        }
        return null;
    }

    private String validarClave(String clave, String confirmacion) {
        if (clave.length() < LARGO_MINIMO_CLAVE) {
            return "La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.";
        }
        if (!clave.equals(confirmacion)) {
            return "La contraseña y su confirmación no coinciden.";
        }
        return null;
    }

    private String volverAlFormulario(Model modelo, Integer id, String nombreCompleto, String nombreUsuario,
                                      String rol, Integer entrenadorId, String error) {
        if (id != null) {
            modelo.addAttribute("id", id);
        }
        modelo.addAttribute("nombreCompleto", nombreCompleto);
        modelo.addAttribute("nombreUsuario", nombreUsuario);
        modelo.addAttribute("rol", rol);
        modelo.addAttribute("entrenadorId", entrenadorId);
        modelo.addAttribute("error", error);
        cargarEntrenadores(modelo, id);
        return "usuario-form";
    }

    // Solo se ofrecen los entrenadores que todavía no tienen usuario (más el del usuario que se edita)
    private void cargarEntrenadores(Model modelo, Integer idUsuarioEditado) {
        Set<Integer> ocupados = new HashSet<>();
        for (UsuarioListadoDTO u : usuarioDAO.listar()) {
            boolean esElEditado = idUsuarioEditado != null && u.getId() == idUsuarioEditado;
            if (u.getEntrenadorId() != null && !esElEditado) {
                ocupados.add(u.getEntrenadorId());
            }
        }
        List<Entrenador> libres = new ArrayList<>();
        for (Entrenador e : entrenadorDAO.listarTodos()) {
            if (!ocupados.contains(e.getId())) {
                libres.add(e);
            }
        }
        modelo.addAttribute("entrenadores", libres);
    }
}
