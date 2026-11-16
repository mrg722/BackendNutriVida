package cl.duoc.nutrivida.citas.controller;

import cl.duoc.nutrivida.citas.dto.MedicionRequest;
import cl.duoc.nutrivida.citas.dto.MedicionResponse;
import cl.duoc.nutrivida.citas.service.MedicionService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Ficha clínica digital: aquí el nutricionista registra peso, talla y cintura, y el paciente
// consulta su progreso y su plan alimenticio desde cualquier dispositivo.
@RestController
@RequestMapping("/api/v1/mediciones")
@Tag(name = "Mediciones", description = "Ficha clínica: mediciones, IMC y plan alimenticio")
public class MedicionController {

    private final MedicionService service;

    public MedicionController(MedicionService service) {
        this.service = service;
    }

    // El paciente pide su propio historial sin tener que indicar su correo.
    @GetMapping("/mias")
    @Operation(summary = "Devuelve el historial de mediciones del paciente de la sesión")
    public List<MedicionResponse> listarMias() {
        return service.listarMias();
    }

    // Lo que el profesional de la sesión registró: es su carpeta de fichas.
    @GetMapping("/registradas")
    @Operation(summary = "Devuelve las mediciones registradas por el nutricionista de la sesión")
    public List<MedicionResponse> listarRegistradasPorMi() {
        return service.listarRegistradasPorMi();
    }

    // Ficha de un paciente concreto. El servicio revisa si quien pregunta tiene derecho a verla.
    @GetMapping("/paciente/{correo}")
    @Operation(summary = "Devuelve la ficha de un paciente, si el usuario tiene permiso")
    public List<MedicionResponse> listarPorPaciente(@PathVariable String correo) {
        return service.listarPorPaciente(correo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca una medición por su id")
    public MedicionResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Registra una medición nueva en la ficha de un paciente")
    public ResponseEntity<MedicionResponse> registrar(@Valid @RequestBody MedicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Corrige una medición ya registrada")
    public MedicionResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody MedicionRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una medición de la ficha")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
