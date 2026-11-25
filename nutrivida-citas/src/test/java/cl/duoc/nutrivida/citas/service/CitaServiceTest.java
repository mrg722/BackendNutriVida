package cl.duoc.nutrivida.citas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

// Pruebo el flujo de la cita y las reglas de pertenencia, que son el corazón del caso.
class CitaServiceTest {

    private static final String CORREO_PACIENTE = "paciente@nutrivida.cl";
    private static final String CORREO_PROFESIONAL = "carolina.fuentes@nutrivida.cl";

    private CitaRepository repository;
    private NutricionistaService nutricionistas;
    private ServicioService servicios;
    private UsuarioAutenticado usuario;
    private CitaService service;

    @BeforeEach
    void prepararDobles() {
        repository = mock(CitaRepository.class);
        nutricionistas = mock(NutricionistaService.class);
        servicios = mock(ServicioService.class);
        usuario = mock(UsuarioAutenticado.class);
        // HorarioAtencion no es un doble: no habla con la base de datos ni con nadie más, solo
        // interpreta las dos columnas de la agenda. Me conviene que corra de verdad, porque es
        // justamente la regla que quiero comprobar.
        service = new CitaService(repository, nutricionistas, servicios, usuario,
                new HorarioAtencion());
    }

    private Nutricionista profesional(boolean activo) {
        // Sin días ni horario cargados, la regla cae en el horario general de la clínica. Lo
        // dejo así en el caso habitual para que las pruebas no dependan del día en que corran.
        return profesional(activo, null, null);
    }

    private Nutricionista profesional(boolean activo, String diasAtencion, String horario) {
        Nutricionista profesional = new Nutricionista();
        profesional.setId(1L);
        profesional.setCodigo("NUT001");
        profesional.setNombre("Nut. Carolina Fuentes M.");
        profesional.setCorreo(CORREO_PROFESIONAL);
        profesional.setEspecialidad("Obesidad y síndrome metabólico");
        profesional.setDiasAtencion(diasAtencion);
        profesional.setHorario(horario);
        profesional.setActivo(activo);
        return profesional;
    }

    private Servicio servicio(boolean activo) {
        return servicio(activo, null);
    }

    private Servicio servicio(boolean activo, Integer duracionMinutos) {
        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setCodigo("CN001");
        servicio.setNombre("Primera consulta nutricional");
        servicio.setDuracionMinutos(duracionMinutos);
        servicio.setPrecio(35000);
        servicio.setActivo(activo);
        return servicio;
    }

    private Cita cita(String estado) {
        Cita cita = new Cita();
        cita.setId(7L);
        cita.setCorreoPaciente(CORREO_PACIENTE);
        cita.setNutricionista(profesional(true));
        cita.setServicio(servicio(true));
        cita.setFecha(LocalDate.now().plusDays(5));
        cita.setHora(LocalTime.of(10, 0));
        cita.setMotivo("Primera consulta para control de peso");
        cita.setEstado(estado);
        return cita;
    }

    private CitaRequest peticion(LocalDate fecha, LocalTime hora) {
        return new CitaRequest(1L, 1L, fecha, hora, "Primera consulta para control de peso");
    }

