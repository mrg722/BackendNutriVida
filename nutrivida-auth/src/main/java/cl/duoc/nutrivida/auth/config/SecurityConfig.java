package cl.duoc.nutrivida.auth.config;

import cl.duoc.nutrivida.auth.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Aquí defino qué rutas de este microservicio son públicas y cuáles exigen rol ADMIN.
@Configuration
public class SecurityConfig {

    // BCrypt es el algoritmo con el que genero y comparo los hash de las contraseñas.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
                // Apago CSRF porque esta API no usa cookies de sesión, sino un token por petición.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // STATELESS: el servidor no guarda sesión, cada petición trae su propio token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Dejo pasar el preflight del navegador para que CORS funcione.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Login y registro tienen que ser públicos: aún no existe token.
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/registro").permitAll()
                        // La documentación queda abierta para poder revisar el contrato.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        // El perfil exige token válido: es la revalidación de la sesión del frontend.
                        .requestMatchers("/api/v1/auth/perfil").authenticated()
                        // La gestión de usuarios queda reservada al rol ADMIN.
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        // Cierro cualquier ruta que no haya declarado arriba.
                        .anyRequest().denyAll())
                // Sin identidad válida respondo 401 en lugar de redirigir a un formulario HTML.
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Pongo mi filtro antes del de usuario y clave, porque aquí la identidad viene del token.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Autorizo solo el origen del frontend, que leo de la configuración y no escribo fijo.
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(allowedOrigin));
        // Declaro los métodos que el panel de administración necesita de verdad.
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permito el encabezado Authorization porque por ahí viaja el Bearer.
        configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuracion);
        return source;
    }
}
