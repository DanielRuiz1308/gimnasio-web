package com.gimnasio.web;

import com.gimnasio.dao.ReporteDAO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes")
public class ReporteWebControlador {

    private final ReporteDAO reporteDAO = new ReporteDAO();

    // GET /reportes -> tarjetas de resumen y gráficos
    @GetMapping
    public String pagina(Model modelo) {
        modelo.addAttribute("reporte", reporteDAO.generar());
        return "reportes";
    }
}