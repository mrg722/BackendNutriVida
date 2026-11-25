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

    // Si un servicio quedara cargado sin duración, le reservo media hora, que es la atención
    // más corta del catálogo. Prefiero bloquear un rato razonable antes que tratar la cita como
    // si no ocupara tiempo y dejar que la siguiente se le encime.
    private static final int DURACION_POR_DEFECTO_MINUTOS = 30;

    private final CitaRepository repository;
    private final NutricionistaService nutricionistas;
    private final ServicioService servicios;
    private final UsuarioAutenticado usuario;
    private final HorarioAtencion horarios;

    public CitaService(CitaRepository repository, NutricionistaService nutricionistas,
            ServicioService servicios, UsuarioAutenticado usuario, HorarioAtencion horarios) {
        this.repository = repository;
        this.nutricionistas = nutricionistas;
        this.servicios = servicios;
        this.usuario = usuario;
        this.horarios = horarios;
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
        // Las tres comprobaciones que siguen son datos mal enviados, no un choque con otra
        // cita, así que respondo 400 y no 409. Para partir, no tiene sentido pedir una hora
        // que ya pasó.
        if (request.fecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser anterior a hoy");
        }

        // El día tiene que ser uno de los que atiende este profesional en particular. Antes
        // comprobaba un horario único para toda la clínica, y eso dejaba agendar con Rodrigo un
        // lunes aunque él atienda martes, jueves y sábado: la tabla guardaba el dato y la regla
        // no lo miraba.
        if (!horarios.atiendeEl(profesional, request.fecha())) {
            throw new IllegalArgumentException(profesional.getNombre() + " no atiende los "
                    + horarios.nombreDelDia(request.fecha().getDayOfWeek())
                    + ". Sus días de atención son: " + profesional.getDiasAtencion());
        }

        // La cita no solo tiene que empezar dentro de la jornada: tiene que terminar dentro.
        // Una consulta de 60 minutos a las 16:30 no cabe si el profesional cierra a las 17:00.
        // Trabajo con minutos desde medianoche para no tener que pensar en vueltas de reloj.
        int duracion = duracionDe(servicio);
        int inicio = minutos(request.hora());
        int termino = inicio + duracion;
        int apertura = minutos(horarios.apertura(profesional));
        int cierre = minutos(horarios.cierre(profesional));

        if (inicio < apertura || termino > cierre) {
            throw new IllegalArgumentException("Una atención de " + duracion
                    + " minutos a las " + request.hora() + " no cabe en el horario de "
                    + profesional.getNombre() + ", que atiende de "
                    + horarios.apertura(profesional) + " a " + horarios.cierre(profesional));
        }

        // Evito la doble reserva, que es justo el problema de la agenda de papel del caso.
        // Comparo intervalos completos y no horas exactas: dos citas se pisan cuando una
        // empieza antes de que la otra termine, aunque no partan en el mismo minuto. Con la
        // comparación anterior, una consulta de una hora a las 10:00 y un control a las 10:30
        // convivían sin problema en la agenda, y el profesional terminaba con dos pacientes
        // encima a la misma hora.
        boolean ocupado = repository
                .findByNutricionista_IdAndFechaAndEstadoIn(
                        profesional.getId(), request.fecha(), ESTADOS_QUE_OCUPAN_HORA)
                .stream()
                // Si estoy reprogramando, la cita que muevo no choca consigo misma.
                .filter(otra -> citaActual == null || !otra.getId().equals(citaActual.getId()))
                .anyMatch(otra -> seCruzan(inicio, termino, otra));

        if (ocupado) {
            throw new ReglaNegocioException("Ese horario ya está tomado con "
                    + profesional.getNombre());
        }
    }

    // Dos bloques se cruzan cuando cada uno empieza antes de que el otro termine. Dejo fuera
    // el caso de los extremos a propósito: una cita que termina a las 10:00 y otra que empieza
    // a las 10:00 no se pisan, van una seguida de la otra.
    private boolean seCruzan(int inicio, int termino, Cita otra) {
        int inicioOtra = minutos(otra.getHora());
        int terminoOtra = inicioOtra + duracionDe(otra.getServicio());
        return inicio < terminoOtra && inicioOtra < termino;
    }

    // La duración está en el catálogo de servicios, así que cada tipo de atención ocupa en la
    // agenda el rato que de verdad dura: una consulta inicial bloquea más que un control.
    private int duracionDe(Servicio servicio) {
        return servicio.getDuracionMinutos() == null
                ? DURACION_POR_DEFECTO_MINUTOS
                : servicio.getDuracionMinutos();
    }

    private int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
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
