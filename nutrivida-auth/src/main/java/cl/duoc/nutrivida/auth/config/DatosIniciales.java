package cl.duoc.nutrivida.auth.config;

import cl.duoc.nutrivida.auth.model.Rol;
import cl.duoc.nutrivida.auth.model.Usuario;
import cl.duoc.nutrivida.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// Dejo cargadas las cuentas de prueba de los tres roles para poder demostrar el control de acceso.
@Configuration
public class DatosIniciales {

    // La clave de demostración llega desde el entorno; el valor por defecto es solo para desarrollo.
    @Value("${app.datos-iniciales.password:Cambiar123!}")
    private String passwordDemostracion;

    @Bean
    CommandLineRunner cargarUsuarios(UsuarioRepository repository, PasswordEncoder encoder) {
        return args -> {
            // Un administrador, para poder entrar al panel de usuarios.
            crearSiFalta(repository, encoder, "Administrador NutriVida",
                    "admin@nutrivida.cl", Rol.ADMIN);

            // Los cuatro nutricionistas del directorio del caso, con sus nombres reales del catálogo.
            crearSiFalta(repository, encoder, "Nut. Carolina Fuentes M.",
                    "carolina.fuentes@nutrivida.cl", Rol.NUTRICIONISTA);
            crearSiFalta(repository, encoder, "Nut. Rodrigo Sepúlveda A.",
                    "rodrigo.sepulveda@nutrivida.cl", Rol.NUTRICIONISTA);
            crearSiFalta(repository, encoder, "Nut. Daniela Morales C.",
                    "daniela.morales@nutrivida.cl", Rol.NUTRICIONISTA);
            crearSiFalta(repository, encoder, "Nut. Felipe Araya R.",
                    "felipe.araya@nutrivida.cl", Rol.NUTRICIONISTA);

            // Un paciente de prueba, para comprobar que no puede entrar donde no corresponde.
            crearSiFalta(repository, encoder, "Paciente de prueba",
                    "paciente@nutrivida.cl", Rol.PACIENTE);
        };
    }

    // Reviso antes de insertar: así puedo reiniciar la aplicación sin duplicar cuentas.
    private void crearSiFalta(UsuarioRepository repository, PasswordEncoder encoder,
            String nombre, String correo, String rol) {
        if (repository.findByCorreoIgnoreCase(correo).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        // Incluso en los datos de prueba guardo la contraseña como hash BCrypt.
        usuario.setPassword(encoder.encode(passwordDemostracion));
        usuario.setRol(rol);
        usuario.setActivo(true);
        repository.save(usuario);
    }
}
