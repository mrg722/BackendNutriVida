package cl.duoc.nutrivida.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.nutrivida.auth.dto.LoginRequest;
import cl.duoc.nutrivida.auth.dto.LoginResponse;
import cl.duoc.nutrivida.auth.dto.RegistroRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.exception.CorreoDuplicadoException;
import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import cl.duoc.nutrivida.auth.repository.UsuarioRepository;
import cl.duoc.nutrivida.auth.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

// Pruebo la lógica del inicio de sesión aislada de la base de datos: reemplazo el repositorio,
// el codificador y el emisor de tokens por dobles, así compruebo solo las reglas de negocio.
class AuthServiceTest {

    private UsuarioRepository usuarios;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthService service;

    @BeforeEach
    void prepararDobles() {
        // Creo los dobles a mano para no depender de extensiones adicionales de Mockito.
        usuarios = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        service = new AuthService(usuarios, encoder, jwt);
    }

    // Este ayudante me ahorra repetir la construcción del usuario en cada prueba.
    private Usuario usuario(String correo, String rol, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPassword("$2a$10$hashDePrueba");
        usuario.setRol(rol);
        usuario.setActivo(activo);
        return usuario;
    }

    @Test
    @DisplayName("Con credenciales correctas devuelvo el token y el rol")
    void loginCorrecto() {
        Usuario admin = usuario("admin@nutrivida.cl", Rol.ADMIN, true);
        when(usuarios.findByCorreoIgnoreCase("admin@nutrivida.cl")).thenReturn(Optional.of(admin));
        when(encoder.matches("Clave123", admin.getPassword())).thenReturn(true);
        when(jwt.generar(admin)).thenReturn("token-firmado");

        LoginResponse respuesta = service.login(new LoginRequest("admin@nutrivida.cl", "Clave123"));

        assertEquals("token-firmado", respuesta.token());
        assertEquals("admin@nutrivida.cl", respuesta.correo());
        assertEquals(Rol.ADMIN, respuesta.rol());
    }

    @Test
    @DisplayName("Si la contraseña no calza no emito ningún token")
    void loginConClaveIncorrecta() {
        Usuario admin = usuario("admin@nutrivida.cl", Rol.ADMIN, true);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(admin));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("admin@nutrivida.cl", "equivocada")));

        // Compruebo que ni siquiera se intentó firmar un token.
        verify(jwt, never()).generar(any());
    }

    @Test
    @DisplayName("Un correo que no existe falla igual que una contraseña mala")
    void loginConCorreoInexistente() {
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Uso la misma excepción a propósito: así la API no revela qué correos están registrados.
        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("nadie@nutrivida.cl", "Clave123")));
    }

    @Test
    @DisplayName("Una cuenta desactivada no puede entrar aunque la clave sea correcta")
    void loginConCuentaDesactivada() {
        Usuario desactivado = usuario("paciente@nutrivida.cl", Rol.PACIENTE, false);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(desactivado));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("paciente@nutrivida.cl", "Clave123")));
        verify(jwt, never()).generar(any());
    }

    @Test
    @DisplayName("El registro público siempre crea un paciente y guarda la clave como hash")
    void registroPublicoFuerzaRolPaciente() {
        when(usuarios.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(false);
        when(encoder.encode("Clave123")).thenReturn("$2a$10$hashNuevo");
        // Devuelvo el mismo objeto que recibo, como haría el repositorio real al guardar.
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        UsuarioResponse respuesta = service.registrar(
                new RegistroRequest("Ana Pérez", "ANA@gmail.com", "Clave123"));

        // Capturo lo que se guardó para revisar el rol y la contraseña.
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());

        assertEquals(Rol.PACIENTE, guardado.getValue().getRol());
        assertEquals("$2a$10$hashNuevo", guardado.getValue().getPassword());
        // Compruebo también que normalizo el correo a minúsculas.
        assertEquals("ana@gmail.com", guardado.getValue().getCorreo());
        assertEquals(Rol.PACIENTE, respuesta.rol());
    }

    @Test
    @DisplayName("No permito registrar un correo que ya existe")
    void registroConCorreoDuplicado() {
        when(usuarios.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.registrar(
                new RegistroRequest("Ana Pérez", "ana@gmail.com", "Clave123")));
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("La renovación entrega un token nuevo leyendo el rol otra vez de la base")
    void renovacionLeeElRolActual() {
        // Simulo que el administrador le cambió el rol después de emitir el token anterior.
        Usuario ascendido = usuario("carolina.fuentes@nutrivida.cl", Rol.ADMIN, true);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(ascendido));
        when(jwt.generar(ascendido)).thenReturn("token-renovado");

        LoginResponse respuesta = service.renovar("carolina.fuentes@nutrivida.cl");

        assertEquals("token-renovado", respuesta.token());
        // El token nuevo lleva el rol actual, no el que traía el token viejo.
        assertEquals(Rol.ADMIN, respuesta.rol());
    }

    @Test
    @DisplayName("No renuevo la sesión de una cuenta que fue desactivada")
    void renovacionDeCuentaDesactivada() {
        Usuario desactivado = usuario("paciente@nutrivida.cl", Rol.PACIENTE, false);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(desactivado));

        assertThrows(BadCredentialsException.class,
                () -> service.renovar("paciente@nutrivida.cl"));
        verify(jwt, never()).generar(any());
    }
}
