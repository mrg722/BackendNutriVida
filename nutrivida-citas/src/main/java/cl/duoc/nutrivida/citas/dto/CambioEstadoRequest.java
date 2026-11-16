package cl.duoc.nutrivida.citas.dto;

import jakarta.validation.constraints.NotBlank;

// Con este cuerpo mínimo muevo una cita por el flujo: confirmar, atender, cancelar o marcar
// que el paciente no asistió. Lo separo de CitaRequest porque cambiar el estado no debe
// permitir cambiar de paso la fecha, el servicio ni el profesional.
public record CambioEstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado) {
}
