package com.gimnasio.web;

import com.gimnasio.dao.ClienteDAO;
import com.gimnasio.dao.FotoDAO;
import com.gimnasio.dao.impl.ClienteDAOImpl;
import com.gimnasio.modelo.Cliente;
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
@RequestMapping("/clientes")
public class ClienteWebControlador {

    private static final long TAMANO_MAXIMO_FOTO = 5L * 1024 * 1024; // 5 MB

    // El mismo DAO que ya tenías funcionando en la versión de escritorio
    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final FotoDAO fotoDAO = new FotoDAO();

    // GET /clientes -> lista de clientes
    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("clientes", clienteDAO.listarTodos());
        return "clientes";
    }

    // GET /clientes/nuevo -> formulario vacío
    @GetMapping("/nuevo")
    public String formularioNuevo() {
        return "cliente-form";
    }

    // GET /clientes/5/editar -> formulario con los datos del cliente 5
    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable int id, Model modelo, RedirectAttributes flash) {
        Cliente cliente = clienteDAO.buscarPorId(id);

        if (cliente == null) {
            flash.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/clientes";
        }

        modelo.addAttribute("id", cliente.getId());
        modelo.addAttribute("nombre", cliente.getNombre());
        modelo.addAttribute("documento", cliente.getDocumento());
        modelo.addAttribute("telefono", cliente.getTelefono());
        modelo.addAttribute("email", cliente.getEmail());
        modelo.addAttribute("genero", cliente.getGenero());
        modelo.addAttribute("fechaNacimiento", cliente.getFechaNacimiento().toString());
        modelo.addAttribute("tieneFoto", fotoDAO.existe(id));
        return "cliente-form";
    }

    // GET /clientes/5/foto -> la imagen del cliente 5 (la usan la lista y el formulario)
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

    // POST /clientes -> crear
    @PostMapping
    public String crear(@RequestParam String nombre,
                        @RequestParam String documento,
                        @RequestParam(defaultValue = "") String telefono,
                        @RequestParam(defaultValue = "") String email,
                        @RequestParam String genero,
                        @RequestParam String fechaNacimiento,
                        @RequestParam(value = "foto", required = false) MultipartFile foto,
                        @RequestParam(defaultValue = "false") boolean quitarFoto,
                        Model modelo, RedirectAttributes flash) {
        return procesar(null, nombre, documento, telefono, email, genero, fechaNacimiento,
                foto, quitarFoto, modelo, flash);
    }

    // POST /clientes/5 -> actualizar el cliente 5
    @PostMapping("/{id}")
    public String actualizar(@PathVariable int id,
                             @RequestParam String nombre,
                             @RequestParam String documento,
                             @RequestParam(defaultValue = "") String telefono,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam String genero,
                             @RequestParam String fechaNacimiento,
                             @RequestParam(value = "foto", required = false) MultipartFile foto,
                             @RequestParam(defaultValue = "false") boolean quitarFoto,
                             Model modelo, RedirectAttributes flash) {
        return procesar(id, nombre, documento, telefono, email, genero, fechaNacimiento,
                foto, quitarFoto, modelo, flash);
    }

    // POST /clientes/5/eliminar -> borrar el cliente 5
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            clienteDAO.eliminar(id);
            flash.addFlashAttribute("mensajeOk", "Cliente eliminado.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/clientes";
    }

    // Lógica común de crear y actualizar. Si id es null, es un cliente nuevo.
    private String procesar(Integer id, String nombre, String documento, String telefono,
                            String email, String genero, String fechaNacimiento,
                            MultipartFile foto, boolean quitarFoto,
                            Model modelo, RedirectAttributes flash) {

        // Para devolver lo que el usuario escribió si hay un error
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

        if (nombre.isBlank() || documento.isBlank()) {
            modelo.addAttribute("error", "El nombre y el documento son obligatorios.");
            return "cliente-form";
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaNacimiento.trim());
        } catch (DateTimeParseException e) {
            modelo.addAttribute("error", "La fecha de nacimiento no es válida.");
            return "cliente-form";
        }

        // Validar la foto ANTES de guardar al cliente
        byte[] datosFoto = null;
        String tipoFoto = null;
        if (foto != null && !foto.isEmpty()) {
            if (foto.getSize() > TAMANO_MAXIMO_FOTO) {
                modelo.addAttribute("error", "La foto pesa más de 5 MB.");
                return "cliente-form";
            }
            try {
                datosFoto = foto.getBytes();
            } catch (IOException e) {
                modelo.addAttribute("error", "No se pudo leer la foto.");
                return "cliente-form";
            }
            tipoFoto = detectarTipoImagen(datosFoto);
            if (tipoFoto == null) {
                modelo.addAttribute("error", "La foto debe ser JPG, PNG o WebP.");
                return "cliente-form";
            }
        }

        String aviso;
        try {
            if (id == null) {
                clienteDAO.guardar(new Cliente(0, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha));
                aviso = "Cliente registrado.";
            } else {
                clienteDAO.actualizar(new Cliente(id, nombre.trim(), documento.trim(),
                        telefono.trim(), email.trim(), genero, fecha));
                aviso = "Cliente actualizado.";
            }
        } catch (RuntimeException e) {
            // Por ejemplo: documento duplicado (mensaje que arma ErrorBD)
            modelo.addAttribute("error", e.getMessage());
            return "cliente-form";
        }

        // La foto se guarda después, ya con el cliente en la base de datos
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
            return "redirect:/clientes";
        }

        flash.addFlashAttribute("mensajeOk", aviso);
        return "redirect:/clientes";
    }

    // Mira los primeros bytes del archivo para saber qué imagen es de verdad
    // (no confiamos en lo que diga el navegador).
    private String detectarTipoImagen(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}