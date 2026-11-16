package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.dto.CitaRequest;
import cl.duoc.nutrivida.citas.dto.CitaResponse;
import cl.duoc.nutrivida.citas.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.citas.exception.ReglaNegocioException;
import cl.duoc.nutrivida.citas.model.Cita;
import cl.duoc.nutrivida.citas.model.EstadoCita;
import cl.duoc.nutrivida.citas.model.Nutricionista;
import cl.duoc.nutrivida.citas.model.Servicio;
import cl.duoc.nutrivida.citas.repository.CitaRepository;
import cl.duoc.nutrivida.citas.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Aquí está el flujo completo del "pedido" del caso: solicitar, confirmar, reprogramar y cancelar.
// También aplico las reglas de privacidad: cada persona solo ve y mueve las citas que le tocan.
@Service
public class CitaService {

    // Estos son los estados que ocupan un horario. Una cita cancelada libera el cupo.
    private static final List<String> ESTADOS_QUE_OCUPAN_HORA =
            List.of(EstadoCita.SOLICITADA, EstadoCita.CONFIRMADA);

    // La clínica atiende entre estas horas, igual que valida el formulario de React.
    private static final LocalTime HORA_APERTURA = LocalTime.of(9, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(18, 0);

    private final CitaRepository repository;
    private final NutricionistaService nutricionistas;
    private final ServicioService servicios;
    private final UsuarioAutenticado usuario;

    public CitaService(CitaRepository repository, NutricionistaService nutricionistas,
            ServicioService servicios, UsuarioAutenticado usuario) {
        this.repository = repository;
        this.nutricionistas = nutricionistas;
        this.servicios = servicios;
        this.usuario = usuario;
    }

    // Listado general: solo lo usa el administrador, que sí puede ver toda la agenda.
    @Transactional(readOnly = true)
    public List<CitaResponse> listarTodas() {
        return repository.findAll().stream().map(CitaResponse::desde).toList();
    }

    // Este método devuelve lo que corresponde según quién pregunta, sin que el frontend decida.
    @Transactional(readOnly = true)
    public List<CitaResponse> listarMias() {
        String correo = usuario.correo();

        if (usuario.esNutricionista()) {
            // Al profesional le entrego su propia agenda, buscada por su correo del token.
            Nutricionista profesional = nutricionistas.buscarPorCorreo(correo);
            return repository.findByNutricionista_IdOrderByFechaAscHoraAsc(profesional.getId())
                    .stream().map(CitaResponse::desde).toList();
        }

        if (usuario.esAdmin()) {
            // El administrador ve todo, así que reutilizo el listado completo.
            return listarTodas();
        }

        // Al paciente le entrego únicamente sus propias citas.
        return repository.findByCorreoPacienteIgnoreCaseOrderByFechaDescHoraDesc(correo)
                .stream().map(CitaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse buscar(Long id) {
        Cita cita = obtener(id);
        // Compruebo el permiso antes de devolver el dato, no después.
        verificarPuedeVer(cita);
        return CitaResponse.desde(cita);
    }

    // El paciente pide una hora. El estado inicial siempre es SOLICITADA: nadie se autoconfirma.
    @Transactional
    public CitaResponse solicitar(CitaRequest request) {
        Nutricionista profesional = nutricionistas.buscar(request.nutricionistaId());
        Servicio servicio = servicios.buscar(request.servicioId());

        validarDisponibilidad(request, profesional, servicio, null);

        Cita cita = new Cita();
        // El correo lo tomo del token y no del cuerpo: así nadie agenda a nombre de otra persona.
        cita.setCorreoPaciente(usuario.correo());
        cita.setNutricionista(profesional);
        cita.setServicio(servicio);
        cita.setFecha(request.fecha());
        cita.setHora(request.hora());
        cita.setMotivo(request.motivo().trim());
        cita.setEstado(EstadoCita.SOLICITADA);

        return CitaResponse.desde(repository.save(cita));
    }

    // Reprogramar: cambio fecha, hora, profesional o servicio y la cita vuelve a quedar por confirmar.
    @Transactional
    public CitaResponse reprogramar(Long id, CitaRequest request) {
        Cita cita = obtener(id);
        verificarPuedeModificar(cita);

        // Una cita ya atendida o cerrada es historial: no la dejo mover.
        if (esEstadoFinal(cita.getEstado())) {
            throw new ReglaNegocioException(
                    "No se puede reprogramar una cita en estado " + cita.getEstado());
        }

        Nutricionista profesional = nutricionistas.buscar(request.nutricionistaId());
        Servicio servicio = servicios.buscar(request.servicioId());

        // Paso el id actual para que el choque de horario no se detecte contra ella misma.
        validarDisponibilidad(request, profesional, servicio, cita);

        cita.setNutricionista(profesional);
        cita.setServicio(servicio);
        cita.setFecha(request.fecha());
        cita.setHora(request.hora());
        cita.setMotivo(request.motivo().trim());
        // Al mover la hora la clínica tiene que volver a confirmarla.
        cita.setEstado(EstadoCita.SOLICITADA);

        return CitaResponse.desde(repository.save(cita));
    }

    // Avance del flujo. Cada rol puede llevar la cita solo a los estados que le corresponden.
    @Transactional
    public CitaResponse cambiarEstado(Long id, String estadoNuevo) {
        if (!EstadoCita.esValido(estadoNuevo)) {
            throw new IllegalArgumentException("El estado debe ser uno de " + EstadoCita.todos());
        }

        Cita cita = obtener(id);
        verificarPuedeModificar(cita);

        if (esEstadoFinal(cita.getEstado())) {
            throw new ReglaNegocioException(
                    "La cita ya está en estado " + cita.getEstado() + " y no admite cambios");
        }

        // El paciente puede cancelar su hora, pero no confirmarla ni marcarla como atendida:
        // esa decisión es de la clínica.
        if (usuario.esPaciente() && !EstadoCita.CANCELADA.equals(estadoNuevo)) {
            throw new AccessDeniedException("Como paciente solo puedes cancelar tu cita");
        }

        cita.setEstado(estadoNuevo);
        return CitaResponse.desde(repository.save(cita));
    }

    // Borrado definitivo del registro. SecurityConfig lo reserva al administrador.
    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }

    // Búsqueda centralizada para lanzar el 404 en un solo lugar.
    private Cita obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
    }

    // Reglas de negocio del horario, todas juntas para poder leerlas de una vez.
    private void validarDisponibilidad(CitaRequest request, Nutricionista profesional,
            Servicio servicio, Cita citaActual) {

        if (!profesional.isActivo()) {
            throw new ReglaNegocioException("El nutricionista "
                    + profesional.getNombre() + " no está atendiendo");
        }
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio "
                    + servicio.getNombre() + " no está disponible");
        }
        // Estas dos son datos mal enviados, no un choque con otra cita, así que respondo 400.
        // No tiene sentido pedir una hora que ya pasó.
        if (request.fecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser anterior a hoy");
        }
        if (request.hora().isBefore(HORA_APERTURA) || request.hora().isAfter(HORA_CIERRE)) {
            throw new IllegalArgumentException("La hora debe estar entre las 09:00 y las 18:00");
        }

