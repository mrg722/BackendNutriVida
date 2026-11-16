package cl.duoc.nutrivida.auth.repository;

import cl.duoc.nutrivida.auth.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Esta interfaz me da el acceso a datos de usuarios sin escribir SQL a mano.
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring interpreta el nombre del método y genera la consulta por correo ignorando mayúsculas.
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    // Lo uso para avisar que el correo ya existe antes de intentar guardar.
    boolean existsByCorreoIgnoreCase(String correo);

    // Con esto el panel de administración puede pedir, por ejemplo, solo los nutricionistas.
    List<Usuario> findByRolIgnoreCase(String rol);
}
