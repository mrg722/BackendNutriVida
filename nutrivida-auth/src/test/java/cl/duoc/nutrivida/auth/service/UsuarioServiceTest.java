package cl.duoc.nutrivida.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.nutrivida.auth.dto.UsuarioRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.exception.CorreoDuplicadoException;
import cl.duoc.nutrivida.auth.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import cl.duoc.nutrivida.auth.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

// Pruebo el CRUD de usuarios del panel de administración, que es donde se asignan los roles.
class UsuarioServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void prepararDobles() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UsuarioService(repository, encoder);
    }

    private Usuario existente() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNombre("Paciente de prueba");
        usuario.setCorreo("paciente@nutrivida.cl");
        usuario.setPassword("$2a$10$hashOriginal");
        usuario.setRol(Rol.PACIENTE);
        usuario.setActivo(true);
        return usuario;
    }

    @Test
    @DisplayName("Rechazo un rol que no existe en el sistema")
    void rolInventado() {
        UsuarioRequest peticion = new UsuarioRequest(
                "Alguien", "alguien@nutrivida.cl", "Clave123", "SUPERVISOR", true);

        // Compruebo también que el mensaje nombra los tres roles válidos.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crear(peticion));
        assertEquals("El rol debe ser ADMIN, NUTRICIONISTA o PACIENTE", error.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Al crear exijo contraseña, porque no hay ninguna anterior que conservar")
    void creacionSinContrasena() {
        UsuarioRequest peticion = new UsuarioRequest(
                "Alguien", "alguien@nutrivida.cl", "   ", Rol.NUTRICIONISTA, true);

        assertThrows(IllegalArgumentException.class, () -> service.crear(peticion));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("No permito dos usuarios con el mismo correo")
    void creacionConCorreoRepetido() {
        when(repository.existsByCorreoIgnoreCase("alguien@nutrivida.cl")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.crear(new UsuarioRequest(
                "Alguien", "alguien@nutrivida.cl", "Clave123", Rol.NUTRICIONISTA, true)));
    }

    @Test
    @DisplayName("Si la contraseña llega vacía al editar, conservo la que ya estaba guardada")
    void edicionSinCambiarContrasena() {
        Usuario usuario = existente();
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuario.class))).thenAnswer(l -> l.getArgument(0));

        service.actualizar(5L, new UsuarioRequest(
                "Paciente Editado", "paciente@nutrivida.cl", "", Rol.PACIENTE, true));

        assertEquals("$2a$10$hashOriginal", usuario.getPassword());
        assertEquals("Paciente Editado", usuario.getNombre());
        // Si no hay contraseña nueva, no tiene sentido llamar al codificador.
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Si llega una contraseña nueva, la guardo codificada y no en texto plano")
    void edicionCambiandoContrasena() {
        Usuario usuario = existente();
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(encoder.encode("ClaveNueva")).thenReturn("$2a$10$hashNuevo");
        when(repository.save(any(Usuario.class))).thenAnswer(l -> l.getArgument(0));

        service.actualizar(5L, new UsuarioRequest(
                "Paciente de prueba", "paciente@nutrivida.cl", "ClaveNueva", Rol.PACIENTE, true));

        assertEquals("$2a$10$hashNuevo", usuario.getPassword());
    }

    @Test
    @DisplayName("El panel puede desactivar una cuenta y cambiarle el rol sin borrarla")
    void edicionDesactivaYCambiaRol() {
        Usuario usuario = existente();
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuario.class))).thenAnswer(l -> l.getArgument(0));

        UsuarioResponse respuesta = service.actualizar(5L, new UsuarioRequest(
                "Paciente de prueba", "paciente@nutrivida.cl", "", Rol.NUTRICIONISTA, false));

        assertEquals(Rol.NUTRICIONISTA, respuesta.rol());
        assertFalse(respuesta.activo());
        // El id original se conserva: editar no crea un registro nuevo.
        assertEquals(5L, respuesta.id());
    }

    @Test
    @DisplayName("Editar el correo de un usuario a uno ya tomado da conflicto")
    void edicionConCorreoDeOtro() {
        when(repository.findById(5L)).thenReturn(Optional.of(existente()));
        when(repository.existsByCorreoIgnoreCase("admin@nutrivida.cl")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.actualizar(5L,
                new UsuarioRequest("Paciente", "admin@nutrivida.cl", "", Rol.PACIENTE, true)));
    }

    @Test
    @DisplayName("Pedir o eliminar un usuario que no existe responde no encontrado")
    void usuarioInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscar(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));
        verify(repository, never()).delete(any());
    }
}
