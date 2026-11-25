package cl.duoc.nutrivida.citas.service;

import cl.duoc.nutrivida.citas.model.Nutricionista;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

// Esta clase interpreta las dos columnas con las que la clínica describe cuándo atiende cada
// profesional: "dias_atencion" (por ejemplo "Lunes, Miércoles, Viernes" o "Lunes a Viernes") y
// "horario" (por ejemplo "09:00 - 17:00").
//
// La saqué de CitaService a propósito. Allá está el flujo de la cita; aquí está una sola idea:
// traducir un texto que escribe el administrador a algo con lo que pueda comparar fechas y horas.
// Así puedo leer cada archivo por separado y, si mañana la clínica guarda el horario de otra
// forma, el cambio queda en un solo lugar.
@Component
public class HorarioAtencion {

    // Los días tal como los escribe la clínica, en el orden de la semana. Uso este mismo orden
    // para resolver los rangos del tipo "Martes a Viernes": me basta con las posiciones.
    private static final List<String> DIAS = List.of(
            "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo");

    // Si la columna viniera vacía o escrita de una forma que no entiendo, prefiero no bloquear la
    // agenda entera: uso este horario, que es el de atención general de la clínica.
    private static final LocalTime APERTURA_POR_DEFECTO = LocalTime.of(9, 0);
    private static final LocalTime CIERRE_POR_DEFECTO = LocalTime.of(18, 0);

    // ¿Trabaja este profesional el día de esta fecha?
    public boolean atiendeEl(Nutricionista profesional, LocalDate fecha) {
        String texto = normalizar(profesional.getDiasAtencion());
        if (texto.isBlank()) {
            return true;
        }

        String diaBuscado = DIAS.get(fecha.getDayOfWeek().getValue() - 1);

        // Primera forma: un rango, "Lunes a Viernes". Miro las posiciones de los dos extremos y
        // compruebo si el día cae entre ambas.
        int separador = texto.indexOf(" a ");
        if (separador > 0) {
            int desde = DIAS.indexOf(texto.substring(0, separador).trim());
            int hasta = DIAS.indexOf(texto.substring(separador + 3).trim());
            if (desde >= 0 && hasta >= 0) {
                int dia = DIAS.indexOf(diaBuscado);
                return dia >= desde && dia <= hasta;
            }
        }

        // Segunda forma: una enumeración, "Lunes, Miércoles, Viernes". Basta con buscar el día.
        return List.of(texto.split("\\s*,\\s*")).contains(diaBuscado);
    }

    // Hora a la que abre el profesional.
    public LocalTime apertura(Nutricionista profesional) {
        return extremo(profesional.getHorario(), 0, APERTURA_POR_DEFECTO);
    }

    // Hora a la que cierra. La cita tiene que terminar antes de este momento, no solo empezar
    // antes: una consulta de 60 minutos a las 16:30 no cabe en una jornada que termina a las 17:00.
    public LocalTime cierre(Nutricionista profesional) {
        return extremo(profesional.getHorario(), 1, CIERRE_POR_DEFECTO);
    }

    // Saca una de las dos horas del texto "09:00 - 17:00". Si no logro leerla, devuelvo el valor
    // general de la clínica en vez de lanzar un error: el problema sería del dato, no de quien
    // está pidiendo la hora.
    private LocalTime extremo(String horario, int posicion, LocalTime porDefecto) {
        if (horario == null) {
            return porDefecto;
        }
        String[] partes = horario.split("\\s*-\\s*");
        if (partes.length != 2) {
            return porDefecto;
        }
        try {
            return LocalTime.parse(partes[posicion].trim());
        } catch (DateTimeParseException e) {
            return porDefecto;
        }
    }

    // Dejo el texto en minúsculas y sin tildes para poder compararlo sin depender de cómo lo
    // haya escrito quien cargó los datos.
    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.toLowerCase(new Locale("es"))
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').trim();
    }

    // Lo dejo público porque CitaService lo necesita para escribir el mensaje de error con el
    // nombre del día en que el paciente intentó agendar.
    public String nombreDelDia(DayOfWeek dia) {
        String nombre = DIAS.get(dia.getValue() - 1);
        return nombre.substring(0, 1).toUpperCase(new Locale("es")) + nombre.substring(1);
    }
}
