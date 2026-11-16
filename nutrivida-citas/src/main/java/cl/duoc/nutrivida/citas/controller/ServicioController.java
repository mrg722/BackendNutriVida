package cl.duoc.nutrivida.citas.controller;

import cl.duoc.nutrivida.citas.model.Servicio;
import cl.duoc.nutrivida.citas.service.ServicioService;
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

// CRUD del catálogo de servicios, con las rutas versionadas igual que el resto de la API.
@RestController
@RequestMapping("/api/v1/servicios")
@Tag(name = "Servicios", description = "Catálogo de consultas, planes, evaluaciones y talleres")
public class ServicioController {

    private final ServicioService service;

    public ServicioController(ServicioService service) {
        this.service = service;
    }

    // Acepto dos filtros opcionales: solo activos, o por tipo de servicio.
    @GetMapping
    @Operation(summary = "Lista el catálogo y permite filtrar por tipo o solo los activos")
    public List<Servicio> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {

        if (tipo != null && !tipo.isBlank()) {
            return service.listarPorTipo(tipo);
        }
        return soloActivos ? service.listarActivos() : service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca un servicio del catálogo por su id")
    public Servicio buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    // POST devuelve 201 y el objeto creado, con el id que asignó la base de datos.
    @PostMapping
    @Operation(summary = "Crea un servicio nuevo en el catálogo")
    public ResponseEntity<Servicio> crear(@Valid @RequestBody Servicio servicio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(servicio));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un servicio del catálogo")
    public Servicio actualizar(@PathVariable Long id, @Valid @RequestBody Servicio servicio) {
        return service.actualizar(id, servicio);
    }

    // DELETE no devuelve cuerpo, por eso respondo 204 No Content.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un servicio del catálogo")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
