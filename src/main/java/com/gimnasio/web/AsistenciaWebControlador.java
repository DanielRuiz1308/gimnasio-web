package com.gimnasio.web;

import com.gimnasio.dao.AsistenciaWebDAO;
import com.gimnasio.dao.EntrenadorDAO;
import com.gimnasio.dao.MembresiaClienteDAO;
import com.gimnasio.dao.impl.EntrenadorDAOImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/asistencia")
public class AsistenciaWebControlador {

    private static final int MAXIMO_EN_HISTORIAL = 200;

    private final AsistenciaWebDAO asistenciaDAO = new AsistenciaWebDAO();
    private final MembresiaClienteDAO membresiaDAO = new MembresiaClienteDAO(); // lista de clientes con su estado
    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAOImpl();

    // GET /asistencia -> formulario de registro + historial
    @GetMapping
    public String pagina(Model modelo) {
        modelo.addAttribute("asistencias", asistenciaDAO.listarRecientes(MAXIMO_EN_HISTORIAL));
        modelo.addAttribute("clientes", membresiaDAO.listarTodos());
        modelo.addAttribute("entrenadores", entrenadorDAO.listarTodos());
        modelo.addAttribute("maximo", MAXIMO_EN_HISTORIAL);
        return "asistencia";
    }

    // POST /asistencia -> registrar la asistencia de ahora mismo
    @PostMapping
    public String registrar(@RequestParam(required = false) Integer clienteId,
                            @RequestParam(required = false) Integer entrenadorId,
                            RedirectAttributes flash) {
        if (clienteId == null || entrenadorId == null) {
            flash.addFlashAttribute("mensajeError", "Elige un cliente y un entrenador.");
            return "redirect:/asistencia";
        }

        try {
            asistenciaDAO.registrar(clienteId, entrenadorId);
            flash.addFlashAttribute("mensajeOk", "Asistencia registrada.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo registrar: " + e.getMessage());
        }
        return "redirect:/asistencia";
    }

    // POST /asistencia/7/eliminar -> borrar un registro hecho por error
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            asistenciaDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Asistencia eliminada.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/asistencia";
    }
}