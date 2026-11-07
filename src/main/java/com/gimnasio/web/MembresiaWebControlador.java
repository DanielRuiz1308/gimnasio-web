package com.gimnasio.web;

import com.gimnasio.dao.MembresiaClienteDAO;
import com.gimnasio.dto.MembresiaClienteDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/membresias")
public class MembresiaWebControlador {

    private final MembresiaClienteDAO membresiaDAO = new MembresiaClienteDAO();

    // GET /membresias -> todos los clientes con el estado de su membresía
    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("membresias", membresiaDAO.listarTodos());
        return "membresias";
    }

    // GET /membresias/5/renovar -> formulario para renovar la membresía del cliente 5
    @GetMapping("/{clienteId}/renovar")
    public String formularioRenovar(@PathVariable int clienteId, Model modelo, RedirectAttributes flash) {
        MembresiaClienteDTO cliente = membresiaDAO.buscarPorClienteId(clienteId);
        if (cliente == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/membresias";
        }
        cargarFormulario(modelo, cliente);
        return "membresia-renovar";
    }

    // POST /membresias/5/renovar -> confirmar el pago
    @PostMapping("/{clienteId}/renovar")
    public String renovar(@PathVariable int clienteId,
                          @RequestParam int tipoId,
                          @RequestParam String metodoPago,
                          Model modelo, RedirectAttributes flash) {
        MembresiaClienteDTO cliente = membresiaDAO.buscarPorClienteId(clienteId);
        if (cliente == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/membresias";
        }

        try {
            membresiaDAO.renovar(clienteId, tipoId, metodoPago);
        } catch (RuntimeException e) {
            cargarFormulario(modelo, cliente);
            modelo.addAttribute("error", e.getMessage());
            return "membresia-renovar";
        }

        MembresiaClienteDTO actualizado = membresiaDAO.buscarPorClienteId(clienteId);
        flash.addFlashAttribute("mensajeOk",
                "Membresía de " + cliente.getNombre() + " renovada hasta " + actualizado.getFechaVencimiento() + ".");
        return "redirect:/membresias";
    }

    private void cargarFormulario(Model modelo, MembresiaClienteDTO cliente) {
        modelo.addAttribute("cliente", cliente);
        modelo.addAttribute("tipos", membresiaDAO.listarTipos());
    }
}