package cl.duoc.nutrivida.auth.security;

import cl.duoc.nutrivida.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Este servicio es el único lugar del sistema donde se firma un token.
@Service
public class JwtService {

    // Leo la clave y los tiempos desde la configuración: no los escribo dentro del código.
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    // Convierto el texto Base64 de la configuración en la clave HMAC con la que firmo.
    private SecretKey clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // Aquí construyo el token con los datos que nutrivida-citas necesita para autorizar.
    public String generar(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                // Declaro quién emite el token para que el otro microservicio pueda exigirlo.
                .issuer(issuer)
                // Uso el correo como identidad de la persona dentro del token.
                .subject(usuario.getCorreo())
                // Agrego rol y nombre como claims: con eso el frontend arma su sesión.
                .claim("rol", usuario.getRol())
                .claim("nombre", usuario.getNombre())
                .issuedAt(ahora)
                // Le pongo vencimiento para que un token filtrado no sirva para siempre.
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(clave())
                .compact();
    }

    // También valido tokens aquí, porque el panel de usuarios de este microservicio está protegido.
    public Claims validar(String token) {
        return Jwts.parser()
                // Compruebo la firma con la misma clave con la que firmé.
                .verifyWith(clave())
                // Rechazo cualquier token que diga venir de otra aplicación.
                .requireIssuer(issuer)
                .build()
                // Este paso revisa además la estructura y la fecha de expiración.
                .parseSignedClaims(token)
                .getPayload();
    }
}
