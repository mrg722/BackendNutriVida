package cl.duoc.nutrivida.auth.exception;

// La uso cuando intentan registrar un correo que ya está tomado, para responder 409 y no 500.
public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String mensaje) {
        // Conservo el mensaje para mostrarle a la persona cuál fue el conflicto.
        super(mensaje);
    }
}
