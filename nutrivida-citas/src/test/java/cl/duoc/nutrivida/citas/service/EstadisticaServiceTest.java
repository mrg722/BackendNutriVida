package cl.duoc.nutrivida.citas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.duoc.nutrivida.citas.dto.EstadisticasResponse;
import cl.duoc.nutrivida.citas.model.EstadoCita;
import cl.duoc.nutrivida.citas.model.Nutricionista;
import cl.duoc.nutrivida.citas.repository.CitaRepository;
import cl.duoc.nutrivida.citas.repository.NutricionistaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Pruebo el cálculo de los indicadores que el caso pide: atenciones, tasa de inasistencia
// y distribución por profesional.
class EstadisticaServiceTest {

    private CitaRepository citas;
    private NutricionistaRepository nutricionistas;
    private EstadisticaService service;

    @BeforeEach
    void prepararDobles() {
        citas = mock(CitaRepository.class);
        nutricionistas = mock(NutricionistaRepository.class);
        service = new EstadisticaService(citas, nutricionistas);
    }

    private Nutricionista profesional(Long id, String nombre) {
        Nutricionista profesional = new Nutricionista();
        profesional.setId(id);
        profesional.setNombre(nombre);
        return profesional;
    }

    @Test
    @DisplayName("Calculo la tasa de inasistencia como porcentaje del total")
    void tasaDeInasistencia() {
        when(citas.count()).thenReturn(50L);
        when(citas.countByEstado(EstadoCita.SOLICITADA)).thenReturn(5L);
        when(citas.countByEstado(EstadoCita.CONFIRMADA)).thenReturn(10L);
        when(citas.countByEstado(EstadoCita.ATENDIDA)).thenReturn(25L);
        when(citas.countByEstado(EstadoCita.CANCELADA)).thenReturn(0L);
        when(citas.countByEstado(EstadoCita.NO_ASISTIO)).thenReturn(10L);
        when(nutricionistas.findAll()).thenReturn(List.of());

        EstadisticasResponse respuesta = service.calcular();

        assertEquals(50L, respuesta.totalCitas());
        // 10 inasistencias sobre 50 citas es el 20 %, justo el problema que describe el caso.
        assertEquals(20.0, respuesta.tasaInasistencia());
        assertEquals(25L, respuesta.citasPorEstado().get(EstadoCita.ATENDIDA));
    }

    @Test
    @DisplayName("Redondeo la tasa a un decimal")
    void tasaRedondeada() {
        when(citas.count()).thenReturn(7L);
        when(citas.countByEstado(EstadoCita.NO_ASISTIO)).thenReturn(1L);
        when(nutricionistas.findAll()).thenReturn(List.of());

        // 1 de 7 es 14,2857...; lo informo como 14.3.
        assertEquals(14.3, service.calcular().tasaInasistencia());
    }

    @Test
    @DisplayName("Sin ninguna cita registrada la tasa es cero y no divido por cero")
    void sinCitasNoHayDivisionPorCero() {
        when(citas.count()).thenReturn(0L);
        when(nutricionistas.findAll()).thenReturn(List.of());

        EstadisticasResponse respuesta = service.calcular();

        assertEquals(0L, respuesta.totalCitas());
        assertEquals(0.0, respuesta.tasaInasistencia());
    }

    @Test
    @DisplayName("Entrego los cinco estados del flujo, incluso los que están en cero")
    void todosLosEstadosAparecen() {
        when(citas.count()).thenReturn(1L);
        when(nutricionistas.findAll()).thenReturn(List.of());

        EstadisticasResponse respuesta = service.calcular();

        // Así el panel siempre dibuja las mismas cinco columnas y no cambia de forma.
        assertEquals(5, respuesta.citasPorEstado().size());
        assertEquals(EstadoCita.todos(), List.copyOf(respuesta.citasPorEstado().keySet()));
    }

    @Test
    @DisplayName("Cuento las atenciones de cada profesional por separado")
    void atencionesPorProfesional() {
        when(citas.count()).thenReturn(12L);
        when(nutricionistas.findAll()).thenReturn(List.of(
                profesional(1L, "Nut. Carolina Fuentes M."),
                profesional(2L, "Nut. Rodrigo Sepúlveda A.")));
        when(citas.countByNutricionista_Id(1L)).thenReturn(8L);
        when(citas.countByNutricionista_Id(2L)).thenReturn(4L);

        List<EstadisticasResponse.AtencionesPorNutricionista> carga =
                service.calcular().atencionesPorNutricionista();

        assertEquals(2, carga.size());
        assertEquals("Nut. Carolina Fuentes M.", carga.get(0).nombre());
        assertEquals(8L, carga.get(0).total());
        assertEquals(4L, carga.get(1).total());
    }
}
