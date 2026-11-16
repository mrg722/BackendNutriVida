package cl.duoc.nutrivida.citas.repository;

import cl.duoc.nutrivida.citas.model.Nutricionista;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Acceso a datos del directorio de nutricionistas.
@Repository
public interface NutricionistaRepository extends JpaRepository<Nutricionista, Long> {

    // El paciente solo debe poder elegir entre los profesionales que están atendiendo.
    List<Nutricionista> findByActivoTrue();

    // Con el correo del token encuentro al profesional dueño de la agenda que se está pidiendo.
    Optional<Nutricionista> findByCorreoIgnoreCase(String correo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCorreoIgnoreCase(String correo);
}
