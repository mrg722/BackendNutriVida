package cl.duoc.nutrivida.auth.controller;

import cl.duoc.nutrivida.auth.dto.UsuarioRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Este es el CRUD de usuarios del panel de administración. SecurityConfig lo reserva para ADMIN.
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles, solo para el rol ADMIN")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // Si llega el parámetro rol filtro la lista; si no llega devuelvo todos los usuarios.
    @GetMapping
    @Operation(summary = "Lista los usuarios y permite filtrar por rol")
    public List<UsuarioResponse> listar(@RequestParam(required = false) String rol) {
        return rol == null || rol.isBlank() ? service.listar() : service.listarPorRol(rol);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca un usuario por su id")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    // Devuelvo 201 Created, que es el código correcto cuando una petición crea un recurso.
    @PostMapping
    @Operation(summary = "Crea un usuario nuevo y le asigna un rol")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    // Con PUT reemplazo los datos del usuario, incluido el campo activo para desactivar la cuenta.
    @PutMapping("/{id}")
    @Operation(summary = "Edita un usuario, cambia su rol o lo desactiva")
    public UsuarioResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request) {
        return service.actualizar(id, request);
    }

    // DELETE no devuelve cuerpo, por eso respondo 204 No Content.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina definitivamente un usuario")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
