package cl.duoc.nutrivida.auth.controller;

import cl.duoc.nutrivida.auth.dto.LoginRequest;
import cl.duoc.nutrivida.auth.dto.LoginResponse;
import cl.duoc.nutrivida.auth.dto.RegistroRequest;
import cl.duoc.nutrivida.auth.dto.UsuarioResponse;
import cl.duoc.nutrivida.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Expongo las rutas de autenticación. Uso /api/v1 porque el Anexo 1 pide versionar la API.
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro público y perfil de la sesión")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // Esta es la única llamada que el frontend necesita para obtener el token.
    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un token JWT con el rol del usuario")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    // Registro público de pacientes: respondo 201 porque se creó un recurso nuevo.
    @PostMapping("/registro")
    @Operation(summary = "Registra una cuenta nueva de paciente")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(request));
    }

    // El frontend llama aquí al recargar la página para comprobar que el token sigue siendo válido.
    @GetMapping("/perfil")
    @Operation(summary = "Devuelve los datos del usuario dueño del token recibido")
    public UsuarioResponse perfil(Principal principal) {
        // Principal trae el correo que el filtro JWT dejó en el contexto de seguridad.
        return auth.perfil(principal.getName());
    }
}
