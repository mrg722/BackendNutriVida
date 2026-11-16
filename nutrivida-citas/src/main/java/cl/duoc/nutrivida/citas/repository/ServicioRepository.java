package cl.duoc.nutrivida.citas.repository;

import cl.duoc.nutrivida.citas.model.Servicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Acceso a datos del catálogo de servicios.
@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    // Lo uso para que el paciente vea solo los servicios que siguen ofreciéndose.
    List<Servicio> findByActivoTrue();

    // Con esto el frontend filtra el catálogo por tipo sin traerse todo y filtrar en memoria.
    List<Servicio> findByTipoIgnoreCase(String tipo);

    // Reviso el código antes de insertar para no repetirlo en el catálogo.
    boolean existsByCodigoIgnoreCase(String codigo);
}
