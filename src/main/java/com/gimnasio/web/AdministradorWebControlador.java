package com.gimnasio.web;

import com.gimnasio.dao.AdministradorDAO;
import com.gimnasio.dao.FotoDAO;
import com.gimnasio.dao.impl.AdministradorDAOImpl;
import com.gimnasio.modelo.Administrador;
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
@RequestMapping("/administradores")
public class AdministradorWebControlador {

    private static final long TAMANO_MAXIMO_FOTO = 5L * 1024 * 1024; // 5 MB

    private final AdministradorDAO administradorDAO = new AdministradorDAOImpl();
    private final FotoDAO fotoDAO = new FotoDAO();

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("administradores", administradorDAO.listarTodos());
        return "administradores";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo() {
        return "administrador-form";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        Administrador a = administradorDAO.buscarPorId(id);

        if (a == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el administrador.");
            return "redirect:/administradores";
        }

        modelo.addAttribute("id", a.getId());
        modelo.addAttribute("nombre", a.getNombre());
        modelo.addAttribute("documento", a.getDocumento());
        modelo.addAttribute("telefono", a.getTelefono());
        modelo.addAttribute("email", a.getEmail());
        modelo.addAttribute("genero", a.getGenero());
        modelo.addAttribute("fechaNacimiento", a.getFechaNacimiento().toString());
        modelo.addAttribute("permisos", a.getPermisos());
        modelo.addAttribute("tieneFoto", fotoDAO.existe(id));
        return "administrador-form";
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
                        @RequestParam(defaultValue = "") String permisos,
                        @RequestParam(value = "foto", required = false) MultipartFile foto,
                        @RequestParam(defaultValue = "false") boolean quitarFoto,
                        Model modelo, RedirectAttributes flash) {
        return procesar(null, nombre, documento, telefono, email, genero, fechaNacimiento,
                permisos, foto, quitarFoto, modelo, flash);
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable int id,
                             @RequestParam String nombre,
                             @RequestParam String documento,
                             @RequestParam(defaultValue = "") String telefono,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam String genero,
                             @RequestParam String fechaNacimiento,
                             @RequestParam(defaultValue = "") String permisos,
                             @RequestParam(value = "foto", required = false) MultipartFile foto,
                             @RequestParam(defaultValue = "false") boolean quitarFoto,
                             Model modelo, RedirectAttributes flash) {
        return procesar(id, nombre, documento, telefono, email, genero, fechaNacimiento,
                permisos, foto, quitarFoto, modelo, flash);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            administradorDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Administrador eliminado.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/administradores";
    }

    // Lógica común de crear y actualizar. Si id es null, es un administrador nuevo.
    private String procesar(Integer id, String nombre, String documento, String telefono,
                            String email, String genero, String fechaNacimiento, String permisos,
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
        modelo.addAttribute("permisos", permisos);

        if (nombre.isBlank() || documento.isBlank()) {
            modelo.addAttribute("error", "El nombre y el documento son obligatorios.");
            return "administrador-form";
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaNacimiento.trim());
        } catch (DateTimeParseException e) {
            modelo.addAttribute("error", "La fecha de nacimiento no es válida.");
            return "administrador-form";
        }

        byte[] datosFoto = null;
        String tipoFoto = null;
        if (foto != null && !foto.isEmpty()) {
            if (foto.getSize() > TAMANO_MAXIMO_FOTO) {
                modelo.addAttribute("error", "La foto pesa más de 5 MB.");
                return "administrador-form";
            }
            try {
                datosFoto = foto.getBytes();
            } catch (IOException e) {
                modelo.addAttribute("error", "No se pudo leer la foto.");
                return "administrador-form";
            }
            tipoFoto = Imagenes.detectarTipo(datosFoto);
            if (tipoFoto == null) {
                modelo.addAttribute("error", "La foto debe ser JPG, PNG o WebP.");
                return "administrador-form";
            }
        }

        String aviso;
        try {
            if (id == null) {
                administradorDAO.guardar(new Administrador(0, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha, permisos.trim()));
                aviso = "Administrador registrado.";
            } else {
                administradorDAO.actualizar(new Administrador(id, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha, permisos.trim()));
                aviso = "Administrador actualizado.";
            }
        } catch (RuntimeException e) {
            modelo.addAttribute("error", e.getMessage());
            return "administrador-form";
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
            return "redirect:/administradores";
        }

        flash.addFlashAttribute("mensajeOk", aviso);
        return "redirect:/administradores";
    }
}