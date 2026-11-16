package cl.duoc.nutrivida.citas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Con esta clase leo quién hizo la petición sin tener que repetir el acceso al contexto de
// seguridad en cada servicio. Los datos vienen del token que ya validó JwtAuthenticationFilter.
@Component
public class UsuarioAutenticado {

    // Devuelvo el correo, que es el subject del token y la identidad de la persona.
    public String correo() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null ? null : autenticacion.getName();
    }

    // Reviso si la persona tiene un rol concreto. Le agrego el prefijo ROLE_ porque así
    // quedan guardadas las autoridades en Spring Security.
    public boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridad -> autoridad.equals("ROLE_" + rol));
    }

    // Atajos que dejan las reglas de negocio más legibles que comparar textos sueltos.
    public boolean esAdmin() {
        return tieneRol("ADMIN");
    }

    public boolean esNutricionista() {
        return tieneRol("NUTRICIONISTA");
    }

    public boolean esPaciente() {
        return tieneRol("PACIENTE");
    }
}
