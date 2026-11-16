package cl.duoc.nutrivida.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Con este cuerpo el administrador crea o edita cualquier usuario y le asigna el rol.
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        // Al editar dejo venir la contraseña vacía: en ese caso conservo la que ya estaba.
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        // Con este campo el administrador desactiva una cuenta sin eliminarla.
        boolean activo) {
}
