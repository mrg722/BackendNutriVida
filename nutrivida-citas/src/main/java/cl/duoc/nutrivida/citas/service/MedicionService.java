package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.dto.MedicionRequest;
import cl.duoc.nutrivida.citas.dto.MedicionResponse;
import cl.duoc.nutrivida.citas.exception.RecursoNoEncontradoException;
import cl.duoc.nutrivida.citas.model.Medicion;
import cl.duoc.nutrivida.citas.repository.MedicionRepository;
import cl.duoc.nutrivida.citas.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

// Esta es la ficha clínica digital. El caso dice que peso, medidas y diagnósticos son datos
// sensibles, y que solo el paciente y su nutricionista pueden verlos: esa regla la aplico aquí.
@Service
public class MedicionService {

    private final MedicionRepository repository;
    private final UsuarioAutenticado usuario;

    public MedicionService(MedicionRepository repository, UsuarioAutenticado usuario) {
        this.repository = repository;
        this.usuario = usuario;
    }

    // Historial del propio paciente: el correo lo tomo del token, no de la URL.
    public List<MedicionResponse> listarMias() {
        return repository.findByCorreoPacienteIgnoreCaseOrderByFechaDesc(usuario.correo())
                .stream().map(MedicionResponse::desde).toList();
    }

    // Historial de un paciente concreto. Solo lo devuelvo si quien pregunta tiene derecho a verlo.
    public List<MedicionResponse> listarPorPaciente(String correoPaciente) {
        // El paciente puede pedir su propia ficha aunque escriba su correo en la URL.
        if (usuario.esPaciente() && !correoPaciente.equalsIgnoreCase(usuario.correo())) {
            throw new AccessDeniedException("Solo puedes ver tu propia ficha clínica");
        }

        List<Medicion> mediciones =
                repository.findByCorreoPacienteIgnoreCaseOrderByFechaDesc(correoPaciente);

        // Un nutricionista solo ve las mediciones que él mismo registró: así no accede a los
        // datos clínicos de pacientes de otros profesionales de la clínica.
        if (usuario.esNutricionista()) {
            mediciones = mediciones.stream()
                    .filter(medicion -> medicion.getCorreoNutricionista()
                            .equalsIgnoreCase(usuario.correo()))
                    .toList();
        }

        return mediciones.stream().map(MedicionResponse::desde).toList();
    }

    // Lo que registró el profesional que inició sesión: es su propia carpeta de fichas.
    public List<MedicionResponse> listarRegistradasPorMi() {
        return repository.findByCorreoNutricionistaIgnoreCaseOrderByFechaDesc(usuario.correo())
                .stream().map(MedicionResponse::desde).toList();
    }

    public MedicionResponse buscar(Long id) {
        Medicion medicion = obtener(id);
        verificarPuedeVer(medicion);
        return MedicionResponse.desde(medicion);
    }

    // Registro de una sesión. La ficha queda firmada con el correo del profesional del token.
    public MedicionResponse registrar(MedicionRequest request) {
        Medicion medicion = new Medicion();
        copiarDatos(medicion, request);
        medicion.setCorreoNutricionista(usuario.correo());
        return MedicionResponse.desde(repository.save(medicion));
    }

    // Corrección de una medición ya registrada, por si el profesional anotó un valor equivocado.
    public MedicionResponse actualizar(Long id, MedicionRequest request) {
        Medicion medicion = obtener(id);
        verificarPuedeModificar(medicion);
        copiarDatos(medicion, request);
        return MedicionResponse.desde(repository.save(medicion));
    }

    public void eliminar(Long id) {
        Medicion medicion = obtener(id);
        verificarPuedeModificar(medicion);
        repository.delete(medicion);
    }

    // Reúno la copia de campos en un método para no repetirla en crear y actualizar.
    private void copiarDatos(Medicion medicion, MedicionRequest request) {
        medicion.setCorreoPaciente(request.correoPaciente().trim().toLowerCase());
        medicion.setFecha(request.fecha());
        medicion.setPesoKg(request.pesoKg());
        medicion.setTallaCm(request.tallaCm());
        medicion.setCinturaCm(request.cinturaCm());
        medicion.setObservaciones(request.observaciones());
        medicion.setPlanAlimenticio(request.planAlimenticio());
    }

    private Medicion obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medición no encontrada: " + id));
    }

    // Puede ver la ficha el administrador, el paciente dueño y el profesional que la escribió.
    private void verificarPuedeVer(Medicion medicion) {
        if (usuario.esAdmin()
                || medicion.getCorreoPaciente().equalsIgnoreCase(usuario.correo())
                || medicion.getCorreoNutricionista().equalsIgnoreCase(usuario.correo())) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para ver esta medición");
    }

    // Para modificar soy más estricto: solo el autor de la medición o el administrador.
    private void verificarPuedeModificar(Medicion medicion) {
        if (usuario.esAdmin()
                || medicion.getCorreoNutricionista().equalsIgnoreCase(usuario.correo())) {
            return;
        }
        throw new AccessDeniedException("Solo el nutricionista que registró la medición puede "
                + "modificarla");
    }
}
