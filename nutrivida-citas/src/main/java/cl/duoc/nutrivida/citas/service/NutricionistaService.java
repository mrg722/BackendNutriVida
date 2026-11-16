package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.citas.exception.ReglaNegocioException;
import cl.duoc.nutrivida.citas.model.Nutricionista;
import cl.duoc.nutrivida.citas.repository.NutricionistaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// Lógica del directorio de profesionales que atienden en la clínica.
@Service
public class NutricionistaService {

    private final NutricionistaRepository repository;

    public NutricionistaService(NutricionistaRepository repository) {
        this.repository = repository;
    }

    public List<Nutricionista> listar() {
        return repository.findAll();
    }

    // El formulario de agendamiento usa este listado: nadie debe poder pedir hora con alguien
    // que ya no atiende.
    public List<Nutricionista> listarActivos() {
        return repository.findByActivoTrue();
    }

    public Nutricionista buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Nutricionista no encontrado: " + id));
    }

    // Con el correo del token encuentro la agenda que le corresponde a quien inició sesión.
    public Nutricionista buscarPorCorreo(String correo) {
        return repository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay un nutricionista registrado con el correo " + correo));
    }

    public Nutricionista crear(Nutricionista nutricionista) {
        // El código y el correo identifican al profesional, así que ninguno puede repetirse.
        if (repository.existsByCodigoIgnoreCase(nutricionista.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un nutricionista con el código "
                    + nutricionista.getCodigo());
        }
        if (repository.existsByCorreoIgnoreCase(nutricionista.getCorreo())) {
            throw new ReglaNegocioException("Ya existe un nutricionista con el correo "
                    + nutricionista.getCorreo());
        }
        nutricionista.setId(null);
        // Normalizo el correo a minúsculas para que siempre calce con el del token.
        nutricionista.setCorreo(nutricionista.getCorreo().trim().toLowerCase());
        return repository.save(nutricionista);
    }

    public Nutricionista actualizar(Long id, Nutricionista datos) {
        Nutricionista actual = buscar(id);
        String correo = datos.getCorreo().trim().toLowerCase();

        if (!actual.getCodigo().equalsIgnoreCase(datos.getCodigo())
                && repository.existsByCodigoIgnoreCase(datos.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un nutricionista con el código "
                    + datos.getCodigo());
        }
        if (!actual.getCorreo().equalsIgnoreCase(correo)
                && repository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un nutricionista con el correo " + correo);
        }

        actual.setCodigo(datos.getCodigo());
        actual.setNombre(datos.getNombre());
        actual.setCorreo(correo);
        actual.setEspecialidad(datos.getEspecialidad());
        actual.setDiasAtencion(datos.getDiasAtencion());
        actual.setHorario(datos.getHorario());
        actual.setActivo(datos.isActivo());

        return repository.save(actual);
    }

    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }
}
