package cl.duoc.nutrivida.citas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.nutrivida.citas.dto.MedicionRequest;
import cl.duoc.nutrivida.citas.dto.MedicionResponse;
import cl.duoc.nutrivida.citas.model.Medicion;
import cl.duoc.nutrivida.citas.repository.MedicionRepository;
import cl.duoc.nutrivida.citas.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

// Aquí pruebo la regla de privacidad de los datos clínicos, que es una restricción explícita
// del caso: solo el paciente y su nutricionista pueden ver peso, medidas y diagnósticos.
class MedicionServiceTest {

    private static final String PACIENTE = "paciente@nutrivida.cl";
    private static final String CAROLINA = "carolina.fuentes@nutrivida.cl";
    private static final String RODRIGO = "rodrigo.sepulveda@nutrivida.cl";

    private MedicionRepository repository;
    private UsuarioAutenticado usuario;
    private MedicionService service;

    @BeforeEach
    void prepararDobles() {
        repository = mock(MedicionRepository.class);
        usuario = mock(UsuarioAutenticado.class);
        service = new MedicionService(repository, usuario);
    }

    private Medicion medicion(String correoNutricionista) {
        Medicion medicion = new Medicion();
        medicion.setId(3L);
        medicion.setCorreoPaciente(PACIENTE);
        medicion.setCorreoNutricionista(correoNutricionista);
        medicion.setFecha(LocalDate.now());
        medicion.setPesoKg(82.5);
        medicion.setTallaCm(172.0);
        return medicion;
    }

    private MedicionRequest peticion() {
        return new MedicionRequest(PACIENTE, LocalDate.now(), 82.5, 172.0, 95.0,
                "Primer control", "Cuatro comidas diarias");
    }

    @Test
    @DisplayName("La ficha queda firmada con el correo del profesional que la escribió")
    void registroFirmaConElCorreoDelToken() {
        when(usuario.correo()).thenReturn(CAROLINA);
        when(repository.save(any(Medicion.class))).thenAnswer(l -> l.getArgument(0));

        service.registrar(peticion());

        ArgumentCaptor<Medicion> guardada = ArgumentCaptor.forClass(Medicion.class);
        verify(repository).save(guardada.capture());
        // El autor no lo elige el cuerpo de la petición: lo pone el servidor desde el token.
        assertEquals(CAROLINA, guardada.getValue().getCorreoNutricionista());
        assertEquals(PACIENTE, guardada.getValue().getCorreoPaciente());
    }

    @Test
    @DisplayName("Un paciente no puede pedir la ficha de otra persona")
    void pacienteNoVeFichaAjena() {
        when(usuario.correo()).thenReturn(PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);

        // Aunque escriba otro correo en la URL, no sale de su propia ficha.
        assertThrows(AccessDeniedException.class,
                () -> service.listarPorPaciente("otro.paciente@nutrivida.cl"));
    }

    @Test
    @DisplayName("Un paciente sí puede pedir su propia ficha por correo")
    void pacienteVeSuPropiaFicha() {
        when(usuario.correo()).thenReturn(PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(repository.findByCorreoPacienteIgnoreCaseOrderByFechaDesc(PACIENTE))
                .thenReturn(List.of(medicion(CAROLINA)));

        List<MedicionResponse> historial = service.listarPorPaciente(PACIENTE);

        assertEquals(1, historial.size());
    }

    @Test
    @DisplayName("Un nutricionista solo ve las mediciones que él mismo registró")
    void nutricionistaNoVeLoDeOtroProfesional() {
        when(usuario.correo()).thenReturn(RODRIGO);
        when(usuario.esNutricionista()).thenReturn(true);
        // En la ficha del paciente hay dos mediciones, una de cada profesional.
        when(repository.findByCorreoPacienteIgnoreCaseOrderByFechaDesc(PACIENTE))
                .thenReturn(List.of(medicion(CAROLINA), medicion(RODRIGO)));

        List<MedicionResponse> historial = service.listarPorPaciente(PACIENTE);

        // Solo debe quedar la suya: así no accede a los datos clínicos de pacientes de otros.
        assertEquals(1, historial.size());
        assertEquals(RODRIGO, historial.get(0).correoNutricionista());
    }

    @Test
    @DisplayName("Solo el autor de la medición puede corregirla o eliminarla")
    void soloElAutorModifica() {
        when(usuario.correo()).thenReturn(RODRIGO);
        when(usuario.esNutricionista()).thenReturn(true);
        when(repository.findById(3L)).thenReturn(Optional.of(medicion(CAROLINA)));

        assertThrows(AccessDeniedException.class, () -> service.actualizar(3L, peticion()));
        assertThrows(AccessDeniedException.class, () -> service.eliminar(3L));
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("El autor sí puede corregir su propia medición")
    void elAutorCorrigeSuMedicion() {
        when(usuario.correo()).thenReturn(CAROLINA);
        when(usuario.esNutricionista()).thenReturn(true);
        when(repository.findById(3L)).thenReturn(Optional.of(medicion(CAROLINA)));
        when(repository.save(any(Medicion.class))).thenAnswer(l -> l.getArgument(0));

        MedicionResponse respuesta = service.actualizar(3L, new MedicionRequest(
                PACIENTE, LocalDate.now(), 80.0, 172.0, 93.0, "Corrección", "Plan ajustado"));

        assertEquals(80.0, respuesta.pesoKg());
        assertEquals("Corrección", respuesta.observaciones());
    }

    @Test
    @DisplayName("El paciente dueño puede leer una medición de su ficha")
    void pacienteLeeSuMedicion() {
        when(usuario.correo()).thenReturn(PACIENTE);
        when(usuario.esPaciente()).thenReturn(true);
        when(repository.findById(3L)).thenReturn(Optional.of(medicion(CAROLINA)));

        MedicionResponse respuesta = service.buscar(3L);

        assertEquals(PACIENTE, respuesta.correoPaciente());
        // Compruebo además que llega con el IMC ya calculado.
        assertTrue(respuesta.imc() > 0);
    }

    @Test
    @DisplayName("Al pedir mi historial uso el correo del token y no un parámetro")
    void historialPropioUsaElToken() {
        when(usuario.correo()).thenReturn(PACIENTE);
        when(repository.findByCorreoPacienteIgnoreCaseOrderByFechaDesc(anyString()))
                .thenReturn(List.of(medicion(CAROLINA)));

        service.listarMias();

        verify(repository).findByCorreoPacienteIgnoreCaseOrderByFechaDesc(PACIENTE);
    }
}
