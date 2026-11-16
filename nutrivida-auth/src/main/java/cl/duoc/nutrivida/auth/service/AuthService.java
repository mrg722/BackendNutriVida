package cl.duoc.nutrivida.auth.service;

import cl.duoc.nutrivida.auth.dto.LoginRequest;
import cl.duoc.nutrivida.auth.dto.LoginResponse;
import cl.duoc.nutrivida.auth.dto.RegistroRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.exception.CorreoDuplicadoException;
import cl.duoc.nutrivida.auth.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import cl.duoc.nutrivida.auth.repository.UsuarioRepository;
import cl.duoc.nutrivida.auth.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Aquí concentro la lógica de negocio de la autenticación: el controlador solo expone HTTP.
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    // Recibo las tres dependencias por constructor para que Spring las inyecte y pueda probarlas.
    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // Compruebo las credenciales y, si están bien, devuelvo el token con el rol dentro.
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(request.correo())
                // Uso el mismo error que una clave mala para no delatar qué correos existen.
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        // matches compara el texto recibido contra el hash guardado, sin descifrar nada.
        if (!encoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Una cuenta desactivada por el administrador no debe poder entrar aunque la clave sea correcta.
        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        return new LoginResponse(jwt.generar(usuario), usuario.getCorreo(),
                usuario.getNombre(), usuario.getRol());
    }

    // Registro público: quien se inscribe desde la web siempre queda como PACIENTE.
    public UsuarioResponse registrar(RegistroRequest request) {
        String correo = request.correo().trim().toLowerCase();

        // Reviso antes de guardar para devolver un 409 claro en vez de un error de base de datos.
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("El correo ya está registrado: " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        // Guardo el hash BCrypt, nunca la contraseña tal como la escribió la persona.
        usuario.setPassword(encoder.encode(request.password()));
        // No dejo que el cuerpo de la petición elija el rol: así nadie se crea una cuenta de ADMIN.
        usuario.setRol(Rol.PACIENTE);
        usuario.setActivo(true);

        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    // Con esto el frontend revalida la sesión al recargar la página: le devuelvo quién es el dueño del token.
    public UsuarioResponse perfil(String correo) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correo));
        return UsuarioResponse.desde(usuario);
    }

    // Renovación del token. El frontend la llama cuando la sesión está por vencer, para que a
    // la persona no se le corte el trabajo a mitad de camino.
    public LoginResponse renovar(String correo) {
        // Solo llego hasta aquí si el filtro ya validó la firma, el emisor y la expiración del
        // token actual: nunca emito un token nuevo a partir de uno vencido o alterado.
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        // Releo la cuenta desde la base en vez de confiar en los datos del token viejo. Así, si
        // el administrador desactivó la cuenta o le cambió el rol, la renovación lo respeta.
        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        return new LoginResponse(jwt.generar(usuario), usuario.getCorreo(),
                usuario.getNombre(), usuario.getRol());
    }
}
