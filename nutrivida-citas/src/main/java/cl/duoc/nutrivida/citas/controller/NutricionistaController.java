package cl.duoc.nutrivida.citas.controller;

import cl.duoc.nutrivida.citas.model.Nutricionista;
import cl.duoc.nutrivida.citas.service.NutricionistaService;
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

// CRUD del directorio de nutricionistas que alimenta el formulario de agendamiento.
@RestController
@RequestMapping("/api/v1/nutricionistas")
@Tag(name = "Nutricionistas", description = "Directorio de profesionales, especialidades y horarios")
public class NutricionistaController {

    private final NutricionistaService service;

    public NutricionistaController(NutricionistaService service) {
        this.service = service;
    }

    // El formulario de citas pide soloActivos=true para no ofrecer profesionales que no atienden.
    @GetMapping
    @Operation(summary = "Lista el directorio de nutricionistas")
    public List<Nutricionista> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        return soloActivos ? service.listarActivos() : service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca un nutricionista por su id")
    public Nutricionista buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Agrega un nutricionista al directorio")
    public ResponseEntity<Nutricionista> crear(@Valid @RequestBody Nutricionista nutricionista) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(nutricionista));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos o la disponibilidad de un nutricionista")
    public Nutricionista actualizar(@PathVariable Long id,
            @Valid @RequestBody Nutricionista nutricionista) {
        return service.actualizar(id, nutricionista);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un nutricionista del directorio")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
