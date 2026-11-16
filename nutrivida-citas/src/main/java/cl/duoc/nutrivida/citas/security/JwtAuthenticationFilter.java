package cl.duoc.nutrivida.citas.security;

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

// Este filtro construye la identidad de la petición a partir de claims ya verificados.
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

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                // Quito el texto "Bearer " y valido lo que queda.
                Claims claims = jwt.validar(header.substring(7));
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);
                // El correo queda como nombre de la identidad: con él compruebo después
                // si la persona es dueña de la cita o de la ficha que está pidiendo.
                var autenticacion = new UsernamePasswordAuthenticationToken(correo, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException | IllegalArgumentException e) {
                // Token alterado, vencido o de otro emisor: dejo la petición sin identidad.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
