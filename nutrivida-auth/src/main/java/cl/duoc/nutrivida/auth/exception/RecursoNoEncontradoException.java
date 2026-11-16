package cl.duoc.nutrivida.auth.exception;

// Lanzo esta excepción cuando pido un usuario que no existe, para responder 404 y no 500.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        // Paso el mensaje al padre para que el manejador de errores lo pueda leer y devolver.
        super(mensaje);
    }
}
