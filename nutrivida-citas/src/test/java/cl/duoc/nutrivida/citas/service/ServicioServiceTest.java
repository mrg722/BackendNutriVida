package cl.duoc.nutrivida.citas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.nutrivida.citas.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.citas.exception.ReglaNegocioException;
import cl.duoc.nutrivida.citas.model.Servicio;
import cl.duoc.nutrivida.citas.repository.ServicioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// Pruebo las reglas del catálogo: el código no se repite, el id no se puede sobrescribir
// desde fuera y un servicio retirado deja de ofrecerse sin perder su historial.
class ServicioServiceTest {

    private ServicioRepository repository;
    private ServicioService service;

    @BeforeEach
    void prepararDobles() {
        repository = mock(ServicioRepository.class);
        service = new ServicioService(repository);
    }

    private Servicio servicio(Long id, String codigo) {
        Servicio servicio = new Servicio();
        servicio.setId(id);
        servicio.setCodigo(codigo);
        servicio.setTipo("Consulta");
        servicio.setNombre("Primera consulta nutricional");
        servicio.setDuracionMinutos(50);
        servicio.setModalidad("Presencial");
        servicio.setPrecio(35000);
        servicio.setDescripcion("Evaluación inicial del paciente");
        servicio.setActivo(true);
        return servicio;
    }

    @Test
    @DisplayName("No permito dos servicios con el mismo código de catálogo")
    void codigoRepetido() {
        when(repository.existsByCodigoIgnoreCase("CN001")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.crear(servicio(null, "CN001")));
        assertEquals("Ya existe un servicio con el código CN001", error.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Al crear ignoro el id que venga en el cuerpo, para no sobrescribir otro registro")
    void creacionIgnoraElIdRecibido() {
        when(repository.existsByCodigoIgnoreCase("CN009")).thenReturn(false);
        when(repository.save(any(Servicio.class))).thenAnswer(l -> l.getArgument(0));

        // Envío a propósito un id existente para comprobar que no se respeta.
        service.crear(servicio(1L, "CN009"));

        ArgumentCaptor<Servicio> guardado = ArgumentCaptor.forClass(Servicio.class);
        verify(repository).save(guardado.capture());
        assertNull(guardado.getValue().getId());
    }

    @Test
    @DisplayName("Al actualizar conservo el id original del registro")
    void actualizacionConservaElId() {
        Servicio existente = servicio(4L, "CN004");
        when(repository.findById(4L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Servicio.class))).thenAnswer(l -> l.getArgument(0));

        Servicio datos = servicio(99L, "CN004");
        datos.setPrecio(21000);
        Servicio resultado = service.actualizar(4L, datos);

        assertEquals(4L, resultado.getId());
        assertEquals(21000, resultado.getPrecio());
    }

    @Test
    @DisplayName("Puedo cambiar el código si el nuevo no está tomado")
    void cambioDeCodigoLibre() {
        when(repository.findById(4L)).thenReturn(Optional.of(servicio(4L, "CN004")));
        when(repository.existsByCodigoIgnoreCase("CN010")).thenReturn(false);
        when(repository.save(any(Servicio.class))).thenAnswer(l -> l.getArgument(0));

        assertEquals("CN010", service.actualizar(4L, servicio(4L, "CN010")).getCodigo());
    }

    @Test
    @DisplayName("No puedo cambiar el código a uno que ya usa otro servicio")
    void cambioDeCodigoOcupado() {
        when(repository.findById(4L)).thenReturn(Optional.of(servicio(4L, "CN004")));
        when(repository.existsByCodigoIgnoreCase("CN001")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(4L, servicio(4L, "CN001")));
    }

    @Test
    @DisplayName("Si el código no cambia no lo trato como duplicado de sí mismo")
    void mismoCodigoNoEsDuplicado() {
        when(repository.findById(4L)).thenReturn(Optional.of(servicio(4L, "CN004")));
        when(repository.save(any(Servicio.class))).thenAnswer(l -> l.getArgument(0));

        Servicio datos = servicio(4L, "CN004");
        datos.setNombre("Teleconsulta nutricional");
        assertEquals("Teleconsulta nutricional", service.actualizar(4L, datos).getNombre());
        // No tiene sentido consultar el duplicado cuando el código es el mismo.
        verify(repository, never()).existsByCodigoIgnoreCase(any());
    }

    @Test
    @DisplayName("Desactivar retira el servicio del catálogo sin eliminarlo")
    void desactivacion() {
        when(repository.findById(4L)).thenReturn(Optional.of(servicio(4L, "CN004")));
        when(repository.save(any(Servicio.class))).thenAnswer(l -> l.getArgument(0));

        Servicio datos = servicio(4L, "CN004");
        datos.setActivo(false);

        assertFalse(service.actualizar(4L, datos).isActivo());
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Un servicio que no existe responde no encontrado")
    void servicioInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscar(99L));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(99L, servicio(99L, "CN099")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));
    }

    @Test
    @DisplayName("El listado para el paciente trae solo los servicios activos")
    void listadoParaElPaciente() {
        when(repository.findByActivoTrue()).thenReturn(List.of(servicio(1L, "CN001")));

        assertEquals(1, service.listarActivos().size());
        // Compruebo que pido el listado filtrado y no todo el catálogo.
        verify(repository).findByActivoTrue();
        verify(repository, never()).findAll();
    }
}
