package cl.duoc.nutrivida.auth.service;

import cl.duoc.nutrivida.auth.dto.UsuarioRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.exception.CorreoDuplicadoException;
import cl.duoc.nutrivida.auth.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import cl.duoc.nutrivida.auth.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Este servicio es el CRUD de usuarios que usa el panel de administración del frontend.
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    // Listo todos los usuarios y los traduzco al record público para no exponer los hash.
    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    // Permito filtrar por rol, que es lo que necesita la vista de nutricionistas del panel.
    public List<UsuarioResponse> listarPorRol(String rol) {
        validarRol(rol);
        return repository.findByRolIgnoreCase(rol).stream().map(UsuarioResponse::desde).toList();
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.desde(obtener(id));
    }

    // Creación desde el panel: aquí sí dejo elegir el rol, porque solo entra un ADMIN.
    public UsuarioResponse crear(UsuarioRequest request) {
        validarRol(request.rol());

        // Al crear exijo contraseña, porque todavía no hay ninguna guardada que conservar.
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria al crear un usuario");
        }

        String correo = request.correo().trim().toLowerCase();
        if (repository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("El correo ya está registrado: " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(encoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());

        return UsuarioResponse.desde(repository.save(usuario));
    }

    // Edición: cambio los datos permitidos y conservo el id original del registro.
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        validarRol(request.rol());
        Usuario usuario = obtener(id);

        String correo = request.correo().trim().toLowerCase();
        // Solo reviso el duplicado si de verdad cambió el correo, para no choquear consigo mismo.
        if (!usuario.getCorreo().equalsIgnoreCase(correo)
                && repository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("El correo ya está registrado: " + correo);
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(request.rol());
        // Este campo es el que usa el panel para desactivar una cuenta sin eliminarla.
        usuario.setActivo(request.activo());

        // Si viene vacía dejo la contraseña anterior: así editar datos no obliga a cambiar la clave.
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(encoder.encode(request.password()));
        }

        return UsuarioResponse.desde(repository.save(usuario));
    }

    // Eliminación definitiva del registro, que es la operación DELETE del contrato REST.
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }

    // Reutilizo esta búsqueda en todos los métodos para lanzar un 404 en un solo lugar.
    private Usuario obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    // Compruebo el rol contra las constantes para que no entre un valor inventado a la base.
    private void validarRol(String rol) {
        if (rol == null || !Rol.esValido(rol)) {
            throw new IllegalArgumentException(
                    "El rol debe ser ADMIN, NUTRICIONISTA o PACIENTE");
        }
    }
}
