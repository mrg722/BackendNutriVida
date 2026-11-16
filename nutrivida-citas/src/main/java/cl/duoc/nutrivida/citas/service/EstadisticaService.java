package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.dto.EstadisticasResponse;
import cl.duoc.nutrivida.citas.model.EstadoCita;
import cl.duoc.nutrivida.citas.repository.CitaRepository;
import cl.duoc.nutrivida.citas.repository.NutricionistaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

// El caso dice que hoy no hay estadísticas sin revisar las agendas página por página.
// Este servicio resuelve eso: calcula los números que pide el resultado esperado del proyecto.
@Service
public class EstadisticaService {

    private final CitaRepository citas;
    private final NutricionistaRepository nutricionistas;

    public EstadisticaService(CitaRepository citas, NutricionistaRepository nutricionistas) {
        this.citas = citas;
        this.nutricionistas = nutricionistas;
    }

    public EstadisticasResponse calcular() {
        long total = citas.count();

        // Uso LinkedHashMap para que los estados salgan siempre en el orden del flujo.
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String estado : EstadoCita.todos()) {
            porEstado.put(estado, citas.countByEstado(estado));
        }

        // Tasa de inasistencia: el 20 % del que habla el caso, ahora medido de verdad.
        long noAsistio = porEstado.getOrDefault(EstadoCita.NO_ASISTIO, 0L);
        // Me protejo de dividir por cero cuando todavía no hay ninguna cita registrada.
        double tasa = total == 0 ? 0.0 : (noAsistio * 100.0) / total;
        // Redondeo a un decimal para mostrarlo como porcentaje legible.
        double tasaRedondeada = Math.round(tasa * 10.0) / 10.0;

        // Distribución por profesional: así se ve la carga de trabajo de cada agenda.
        List<EstadisticasResponse.AtencionesPorNutricionista> porNutricionista =
                nutricionistas.findAll().stream()
                        .map(profesional -> new EstadisticasResponse.AtencionesPorNutricionista(
                                profesional.getId(),
                                profesional.getNombre(),
                                citas.countByNutricionista_Id(profesional.getId())))
                        .toList();

        return new EstadisticasResponse(total, porEstado, tasaRedondeada, porNutricionista);
    }
}
