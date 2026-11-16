package cl.duoc.nutrivida.auth.dto;

import cl.duoc.nutrivida.auth.model.Usuario;

// Esta es la vista pública de un usuario: la armo a mano para dejar fuera la contraseña.
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rol,
        boolean activo) {

    // Traduzco la entidad a este record para que el hash nunca salga en una respuesta JSON.
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isActivo());
    }
}
