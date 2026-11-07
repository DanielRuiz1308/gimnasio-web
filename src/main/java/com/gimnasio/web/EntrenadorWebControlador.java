package com.gimnasio.web;

import com.gimnasio.dao.EntrenadorDAO;
import com.gimnasio.dao.FotoDAO;
import com.gimnasio.dao.impl.EntrenadorDAOImpl;
import com.gimnasio.modelo.Entrenador;
import com.gimnasio.util.Imagenes;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/entrenadores")
public class EntrenadorWebControlador {

    private static final long TAMANO_MAXIMO_FOTO = 5L * 1024 * 1024; // 5 MB

    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAOImpl();
    private final FotoDAO fotoDAO = new FotoDAO();

    @GetMapping
    public String listar(Model modelo) {
        // Si tu EntrenadorDAO usa otro nombre (por ejemplo listarTodos), cámbialo aquí
        modelo.addAttribute("entrenadores", entrenadorDAO.listarTodos());
        return "entrenadores";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo() {
        return "entrenador-form";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        Entrenador e = entrenadorDAO.buscarPorId(id);

        if (e == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el entrenador.");
            return "redirect:/entrenadores";
        }

        modelo.addAttribute("id", e.getId());
        modelo.addAttribute("nombre", e.getNombre());
        modelo.addAttribute("documento", e.getDocumento());
        modelo.addAttribute("telefono", e.getTelefono());
        modelo.addAttribute("email", e.getEmail());
        modelo.addAttribute("genero", e.getGenero());
        modelo.addAttribute("fechaNacimiento", e.getFechaNacimiento().toString());
        modelo.addAttribute("especialidad", e.getEspecialidad());
        modelo.addAttribute("tieneFoto", fotoDAO.existe(id));
        return "entrenador-form";
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable int id) {
        FotoDAO.Foto foto = fotoDAO.obtener(id);
        if (foto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.datos);
    }

    @PostMapping
    public String crear(@RequestParam String nombre,
                        @RequestParam String documento,
                        @RequestParam(defaultValue = "") String telefono,
                        @RequestParam(defaultValue = "") String email,
                        @RequestParam String genero,
                        @RequestParam String fechaNacimiento,
                        @RequestParam(defaultValue = "") String especialidad,
                        @RequestParam(value = "foto", required = false) MultipartFile foto,
                        @RequestParam(defaultValue = "false") boolean quitarFoto,
                        Model modelo, RedirectAttributes flash) {
        return procesar(null, nombre, documento, telefono, email, genero, fechaNacimiento,
                especialidad, foto, quitarFoto, modelo, flash);
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable int id,
                             @RequestParam String nombre,
                             @RequestParam String documento,
                             @RequestParam(defaultValue = "") String telefono,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam String genero,
                             @RequestParam String fechaNacimiento,
                             @RequestParam(defaultValue = "") String especialidad,
                             @RequestParam(value = "foto", required = false) MultipartFile foto,
                             @RequestParam(defaultValue = "false") boolean quitarFoto,
                             Model modelo, RedirectAttributes flash) {
        return procesar(id, nombre, documento, telefono, email, genero, fechaNacimiento,
                especialidad, foto, quitarFoto, modelo, flash);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            entrenadorDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Entrenador eliminado.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/entrenadores";
    }

    // Lógica común de crear y actualizar. Si id es null, es un entrenador nuevo.
    private String procesar(Integer id, String nombre, String documento, String telefono,
                            String email, String genero, String fechaNacimiento, String especialidad,
                            MultipartFile foto, boolean quitarFoto,
                            Model modelo, RedirectAttributes flash) {

        if (id != null) {
            modelo.addAttribute("id", id);
            modelo.addAttribute("tieneFoto", fotoDAO.existe(id));
        }
        modelo.addAttribute("nombre", nombre);
        modelo.addAttribute("documento", documento);
        modelo.addAttribute("telefono", telefono);
        modelo.addAttribute("email", email);
        modelo.addAttribute("genero", genero);
        modelo.addAttribute("fechaNacimiento", fechaNacimiento);
        modelo.addAttribute("especialidad", especialidad);

        if (nombre.isBlank() || documento.isBlank()) {
            modelo.addAttribute("error", "El nombre y el documento son obligatorios.");
            return "entrenador-form";
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaNacimiento.trim());
        } catch (DateTimeParseException e) {
            modelo.addAttribute("error", "La fecha de nacimiento no es válida.");
            return "entrenador-form";
        }

        byte[] datosFoto = null;
        String tipoFoto = null;
        if (foto != null && !foto.isEmpty()) {
            if (foto.getSize() > TAMANO_MAXIMO_FOTO) {
                modelo.addAttribute("error", "La foto pesa más de 5 MB.");
                return "entrenador-form";
            }
            try {
                datosFoto = foto.getBytes();
            } catch (IOException e) {
                modelo.addAttribute("error", "No se pudo leer la foto.");
                return "entrenador-form";
            }
            tipoFoto = Imagenes.detectarTipo(datosFoto);
            if (tipoFoto == null) {
                modelo.addAttribute("error", "La foto debe ser JPG, PNG o WebP.");
                return "entrenador-form";
            }
        }

        String aviso;
        try {
            if (id == null) {
                entrenadorDAO.guardar(new Entrenador(0, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha, especialidad.trim()));
                aviso = "Entrenador registrado.";
            } else {
                entrenadorDAO.actualizar(new Entrenador(id, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha, especialidad.trim()));
                aviso = "Entrenador actualizado.";
            }
        } catch (RuntimeException e) {
            modelo.addAttribute("error", e.getMessage());
            return "entrenador-form";
        }

        try {
            if (datosFoto != null) {
                if (id == null) {
                    fotoDAO.guardarPorDocumento(documento.trim(), datosFoto, tipoFoto);
                } else {
                    fotoDAO.guardar(id, datosFoto, tipoFoto);
                }
            } else if (quitarFoto && id != null) {
                fotoDAO.quitar(id);
            }
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", aviso + " Pero la foto no se pudo guardar: " + e.getMessage());
            return "redirect:/entrenadores";
        }

        flash.addFlashAttribute("mensajeOk", aviso);
        return "redirect:/entrenadores";
    }
}