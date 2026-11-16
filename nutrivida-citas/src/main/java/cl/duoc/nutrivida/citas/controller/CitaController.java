package cl.duoc.nutrivida.citas.controller;

import cl.duoc.nutrivida.citas.dto.CambioEstadoRequest;
import cl.duoc.nutrivida.citas.dto.CitaRequest;
import cl.duoc.nutrivida.citas.dto.CitaResponse;
import cl.duoc.nutrivida.citas.service.CitaService;
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

// Flujo completo de la cita, que es el "pedido" de este contexto: el paciente la solicita,
// la clínica la confirma, y cualquiera de los dos puede cancelarla o reprogramarla.
@RestController
@RequestMapping("/api/v1/citas")
@Tag(name = "Citas", description = "Solicitud, confirmación, reprogramación y cancelación de horas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    // Esta ruta devuelve lo que corresponde a quien pregunta: el paciente sus citas, el
    // nutricionista su agenda y el administrador todas. La decisión la toma el servicio.
    @GetMapping("/mias")
    @Operation(summary = "Devuelve las citas del usuario de la sesión según su rol")
    public List<CitaResponse> listarMias() {
        return service.listarMias();
    }

    @GetMapping
    @Operation(summary = "Lista todas las citas de la clínica")
    public List<CitaResponse> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca una cita por su id, si el usuario tiene permiso para verla")
    public CitaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    // El paciente pide la hora. Queda en SOLICITADA hasta que la clínica la confirme.
    @PostMapping
    @Operation(summary = "Solicita una hora nueva")
    public ResponseEntity<CitaResponse> solicitar(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitar(request));
    }

    // Reprogramar: cambio los datos de la hora y la cita vuelve a quedar por confirmar.
    @PutMapping("/{id}")
    @Operation(summary = "Reprograma una cita y la deja otra vez en estado SOLICITADA")
    public CitaResponse reprogramar(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        return service.reprogramar(id, request);
    }

    // Separo el cambio de estado en su propia ruta para que confirmar o cancelar no permita
    // modificar de paso la fecha ni el profesional.
    @PutMapping("/{id}/estado")
    @Operation(summary = "Confirma, atiende, cancela o marca la inasistencia de una cita")
    public CitaResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una cita del historial")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
