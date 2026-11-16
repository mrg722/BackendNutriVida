package cl.duoc.nutrivida.citas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// Este es el cuerpo con el que un nutricionista registra una sesión en la ficha del paciente.
// No incluyo su propio correo: ese lo tomo del token, así la ficha queda firmada por quien la escribió.
public record MedicionRequest(
        @NotBlank(message = "El correo del paciente es obligatorio")
        @Email(message = "El correo del paciente no tiene un formato válido")
        String correoPaciente,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        // Acoto los rangos a valores humanos posibles para atajar errores de tipeo.
        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "1.0", message = "El peso debe ser mayor que 1 kilo")
        @DecimalMax(value = "400.0", message = "El peso no puede superar los 400 kilos")
        Double pesoKg,

        @NotNull(message = "La talla es obligatoria")
        @DecimalMin(value = "30.0", message = "La talla debe ser mayor que 30 centímetros")
        @DecimalMax(value = "250.0", message = "La talla no puede superar los 250 centímetros")
        Double tallaCm,

        // La cintura es opcional: no en toda sesión se mide.
        @DecimalMin(value = "20.0", message = "La cintura debe ser mayor que 20 centímetros")
        @DecimalMax(value = "250.0", message = "La cintura no puede superar los 250 centímetros")
        Double cinturaCm,

        @Size(max = 400, message = "Las observaciones no pueden superar los 400 caracteres")
        String observaciones,

        @Size(max = 1000, message = "El plan alimenticio no puede superar los 1000 caracteres")
        String planAlimenticio) {
}
