package cl.duoc.nutrivida.auth.dto;

// Devuelvo el token y los datos mínimos de la sesión. Nunca devuelvo el hash de la contraseña.
public record LoginResponse(
        String token,
        String correo,
        String nombre,
        String rol) {
}
