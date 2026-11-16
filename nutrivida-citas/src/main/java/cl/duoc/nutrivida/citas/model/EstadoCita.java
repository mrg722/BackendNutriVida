package cl.duoc.nutrivida.citas.model;

import java.util.List;

// Reúno aquí los estados por los que pasa una cita, para no repetir los textos por el código.
public final class EstadoCita {

    // El paciente pidió la hora y todavía nadie la revisa.
    public static final String SOLICITADA = "SOLICITADA";
    // La secretaria o el nutricionista aceptó el horario.
    public static final String CONFIRMADA = "CONFIRMADA";
    // La cita ya ocurrió y quedó registrada la atención.
    public static final String ATENDIDA = "ATENDIDA";
    // El paciente o la clínica dieron de baja la hora antes de que ocurriera.
    public static final String CANCELADA = "CANCELADA";
    // El paciente no llegó: este estado es el que me permite calcular la tasa de inasistencia.
    public static final String NO_ASISTIO = "NO_ASISTIO";

    private EstadoCita() {
    }

    // Esta lista la uso para validar el estado que llega por la API y para armar las estadísticas.
    public static List<String> todos() {
        return List.of(SOLICITADA, CONFIRMADA, ATENDIDA, CANCELADA, NO_ASISTIO);
    }

    public static boolean esValido(String estado) {
        return estado != null && todos().contains(estado);
    }
}
