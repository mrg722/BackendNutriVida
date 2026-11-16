package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.citas.exception.ReglaNegocioException;
import cl.duoc.nutrivida.citas.model.Servicio;
import cl.duoc.nutrivida.citas.repository.ServicioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// Aquí vive la lógica del catálogo. El controlador solo recibe HTTP y delega en estos métodos.
@Service
public class ServicioService {

    private final ServicioRepository repository;

    // Pido el repositorio por constructor: es la dependencia obligatoria de esta clase.
    public ServicioService(ServicioRepository repository) {
        this.repository = repository;
    }

    // Listado completo, incluidos los servicios dados de baja: lo necesita el panel de administración.
    public List<Servicio> listar() {
        return repository.findAll();
    }

    // Listado para el paciente: solo lo que la clínica sigue ofreciendo.
    public List<Servicio> listarActivos() {
        return repository.findByActivoTrue();
    }

    public List<Servicio> listarPorTipo(String tipo) {
        return repository.findByTipoIgnoreCase(tipo);
    }

    public Servicio buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
    }

    public Servicio crear(Servicio servicio) {
        // El código identifica el servicio en el catálogo, así que no permito repetirlo.
        if (repository.existsByCodigoIgnoreCase(servicio.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un servicio con el código "
                    + servicio.getCodigo());
        }
        // Fuerzo el id a nulo para que un POST nunca sobrescriba un registro existente.
        servicio.setId(null);
        return repository.save(servicio);
    }

    public Servicio actualizar(Long id, Servicio datos) {
        // Busco primero: así, si no existe, respondo 404 antes de tocar nada.
        Servicio actual = buscar(id);

        // Solo compruebo el duplicado si de verdad cambió el código.
        if (!actual.getCodigo().equalsIgnoreCase(datos.getCodigo())
                && repository.existsByCodigoIgnoreCase(datos.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un servicio con el código "
                    + datos.getCodigo());
        }

        // Copio campo por campo y conservo el id original del registro.
        actual.setCodigo(datos.getCodigo());
        actual.setTipo(datos.getTipo());
        actual.setNombre(datos.getNombre());
        actual.setDuracionMinutos(datos.getDuracionMinutos());
        actual.setModalidad(datos.getModalidad());
        actual.setPrecio(datos.getPrecio());
        actual.setDescripcion(datos.getDescripcion());
        actual.setActivo(datos.isActivo());

        return repository.save(actual);
    }

    public void eliminar(Long id) {
        // Reutilizo buscar para que un id inexistente devuelva 404 en lugar de un error genérico.
        repository.delete(buscar(id));
    }
}
