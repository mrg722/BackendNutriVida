package cl.duoc.nutrivida.citas.config;

import cl.duoc.nutrivida.citas.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Aquí decido, ruta por ruta y método por método, qué rol puede entrar. Esta es la autorización
// real del sistema: aunque alguien edite el sessionStorage del navegador, no pasa de aquí.
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
                // Sin CSRF porque no uso cookies de sesión: la credencial es el token de cada petición.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // STATELESS: el servidor no recuerda nada entre peticiones.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Dejo pasar el preflight del navegador.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Documentación abierta para poder revisar y probar el contrato.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        // Dejo pasar el despacho interno de error de Tomcat. Cuando Spring
                        // deniega por rol llama a sendError(403), y eso reenvía la petición
                        // a /error: si esa segunda vuelta no estuviera permitida, caería en el
                        // denyAll de abajo ya sin identidad y el 403 se convertiría en un 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Catálogo: cualquiera con sesión lo lee; solo ADMIN lo modifica.
                        .requestMatchers(HttpMethod.GET, "/api/v1/servicios/**").authenticated()
                        .requestMatchers("/api/v1/servicios/**").hasRole("ADMIN")

                        // Directorio de profesionales: misma regla que el catálogo.
                        .requestMatchers(HttpMethod.GET, "/api/v1/nutricionistas/**").authenticated()
                        .requestMatchers("/api/v1/nutricionistas/**").hasRole("ADMIN")

                        // Pedir hora es propio del paciente; el administrador también puede agendar.
                        .requestMatchers(HttpMethod.POST, "/api/v1/citas")
                        .hasAnyRole("PACIENTE", "ADMIN")
                        // El listado completo de la clínica es solo del administrador. Lo declaro
                        // con la ruta exacta para no tapar /api/v1/citas/mias, que sí es de todos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/citas").hasRole("ADMIN")
                        // Borrar una cita del historial queda solo en manos del administrador.
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/citas/**").hasRole("ADMIN")
                        // El resto de las citas exige sesión; quién puede ver o cambiar cada una
                        // lo reviso en CitaService, porque depende de si la persona es su dueña.
                        .requestMatchers("/api/v1/citas/**").authenticated()

                        // Escribir en la ficha clínica es tarea del profesional.
                        .requestMatchers(HttpMethod.POST, "/api/v1/mediciones")
                        .hasAnyRole("NUTRICIONISTA", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/mediciones/**")
                        .hasAnyRole("NUTRICIONISTA", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/mediciones/**")
                        .hasAnyRole("NUTRICIONISTA", "ADMIN")
                        // Leer la ficha exige sesión y, además, ser su dueño o su nutricionista.
                        .requestMatchers("/api/v1/mediciones/**").authenticated()

                        // Las estadísticas de la clínica son solo para el administrador.
                        .requestMatchers("/api/v1/estadisticas/**").hasRole("ADMIN")

                        // Cierro cualquier ruta que no haya declarado.
                        .anyRequest().denyAll())
                // Sin token válido respondo 401; con token pero sin permiso, Spring responde 403.
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuracion = new CorsConfiguration();
        // Autorizo un origen concreto y nunca el comodín, para no abrir la API a cualquier sitio.
        configuracion.setAllowedOrigins(List.of(allowedOrigin));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permito Authorization porque por ese encabezado viaja el Bearer.
        configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuracion);
        return source;
    }
}
