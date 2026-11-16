package cl.duoc.nutrivida.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Este record describe el JSON que recibo cuando alguien intenta iniciar sesión.
public record LoginRequest(
        // Exijo que venga un correo con formato válido.
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        // La contraseña solo tiene que venir presente: su validez la comprueba BCrypt.
        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}
