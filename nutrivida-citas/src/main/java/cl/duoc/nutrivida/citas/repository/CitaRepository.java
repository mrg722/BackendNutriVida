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

    // Para comprobar la disponibilidad no me basta con preguntar si existe una cita a la
    // misma hora exacta: dos citas de distinta duración pueden empezar a horas distintas y
    // aún así pisarse. Por eso me traigo todas las del profesional en ese día y comparo los
    // intervalos en el servicio, donde además tengo a mano la duración de cada atención.
    List<Cita> findByNutricionista_IdAndFechaAndEstadoIn(Long nutricionistaId,
            java.time.LocalDate fecha, List<String> estados);
}
