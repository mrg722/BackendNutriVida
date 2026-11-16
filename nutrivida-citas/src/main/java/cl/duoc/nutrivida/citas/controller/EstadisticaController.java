package cl.duoc.nutrivida.citas.controller;

import cl.duoc.nutrivida.citas.dto.EstadisticasResponse;
import cl.duoc.nutrivida.citas.service.EstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Resumen de la operación de la clínica. SecurityConfig reserva esta ruta al rol ADMIN.
@RestController
@RequestMapping("/api/v1/estadisticas")
@Tag(name = "Estadísticas", description = "Atenciones, tasa de inasistencia y carga por profesional")
public class EstadisticaController {

    private final EstadisticaService service;

    public EstadisticaController(EstadisticaService service) {
        this.service = service;
    }

    // Una sola llamada entrega los tres indicadores que pide el resultado esperado del caso.
    @GetMapping
    @Operation(summary = "Devuelve el resumen de citas, la tasa de inasistencia y las "
            + "atenciones por nutricionista")
    public EstadisticasResponse obtener() {
        return service.calcular();
    }
}
