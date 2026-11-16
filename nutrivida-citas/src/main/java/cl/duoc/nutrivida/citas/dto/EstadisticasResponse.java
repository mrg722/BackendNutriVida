package cl.duoc.nutrivida.citas.dto;

import java.util.List;
import java.util.Map;

// Esta es la respuesta del panel de estadísticas que pide el caso: el administrador necesitaba
// revisar las agendas página por página, y aquí le entrego los tres números ya calculados.
public record EstadisticasResponse(
        long totalCitas,
        // Cuántas citas hay en cada estado del flujo.
        Map<String, Long> citasPorEstado,
        // Porcentaje de citas en las que el paciente no llegó.
        double tasaInasistencia,
        // Cuántas citas acumula cada profesional, para ver la carga de trabajo.
        List<AtencionesPorNutricionista> atencionesPorNutricionista) {

    // Uso un record anidado porque este dato solo tiene sentido dentro de las estadísticas.
    public record AtencionesPorNutricionista(
            Long nutricionistaId,
            String nombre,
            long total) {
    }
}
