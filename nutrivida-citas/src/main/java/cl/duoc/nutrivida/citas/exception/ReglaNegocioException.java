package cl.duoc.nutrivida.citas.exception;

// La uso cuando el dato está bien escrito pero rompe una regla del negocio: por ejemplo pedir
// una hora que el profesional ya tiene tomada, o confirmar una cita que ya estaba cancelada.
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
