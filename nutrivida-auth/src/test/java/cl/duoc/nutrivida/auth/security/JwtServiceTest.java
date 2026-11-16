package cl.duoc.nutrivida.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Pruebo la firma y la verificación del token, que es el centro de la seguridad del sistema.
class JwtServiceTest {

    // Clave de prueba en Base64. Al decodificarse supera los 32 bytes que exige HS256.
    private static final String CLAVE =
            "bnV0cml2aWRhLWRlc2Fycm9sbG8tY2xhdmUtbG9jYWwtc29sby1wYXJhLXBydWViYXM=";
    private static final String OTRA_CLAVE =
            "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLXByb2Jhci1lbC1yZWNoYXpvLWRlLWZpcm1h";

    private JwtService service;

    @BeforeEach
    void prepararServicio() {
        service = construir(CLAVE, "nutrivida-auth", 3600000L);
    }

    // Los valores llegan por @Value, así que en la prueba los inyecto directamente.
    private JwtService construir(String clave, String emisor, long duracionMs) {
        JwtService nuevo = new JwtService();
        ReflectionTestUtils.setField(nuevo, "secret", clave);
        ReflectionTestUtils.setField(nuevo, "issuer", emisor);
        ReflectionTestUtils.setField(nuevo, "expirationMs", duracionMs);
        return nuevo;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("carolina.fuentes@nutrivida.cl");
        usuario.setNombre("Nut. Carolina Fuentes M.");
        usuario.setRol(Rol.NUTRICIONISTA);
        return usuario;
    }

    @Test
    @DisplayName("El token que firmo lo puedo verificar y trae el correo y el rol")
    void firmaYVerificacion() {
        String token = service.generar(usuario());
        Claims claims = service.validar(token);

        assertEquals("carolina.fuentes@nutrivida.cl", claims.getSubject());
        assertEquals(Rol.NUTRICIONISTA, claims.get("rol", String.class));
        assertEquals("Nut. Carolina Fuentes M.", claims.get("nombre", String.class));
        assertEquals("nutrivida-auth", claims.getIssuer());
        // El token tiene que venir con fecha de vencimiento, no vivir para siempre.
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    @DisplayName("Rechazo un token al que le alteraron la firma")
    void tokenAlterado() {
        String token = service.generar(usuario());
        // Le cambio el último carácter, que es parte de la firma.
        String alterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThrows(JwtException.class, () -> service.validar(alterado));
    }

    @Test
    @DisplayName("Rechazo un token firmado con otra clave")
    void tokenDeOtraClave() {
        JwtService otroEmisor = construir(OTRA_CLAVE, "nutrivida-auth", 3600000L);
        String token = otroEmisor.generar(usuario());

        // Aunque el contenido sea idéntico, la firma no corresponde a mi clave.
        assertThrows(JwtException.class, () -> service.validar(token));
    }

    @Test
    @DisplayName("Rechazo un token que dice venir de otra aplicación")
    void tokenDeOtroEmisor() {
        JwtService aplicacionAjena = construir(CLAVE, "otra-aplicacion", 3600000L);
        String token = aplicacionAjena.generar(usuario());

        // Esto es lo que impide que un token de otro sistema sirva aquí.
        assertThrows(JwtException.class, () -> service.validar(token));
    }

    @Test
    @DisplayName("Rechazo un token que ya venció")
    void tokenVencido() {
        // Lo emito con duración negativa para que nazca expirado.
        JwtService vencedorInmediato = construir(CLAVE, "nutrivida-auth", -1000L);
        String token = vencedorInmediato.generar(usuario());

        assertThrows(JwtException.class, () -> service.validar(token));
    }
}
