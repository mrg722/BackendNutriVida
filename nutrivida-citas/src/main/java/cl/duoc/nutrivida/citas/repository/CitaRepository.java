package cl.duoc.nutrivida.citas.repository;

import cl.duoc.nutrivida.citas.model.Cita;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Acceso a datos de las citas, con las consultas que necesita cada rol.
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Las citas de un paciente, ordenadas de la más próxima a la más antigua.
    List<Cita> findByCorreoPacienteIgnoreCaseOrderByFechaDescHoraDesc(String correoPaciente);

    // La agenda de un profesional: Spring navega la relación con el guion bajo del nombre.
    List<Cita> findByNutricionista_IdOrderByFechaAscHoraAsc(Long nutricionistaId);

    // Lo uso en las estadísticas para contar cuántas citas quedaron en cada estado.
    long countByEstado(String estado);

    // Con esto saco la distribución de atenciones por nutricionista que pide el caso.
    long countByNutricionista_Id(Long nutricionistaId);

    // Antes de aceptar una hora reviso que el profesional no la tenga ya tomada.
    boolean existsByNutricionista_IdAndFechaAndHoraAndEstadoIn(Long nutricionistaId,
            java.time.LocalDate fecha, java.time.LocalTime hora, List<String> estados);
}
