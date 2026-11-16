package cl.duoc.nutrivida.citas.repository;

import cl.duoc.nutrivida.citas.model.Medicion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Acceso a datos de la ficha clínica.
@Repository
public interface MedicionRepository extends JpaRepository<Medicion, Long> {

    // El historial de un paciente, de la medición más reciente hacia atrás.
    List<Medicion> findByCorreoPacienteIgnoreCaseOrderByFechaDesc(String correoPaciente);

    // Las mediciones que registró un profesional, que es lo único que puede ver de otros pacientes.
    List<Medicion> findByCorreoNutricionistaIgnoreCaseOrderByFechaDesc(String correoNutricionista);
}
