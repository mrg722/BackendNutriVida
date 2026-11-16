package cl.duoc.nutrivida.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Esta entidad representa la tabla usuarios de la base nutrivida_auth.
@Entity
@Table(name = "usuarios")
// Lombok me genera los getters y setters al compilar, así la clase queda corta y legible.
@Getter
@Setter
public class Usuario {

    // Uso IDENTITY para que MySQL genere el id con AUTO_INCREMENT.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Marco el correo como único porque es la credencial con la que identifico a la persona.
    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    // Aquí guardo solo el hash BCrypt: nunca la contraseña legible.
    @Column(nullable = false, length = 100)
    private String password;

    // El rol decide a qué vistas y endpoints puede entrar: ADMIN, NUTRICIONISTA o PACIENTE.
    @Column(nullable = false, length = 20)
    private String rol;

    // Con este campo desactivo una cuenta sin borrarla, como pide el panel de administración.
    @Column(nullable = false)
    private boolean activo = true;
}
