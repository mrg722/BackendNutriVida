package cl.duoc.nutrivida.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Este es el cuerpo del registro público: con él una persona crea su cuenta de paciente.
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        // Mantengo el mismo largo mínimo que ya valida el formulario de React.
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres")
        String password) {
}
