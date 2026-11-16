package cl.duoc.nutrivida.citas.exception;

// Lanzo esta excepción cuando pido una cita, un servicio o una medición que no existe.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        // Conservo el mensaje para devolverlo dentro del 404.
        super(mensaje);
    }
}
