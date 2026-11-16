package cl.duoc.nutrivida.citas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

// Este es el cuerpo con el que un paciente pide una hora, o con el que reprograma una ya existente.
// A propósito no incluyo el correo del paciente ni el estado: esos los pongo yo en el servidor
// a partir del token, para que nadie pueda agendar a nombre de otra persona ni autoconfirmarse.
public record CitaRequest(
        @NotNull(message = "Debes elegir un nutricionista")
        Long nutricionistaId,

        @NotNull(message = "Debes elegir un servicio")
        Long servicioId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        // Pido un motivo con algo de contenido, igual que el formulario de React.
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 300, message = "El motivo debe tener entre 10 y 300 caracteres")
        String motivo) {
}
