package cl.duoc.nutrivida.citas.dto;

import cl.duoc.nutrivida.citas.model.Cita;
import java.time.LocalDate;
import java.time.LocalTime;

// Esta es la vista de una cita que devuelvo al frontend. Aplano los datos del nutricionista y
// del servicio para que React pueda mostrar la tarjeta sin tener que pedir dos recursos más.
public record CitaResponse(
        Long id,
        String correoPaciente,
        Long nutricionistaId,
        String nutricionistaNombre,
        String nutricionistaEspecialidad,
        Long servicioId,
        String servicioCodigo,
        String servicioNombre,
        Integer servicioPrecio,
        LocalDate fecha,
        LocalTime hora,
        String motivo,
        String estado) {

    // Traduzco la entidad a este record en un solo lugar, para no repetir el armado en cada servicio.
    public static CitaResponse desde(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getCorreoPaciente(),
                cita.getNutricionista().getId(),
                cita.getNutricionista().getNombre(),
                cita.getNutricionista().getEspecialidad(),
                cita.getServicio().getId(),
                cita.getServicio().getCodigo(),
                cita.getServicio().getNombre(),
                cita.getServicio().getPrecio(),
                cita.getFecha(),
                cita.getHora(),
                cita.getMotivo(),
                cita.getEstado());
    }
}
