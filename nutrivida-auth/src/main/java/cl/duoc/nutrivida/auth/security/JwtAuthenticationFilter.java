package cl.duoc.nutrivida.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Este filtro corre una vez por petición y traduce el token recibido en una identidad de Spring.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Busco el token en el encabezado Authorization, con el esquema Bearer.
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                // Recorto los siete caracteres del esquema y me queda solo el token.
                Claims claims = jwt.validar(header.substring(7));
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);
                // Spring espera el prefijo ROLE_, así que convierto ADMIN en ROLE_ADMIN.
                var autenticacion = new UsernamePasswordAuthenticationToken(correo, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException | IllegalArgumentException e) {
                // Si el token viene alterado o vencido dejo la petición sin identidad: responderá 401.
                SecurityContextHolder.clearContext();
            }
        }
        // Sigo la cadena siempre: decidir si la petición pasa o no es tarea de SecurityConfig.
        chain.doFilter(request, response);
    }
}
