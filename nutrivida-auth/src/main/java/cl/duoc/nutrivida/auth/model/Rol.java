package cl.duoc.nutrivida.auth.model;

// Reúno aquí los tres roles del sistema para no repetir los textos sueltos por el código.
public final class Rol {

    public static final String ADMIN = "ADMIN";
    public static final String NUTRICIONISTA = "NUTRICIONISTA";
    public static final String PACIENTE = "PACIENTE";

    // Dejo el constructor privado porque esta clase solo agrupa constantes.
    private Rol() {
    }

    // Compruebo que el rol recibido sea uno de los tres permitidos antes de guardarlo.
    public static boolean esValido(String rol) {
        return ADMIN.equals(rol) || NUTRICIONISTA.equals(rol) || PACIENTE.equals(rol);
    }
}
