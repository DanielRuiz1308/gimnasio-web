package com.gimnasio.web;

import com.gimnasio.dao.ResumenDAO;
import com.gimnasio.dto.UsuarioDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioControlador {

    private final ResumenDAO resumenDAO = new ResumenDAO();

    // Cuando alguien abre la dirección "/", se ejecuta este método
    @GetMapping("/")
    public String inicio(HttpSession sesion, Model modelo) {
        // Dato que la página puede mostrar
        modelo.addAttribute("nombreGimnasio", "Lifting Club Fitness");

        // El resumen solo se consulta si el rol puede ver alguna de sus partes
        UsuarioDTO u = (UsuarioDTO) sesion.getAttribute("usuario");
        if (u != null && (u.puede("asistencia") || u.puede("membresias") || u.puede("reportes"))) {
            try {
                modelo.addAttribute("resumen", resumenDAO.generar());
            } catch (RuntimeException e) {
                // Si falla la base de datos, el inicio sigue funcionando sin el resumen
                modelo.addAttribute("errorResumen", "No se pudo cargar el resumen del día.");
            }
        }

        // Nombre del archivo en la carpeta templates (sin .html)
        return "inicio";
    }
}
