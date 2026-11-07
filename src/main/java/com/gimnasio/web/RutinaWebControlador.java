package com.gimnasio.web;

import com.gimnasio.dao.EntrenadorDAO;
import com.gimnasio.dao.MembresiaClienteDAO;
import com.gimnasio.dao.RutinaWebDAO;
import com.gimnasio.dao.impl.EntrenadorDAOImpl;
import com.gimnasio.dto.EjercicioDTO;
import com.gimnasio.dto.MembresiaClienteDTO;
import com.gimnasio.dto.RutinaDTO;
import jakarta.servlet.http.HttpServletRequest;
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

@Controller
@RequestMapping("/rutinas")
public class RutinaWebControlador {

    private final RutinaWebDAO rutinaDAO = new RutinaWebDAO();
    private final MembresiaClienteDAO clienteDAO = new MembresiaClienteDAO(); // lista de clientes
    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAOImpl();

    // GET /rutinas -> lista
    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("rutinas", rutinaDAO.listarTodas());
        return "rutinas";
    }

    // GET /rutinas/nuevo -> formulario vacío
    @GetMapping("/nuevo")
    public String formularioNuevo(Model modelo) {
        cargarListas(modelo, null, null);
        modelo.addAttribute("ejercicios", new ArrayList<EjercicioDTO>());
        return "rutina-form";
    }

    // GET /rutinas/5 -> la rutina completa para leerla (o enseñársela al cliente)
    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        RutinaDTO rutina = rutinaDAO.buscarPorId(id);
        if (rutina == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró la rutina.");
            return "redirect:/rutinas";
        }
        modelo.addAttribute("rutina", rutina);
        return "rutina-detalle";
    }

    // GET /rutinas/5/editar
    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        RutinaDTO rutina = rutinaDAO.buscarPorId(id);
        if (rutina == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró la rutina.");
            return "redirect:/rutinas";
        }
        cargarListas(modelo, id, rutina.getClienteNombre());
        modelo.addAttribute("entrenadorId", rutina.getEntrenadorId());
        modelo.addAttribute("nombre", rutina.getNombre());
        modelo.addAttribute("nivel", rutina.getNivel());
        modelo.addAttribute("objetivo", rutina.getObjetivo());
        modelo.addAttribute("ejercicios", rutina.getEjercicios());
        return "rutina-form";
    }

    // POST /rutinas -> crear
    @PostMapping
    public String crear(@RequestParam(required = false) Integer clienteId,
                        @RequestParam int entrenadorId,
                        @RequestParam String nombre,
                        @RequestParam(defaultValue = "") String nivel,
                        @RequestParam(defaultValue = "") String objetivo,
                        HttpServletRequest solicitud, Model modelo, RedirectAttributes flash) {
        return procesar(null, clienteId, entrenadorId, nombre, nivel, objetivo, solicitud, modelo, flash);
    }

    // POST /rutinas/5 -> actualizar
    @PostMapping("/{id}")
    public String actualizar(@PathVariable int id,
                             @RequestParam int entrenadorId,
                             @RequestParam String nombre,
                             @RequestParam(defaultValue = "") String nivel,
                             @RequestParam(defaultValue = "") String objetivo,
                             HttpServletRequest solicitud, Model modelo, RedirectAttributes flash) {
        return procesar(id, null, entrenadorId, nombre, nivel, objetivo, solicitud, modelo, flash);
    }

    // POST /rutinas/5/eliminar
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            rutinaDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Rutina eliminada.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/rutinas";
    }

    // Lógica común de crear y actualizar. Si id es null, es una rutina nueva.
    private String procesar(Integer id, Integer clienteId, int entrenadorId, String nombre, String nivel,
                            String objetivo, HttpServletRequest solicitud, Model modelo, RedirectAttributes flash) {

        List<EjercicioDTO> ejercicios = leerEjercicios(solicitud);

        // Para devolver lo que el usuario escribió si hay un error
        String clienteNombre = null;
        if (id != null) {
            RutinaDTO actual = rutinaDAO.buscarPorId(id);
            clienteNombre = actual == null ? "" : actual.getClienteNombre();
        }
        cargarListas(modelo, id, clienteNombre);
        modelo.addAttribute("entrenadorId", entrenadorId);
        modelo.addAttribute("nombre", nombre);
        modelo.addAttribute("nivel", nivel);
        modelo.addAttribute("objetivo", objetivo);
        modelo.addAttribute("ejercicios", ejercicios);
        if (id == null && clienteId != null) {
            modelo.addAttribute("clienteId", clienteId);
            for (MembresiaClienteDTO c : clientesSinRutina()) {
                if (c.getClienteId() == clienteId) {
                    modelo.addAttribute("clienteTexto", c.getNombre() + " — " + c.getDocumento());
                }
            }
        }

        String error = validar(id, clienteId, nombre, ejercicios);
        if (error != null) {
            modelo.addAttribute("error", error);
            return "rutina-form";
        }

        try {
            if (id == null) {
                rutinaDAO.crear(clienteId, entrenadorId, nombre.trim(), nivel, objetivo, ejercicios);
                flash.addFlashAttribute("mensajeOk", "Rutina creada.");
            } else {
                rutinaDAO.actualizar(id, entrenadorId, nombre.trim(), nivel, objetivo, ejercicios);
                flash.addFlashAttribute("mensajeOk", "Rutina actualizada.");
            }
        } catch (RuntimeException e) {
            // Por ejemplo: ese cliente ya tiene rutina (mensaje que arma ErrorBD)
            modelo.addAttribute("error", e.getMessage());
            return "rutina-form";
        }

        return "redirect:/rutinas";
    }

    private String validar(Integer id, Integer clienteId, String nombre, List<EjercicioDTO> ejercicios) {
        if (id == null && clienteId == null) {
            return "Elige un cliente de la lista de sugerencias.";
        }
        if (nombre.isBlank()) {
            return "El nombre de la rutina es obligatorio.";
        }
        if (ejercicios.isEmpty()) {
            return "Agrega al menos un ejercicio.";
        }
        int numero = 0;
        for (EjercicioDTO e : ejercicios) {
            numero++;
            if (e.getNombre().isEmpty()) {
                return "El ejercicio " + numero + " necesita un nombre.";
            }
            if (e.getSeries() <= 0 || e.getRepeticiones() <= 0) {
                return "En \"" + e.getNombre() + "\", las series y las repeticiones deben ser números mayores que 0.";
            }
            if (e.getDescanso() != null && e.getDescanso() < 0) {
                return "En \"" + e.getNombre() + "\", el descanso debe ser un número de segundos (o déjalo vacío).";
            }
        }
        return null;
    }

    // Los ejercicios llegan como listas paralelas (ejNombre, ejSeries, ...). Se leen directo de la
    // solicitud porque con @RequestParam un solo ejercicio con comas en el nombre se partiría en varios.
    private List<EjercicioDTO> leerEjercicios(HttpServletRequest solicitud) {
        String[] nombres = solicitud.getParameterValues("ejNombre");
        String[] series = solicitud.getParameterValues("ejSeries");
        String[] repeticiones = solicitud.getParameterValues("ejRepeticiones");
        String[] descansos = solicitud.getParameterValues("ejDescanso");

        List<EjercicioDTO> lista = new ArrayList<>();
        if (nombres == null) {
            return lista;
        }
        for (int i = 0; i < nombres.length; i++) {
            String nombre = valor(nombres, i);
            String s = valor(series, i);
            String r = valor(repeticiones, i);
            String d = valor(descansos, i);

            if (nombre.isEmpty() && s.isEmpty() && r.isEmpty() && d.isEmpty()) {
                continue; // fila vacía, se ignora
            }
            // Un número inválido queda como 0 (series/repeticiones) o -1 (descanso) y lo rechaza validar()
            lista.add(new EjercicioDTO(nombre, entero(s, 0), entero(r, 0), d.isEmpty() ? null : entero(d, -1)));
        }
        return lista;
    }

    private String valor(String[] arreglo, int i) {
        if (arreglo == null || i >= arreglo.length || arreglo[i] == null) {
            return "";
        }
        return arreglo[i].trim();
    }

    private int entero(String texto, int siFalla) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return siFalla;
        }
    }

    // Clientes que todavía no tienen rutina (cada cliente solo puede tener una)
    private List<MembresiaClienteDTO> clientesSinRutina() {
        Set<Integer> conRutina = new HashSet<>();
        for (RutinaDTO r : rutinaDAO.listarTodas()) {
            conRutina.add(r.getClienteId());
        }
        List<MembresiaClienteDTO> libres = new ArrayList<>();
        for (MembresiaClienteDTO c : clienteDAO.listarTodos()) {
            if (!conRutina.contains(c.getClienteId())) {
                libres.add(c);
            }
        }
        return libres;
    }

    private void cargarListas(Model modelo, Integer id, String clienteNombre) {
        modelo.addAttribute("entrenadores", entrenadorDAO.listarTodos());
        if (id == null) {
            modelo.addAttribute("clientes", clientesSinRutina());
        } else {
            modelo.addAttribute("id", id);
            modelo.addAttribute("clienteNombre", clienteNombre);
        }
    }
}