    // Dejo preparado el caso habitual: paciente con sesión, profesional y servicio activos.
    private void escenarioPacienteConHoraLibre() {
        when(usuario.correo()).thenReturn(CORREO_PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(nutricionistas.buscar(1L)).thenReturn(profesional(true));
        when(servicios.buscar(1L)).thenReturn(servicio(true));
        // La agenda del día viene vacía: no hay con qué chocar.
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of());
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));
    }

    // Una cita ya tomada en la agenda del profesional, para los casos de choque.
    private Cita citaOcupando(long id, LocalDate fecha, LocalTime hora, int duracionMinutos) {
        Cita ocupada = new Cita();
        ocupada.setId(id);
        ocupada.setCorreoPaciente("otra.paciente@nutrivida.cl");
        ocupada.setNutricionista(profesional(true));
        ocupada.setServicio(servicio(true, duracionMinutos));
        ocupada.setFecha(fecha);
        ocupada.setHora(hora);
        ocupada.setMotivo("Cita que ya estaba en la agenda");
        ocupada.setEstado(EstadoCita.CONFIRMADA);
        return ocupada;
    }

    @Test
    @DisplayName("La cita nace en SOLICITADA y con el correo que viene del token")
    void solicitudCorrecta() {
        escenarioPacienteConHoraLibre();

        CitaResponse respuesta = service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(10, 0)));

        // Nadie se autoconfirma: la clínica tiene que aceptar el horario después.
        assertEquals(EstadoCita.SOLICITADA, respuesta.estado());
        // El correo sale del token, no del cuerpo de la petición.
        assertEquals(CORREO_PACIENTE, respuesta.correoPaciente());
        assertEquals("CN001", respuesta.servicioCodigo());
    }

    @Test
    @DisplayName("No permito dos citas en el mismo horario del mismo profesional")
    void horarioYaTomado() {
        escenarioPacienteConHoraLibre();
        LocalDate fecha = LocalDate.now().plusDays(5);
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of(citaOcupando(99L, fecha, LocalTime.of(10, 0), 30)));

        // Este es justo el problema de la agenda de papel que el sistema viene a resolver.
        assertThrows(ReglaNegocioException.class,
                () -> service.solicitar(peticion(fecha, LocalTime.of(10, 0))));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Tampoco permito dos citas que se encimen por su duración")
    void citasQueSeEnciman() {
        escenarioPacienteConHoraLibre();
        LocalDate fecha = LocalDate.now().plusDays(5);
        // La cita que ya está en la agenda empieza a las 10:00 y dura 50 minutos, o sea que
        // tiene ocupado al profesional hasta las 10:50.
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of(citaOcupando(99L, fecha, LocalTime.of(10, 0), 50)));
        when(servicios.buscar(1L)).thenReturn(servicio(true, 30));

        // Pido las 10:30: es otra hora de inicio, pero cae dentro de la cita anterior. Con la
        // comparación de hora exacta que tenía antes, esto se aceptaba sin reclamar.
        assertThrows(ReglaNegocioException.class,
                () -> service.solicitar(peticion(fecha, LocalTime.of(10, 30))));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Dos citas seguidas sí caben: una termina justo cuando empieza la otra")
    void citasSeguidasSinChoque() {
        escenarioPacienteConHoraLibre();
        LocalDate fecha = LocalDate.now().plusDays(5);
        // La anterior ocupa de 10:00 a 10:50.
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of(citaOcupando(99L, fecha, LocalTime.of(10, 0), 50)));
        when(servicios.buscar(1L)).thenReturn(servicio(true, 30));

        // Pido las 10:50, justo cuando la otra termina. No se pisan, van una seguida de la
        // otra, y la agenda no tiene por qué rechazarla.
        assertEquals(EstadoCita.SOLICITADA,
                service.solicitar(peticion(fecha, LocalTime.of(10, 50))).estado());
    }

    @Test
    @DisplayName("Rechazo un día que este profesional no atiende")
    void diaQueNoAtiende() {
        escenarioPacienteConHoraLibre();
        when(nutricionistas.buscar(1L))
                .thenReturn(profesional(true, "Lunes, Miércoles, Viernes", "09:00 - 17:00"));

        // Busco el próximo domingo en vez de escribir una fecha fija, para que la prueba dé lo
        // mismo el día en que se ejecute.
        LocalDate domingo = LocalDate.now().plusDays(1);
        while (domingo.getDayOfWeek() != DayOfWeek.SUNDAY) {
            domingo = domingo.plusDays(1);
        }

        LocalDate fecha = domingo;
        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(peticion(fecha, LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("La cita tiene que terminar antes de que el profesional cierre")
    void laCitaNoAlcanzaATerminar() {
        escenarioPacienteConHoraLibre();
        when(nutricionistas.buscar(1L)).thenReturn(profesional(true, null, "09:00 - 17:00"));
        when(servicios.buscar(1L)).thenReturn(servicio(true, 60));

        // Las 16:30 están dentro de la jornada, pero una atención de una hora terminaría a las
        // 17:30, con el profesional ya fuera. No basta con que la cita empiece a tiempo.
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(16, 30))));
    }

    @Test
    @DisplayName("Rechazo una fecha anterior a hoy")
    void fechaEnElPasado() {
        escenarioPacienteConHoraLibre();

        assertThrows(IllegalArgumentException.class, () -> service.solicitar(
                peticion(LocalDate.now().minusDays(1), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("Rechazo una hora fuera del horario de atención")
    void horaFueraDeHorario() {
        escenarioPacienteConHoraLibre();

        // Antes de las 09:00 y después de las 18:00 la clínica no atiende.
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(8, 0))));
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(19, 0))));
    }

    @Test
    @DisplayName("No se puede pedir hora con un profesional que ya no atiende")
    void profesionalDesactivado() {
        escenarioPacienteConHoraLibre();
        when(nutricionistas.buscar(1L)).thenReturn(profesional(false));

        assertThrows(ReglaNegocioException.class, () -> service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("No se puede pedir un servicio retirado del catálogo")
    void servicioDesactivado() {
        escenarioPacienteConHoraLibre();
        when(servicios.buscar(1L)).thenReturn(servicio(false));

        assertThrows(ReglaNegocioException.class, () -> service.solicitar(
                peticion(LocalDate.now().plusDays(5), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("El paciente puede cancelar su cita")
    void pacienteCancelaSuCita() {
        when(usuario.correo()).thenReturn(CORREO_PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.CONFIRMADA)));
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));

        CitaResponse respuesta = service.cambiarEstado(7L, EstadoCita.CANCELADA);

        assertEquals(EstadoCita.CANCELADA, respuesta.estado());
    }

    @Test
    @DisplayName("El paciente no puede confirmar ni dar por atendida su propia cita")
    void pacienteNoPuedeConfirmar() {
        when(usuario.correo()).thenReturn(CORREO_PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.SOLICITADA)));

        // Confirmar y cerrar la atención son decisiones de la clínica, no del paciente.
        assertThrows(AccessDeniedException.class,
                () -> service.cambiarEstado(7L, EstadoCita.CONFIRMADA));
        assertThrows(AccessDeniedException.class,
                () -> service.cambiarEstado(7L, EstadoCita.ATENDIDA));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("El nutricionista dueño de la agenda sí puede confirmar")
    void profesionalConfirmaSuAgenda() {
        when(usuario.correo()).thenReturn(CORREO_PROFESIONAL);
        when(usuario.esNutricionista()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.SOLICITADA)));
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));

        CitaResponse respuesta = service.cambiarEstado(7L, EstadoCita.CONFIRMADA);

        assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
    }

    @Test
    @DisplayName("Un nutricionista no puede tocar la cita de otra agenda")
    void profesionalDeOtraAgenda() {
        when(usuario.correo()).thenReturn("rodrigo.sepulveda@nutrivida.cl");
        when(usuario.esNutricionista()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.SOLICITADA)));

        assertThrows(AccessDeniedException.class, () -> service.buscar(7L));
        assertThrows(AccessDeniedException.class,
                () -> service.cambiarEstado(7L, EstadoCita.CONFIRMADA));
    }

    @Test
    @DisplayName("Una cita cerrada no admite más cambios de estado")
    void estadoFinalNoSeMueve() {
        when(usuario.correo()).thenReturn(CORREO_PROFESIONAL);
        when(usuario.esNutricionista()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.ATENDIDA)));

        assertThrows(ReglaNegocioException.class,
                () -> service.cambiarEstado(7L, EstadoCita.CONFIRMADA));
    }

    @Test
    @DisplayName("Rechazo un estado que no existe en el flujo")
    void estadoInventado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(7L, "PENDIENTE_DE_PAGO"));
        // Ni siquiera busco la cita: el estado se valida primero.
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Reprogramar deja la cita otra vez por confirmar")
    void reprogramacionVuelveASolicitada() {
        when(usuario.correo()).thenReturn(CORREO_PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.CONFIRMADA)));
        when(nutricionistas.buscar(1L)).thenReturn(profesional(true));
        when(servicios.buscar(1L)).thenReturn(servicio(true));
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of());
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));

        CitaResponse respuesta = service.reprogramar(7L,
                peticion(LocalDate.now().plusDays(9), LocalTime.of(15, 30)));

        assertEquals(EstadoCita.SOLICITADA, respuesta.estado());
        assertEquals(LocalTime.of(15, 30), respuesta.hora());
    }

    @Test
    @DisplayName("Al reprogramar, su propio horario actual no cuenta como choque")
    void reprogramacionConservandoSuHorario() {
        when(usuario.correo()).thenReturn(CORREO_PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        Cita existente = cita(EstadoCita.CONFIRMADA);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(nutricionistas.buscar(1L)).thenReturn(profesional(true));
        when(servicios.buscar(1L)).thenReturn(servicio(true));
        // La agenda de ese día trae una sola cita ocupando el bloque, y es esta misma. Como la
        // reconozco por su id, no la cuento como un choque consigo misma.
        when(repository.findByNutricionista_IdAndFechaAndEstadoIn(
                anyLong(), any(LocalDate.class), anyList()))
                .thenReturn(List.of(existente));
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));

        // Solo cambio el motivo y mantengo fecha y hora: no debe fallar.
        CitaResponse respuesta = service.reprogramar(7L,
                peticion(existente.getFecha(), existente.getHora()));

        assertEquals(EstadoCita.SOLICITADA, respuesta.estado());
    }

    @Test
    @DisplayName("Una cita que no existe responde no encontrada")
    void citaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscar(99L));
    }

    @Test
    @DisplayName("El administrador ve y mueve cualquier cita")
    void administradorPuedeTodo() {
        when(usuario.correo()).thenReturn("admin@nutrivida.cl");
        when(usuario.esAdmin()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(cita(EstadoCita.SOLICITADA)));
        when(repository.save(any(Cita.class))).thenAnswer(l -> l.getArgument(0));

        assertEquals(CORREO_PACIENTE, service.buscar(7L).correoPaciente());
        assertEquals(EstadoCita.NO_ASISTIO,
                service.cambiarEstado(7L, EstadoCita.NO_ASISTIO).estado());
    }
}