        // Evito la doble reserva, que es justo el problema de la agenda de papel del caso.
        boolean ocupada = repository.existsByNutricionista_IdAndFechaAndHoraAndEstadoIn(
                profesional.getId(), request.fecha(), request.hora(), ESTADOS_QUE_OCUPAN_HORA);

        // Si estoy reprogramando, su propio horario actual no cuenta como choque.
        boolean esSuPropioHorario = citaActual != null
                && citaActual.getNutricionista().getId().equals(profesional.getId())
                && citaActual.getFecha().equals(request.fecha())
                && citaActual.getHora().equals(request.hora());

        if (ocupada && !esSuPropioHorario) {
            throw new ReglaNegocioException("Ese horario ya está tomado con "
                    + profesional.getNombre());
        }
    }

    // Estos tres estados cierran la cita: después de ellos ya no hay transición posible.
    private boolean esEstadoFinal(String estado) {
        return EstadoCita.ATENDIDA.equals(estado)
                || EstadoCita.CANCELADA.equals(estado)
                || EstadoCita.NO_ASISTIO.equals(estado);
    }

    // Privacidad de lectura: el administrador ve todo, el paciente su cita y el profesional su agenda.
    private void verificarPuedeVer(Cita cita) {
        if (usuario.esAdmin() || esDuenoPaciente(cita) || esSuAgenda(cita)) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para ver esta cita");
    }

    // Para modificar aplico la misma regla de pertenencia que para leer.
    private void verificarPuedeModificar(Cita cita) {
        if (usuario.esAdmin() || esDuenoPaciente(cita) || esSuAgenda(cita)) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para modificar esta cita");
    }

    private boolean esDuenoPaciente(Cita cita) {
        return usuario.esPaciente() && cita.getCorreoPaciente().equalsIgnoreCase(usuario.correo());
    }

    private boolean esSuAgenda(Cita cita) {
        return usuario.esNutricionista()
                && cita.getNutricionista().getCorreo().equalsIgnoreCase(usuario.correo());
    }
}
