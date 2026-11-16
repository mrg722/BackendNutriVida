package cl.duoc.nutrivida.citas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Este servicio solo valida tokens: no genera ninguno ni consulta la tabla de usuarios.
// Esa es justamente la frontera entre los dos microservicios: nutrivida-auth firma, yo verifico.
@Service
public class JwtService {

    // La clave tiene que ser exactamente la misma que usa nutrivida-auth para firmar.
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public Claims validar(String token) {
        return Jwts.parser()
                // Verifico la firma localmente: no necesito llamar a nutrivida-auth en cada petición.
                .verifyWith(clave())
                // Exijo el emisor para rechazar un token firmado por otra aplicación.
                .requireIssuer(issuer)
                .build()
                // Este paso comprueba además la estructura y que el token no esté vencido.
                .parseSignedClaims(token)
                .getPayload();
    }
}
