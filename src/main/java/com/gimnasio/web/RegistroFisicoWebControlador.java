package com.gimnasio.web;

import com.gimnasio.dao.MembresiaClienteDAO;
import com.gimnasio.dao.RegistroFisicoWebDAO;
import com.gimnasio.dto.MembresiaClienteDTO;
import com.gimnasio.dto.RegistroFisicoDTO;
import com.gimnasio.util.Reloj;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/registro-fisico")
public class RegistroFisicoWebControlador {

    private static final int MAXIMO_EN_HISTORIAL = 200;

    private final RegistroFisicoWebDAO registroDAO = new RegistroFisicoWebDAO();
    private final MembresiaClienteDAO clienteDAO = new MembresiaClienteDAO(); // lista de clientes

    // GET /registro-fisico -> formulario + mediciones recientes de todos
    @GetMapping
    public String pagina(Model modelo) {
        modelo.addAttribute("registros", registroDAO.listarRecientes(MAXIMO_EN_HISTORIAL));
        modelo.addAttribute("clientes", clienteDAO.listarTodos());
        modelo.addAttribute("hoy", Reloj.hoy().toString());
        modelo.addAttribute("maximo", MAXIMO_EN_HISTORIAL);
        return "registro-fisico";
    }

    // GET /registro-fisico/cliente/5 -> historial completo del cliente 5
    @GetMapping("/cliente/{id}")
    public String historialCliente(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        MembresiaClienteDTO cliente = clienteDAO.buscarPorClienteId(id);
        if (cliente == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/registro-fisico";
        }

        List<RegistroFisicoDTO> registros = registroDAO.listarPorCliente(id);

        modelo.addAttribute("cliente", cliente);
        modelo.addAttribute("registros", registros);
        modelo.addAttribute("hoy", Reloj.hoy().toString());
        if (!registros.isEmpty()) {
            RegistroFisicoDTO ultimo = registros.get(0);
            RegistroFisicoDTO primero = registros.get(registros.size() - 1);
            modelo.addAttribute("ultimo", ultimo);
            modelo.addAttribute("cambioTotal", registros.size() < 2 ? "—" : cambio(primero, ultimo));
        }
        return "registro-fisico-cliente";
    }

    // POST /registro-fisico -> guardar una medición
    @PostMapping
    public String registrar(@RequestParam(required = false) Integer clienteId,
                            @RequestParam String fecha,
                            @RequestParam String peso,
                            @RequestParam String altura,
                            @RequestParam(defaultValue = "false") boolean desdeCliente,
                            RedirectAttributes flash) {

        String destino = destino(desdeCliente, clienteId == null ? 0 : clienteId);

        if (clienteId == null) {
            flash.addFlashAttribute("mensajeError", "Elige un cliente de la lista de sugerencias.");
            return destino;
        }

        LocalDate fechaMedicion;
        try {
            fechaMedicion = LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
            flash.addFlashAttribute("mensajeError", "La fecha no es válida.");
            return destino;
        }
        if (fechaMedicion.isAfter(Reloj.hoy())) {
            flash.addFlashAttribute("mensajeError", "La fecha no puede ser futura.");
            return destino;
        }

        BigDecimal pesoKg = numero(peso);
        BigDecimal alturaM = numero(altura);
        if (pesoKg == null || alturaM == null) {
            flash.addFlashAttribute("mensajeError", "El peso y la altura deben ser números (ej: 72.5 y 1.75).");
            return destino;
        }
        if (pesoKg.compareTo(new BigDecimal("20")) < 0 || pesoKg.compareTo(new BigDecimal("400")) > 0) {
            flash.addFlashAttribute("mensajeError", "El peso debe estar entre 20 y 400 kg.");
            return destino;
        }
        if (alturaM.compareTo(new BigDecimal("0.5")) < 0 || alturaM.compareTo(new BigDecimal("2.5")) > 0) {
            flash.addFlashAttribute("mensajeError", "La altura debe estar entre 0.5 y 2.5 metros (ej: 1.75).");
            return destino;
        }

        try {
            registroDAO.registrar(clienteId, fechaMedicion,
                    pesoKg.setScale(2, RoundingMode.HALF_UP), alturaM.setScale(2, RoundingMode.HALF_UP));
            flash.addFlashAttribute("mensajeOk", "Medición registrada.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo registrar: " + e.getMessage());
        }
        return destino;
    }

    // POST /registro-fisico/7/eliminar -> borrar una medición hecha por error
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id,
                           @RequestParam(defaultValue = "false") boolean desdeCliente,
                           RedirectAttributes flash) {
        int clienteId = 0;
        try {
            clienteId = registroDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Medición eliminada.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return destino(desdeCliente, clienteId);
    }

    // A dónde volver: a la página general o al historial del cliente
    private String destino(boolean desdeCliente, int clienteId) {
        if (desdeCliente && clienteId > 0) {
            return "redirect:/registro-fisico/cliente/" + clienteId;
        }
        return "redirect:/registro-fisico";
    }

    // Acepta "72.5" y "72,5". Devuelve null si no es un número.
    private BigDecimal numero(String texto) {
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String cambio(RegistroFisicoDTO primero, RegistroFisicoDTO ultimo) {
        BigDecimal diferencia = ultimo.getPeso().subtract(primero.getPeso());
        return (diferencia.signum() > 0 ? "+" : "") + diferencia.toPlainString() + " kg";
    }
}
