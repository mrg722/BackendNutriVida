package cl.duoc.nutrivida.citas.config;

import cl.duoc.nutrivida.citas.model.Nutricionista;
import cl.duoc.nutrivida.citas.model.Servicio;
import cl.duoc.nutrivida.citas.repository.NutricionistaRepository;
import cl.duoc.nutrivida.citas.repository.ServicioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Dejo cargado el catálogo y el directorio tal como vienen en el documento del caso, para que
// la aplicación arranque con datos reales de la clínica y no con ejemplos inventados.
@Configuration
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarCatalogo(ServicioRepository servicios,
            NutricionistaRepository nutricionistas) {
        return args -> {
            // Solo cargo si las tablas están vacías: así no duplico nada al reiniciar.
            if (servicios.count() == 0) {
                cargarServicios(servicios);
            }
            if (nutricionistas.count() == 0) {
                cargarNutricionistas(nutricionistas);
            }
        };
    }

    private void cargarServicios(ServicioRepository servicios) {
        // Consultas: la atención individual que es el día a día de la clínica.
        servicios.save(servicio("CN001", "Consulta", "Primera consulta nutricional", 50,
                "Presencial", 35000,
                "Evaluación inicial: anamnesis, antropometría completa y diseño del primer "
                        + "plan alimenticio."));
        servicios.save(servicio("CN002", "Consulta", "Control nutricional (seguimiento)", 30,
                "Presencial", 25000,
                "Seguimiento mensual: medición de indicadores y ajuste del plan vigente."));
        servicios.save(servicio("CN003", "Consulta", "Control nutricional quincenal", 30,
                "Presencial", 22000,
                "Seguimiento intensivo cada 15 días. Recomendado en los primeros 2 meses."));
        servicios.save(servicio("CN004", "Consulta", "Teleconsulta nutricional", 30,
                "Online (video)", 20000,
                "Consulta de seguimiento vía videollamada. Requiere contar con consulta "
                        + "presencial previa."));
        servicios.save(servicio("CN005", "Consulta", "Consulta de urgencia / reagendada", 30,
                "Presencial", 28000,
                "Para pacientes que requieren atención fuera de su control habitual."));

        // Planes especializados: no tienen duración por sesión, por eso les paso null.
        servicios.save(servicio("PL001", "Plan especializado", "Plan pérdida de peso (1 mes)", null,
                "Presencial", 65000,
                "Incluye primera consulta + 1 control quincenal + plan alimenticio "
                        + "personalizado + seguimiento por WhatsApp."));
        servicios.save(servicio("PL002", "Plan especializado", "Plan pérdida de peso (3 meses)",
                null, "Presencial", 170000,
                "Incluye primera consulta + 5 controles + 3 planes mensuales + seguimiento "
                        + "continuo."));
        servicios.save(servicio("PL003", "Plan especializado", "Plan nutrición deportiva (1 mes)",
                null, "Presencial", 70000,
                "Para deportistas y personas con actividad física frecuente. Cálculo de "
                        + "requerimientos energéticos y proteicos."));
        servicios.save(servicio("PL004", "Plan especializado", "Plan control diabetes / hipertensión",
                null, "Presencial", 75000,
                "Plan adaptado para patologías metabólicas. Coordinación con médico tratante "
                        + "si aplica."));
        servicios.save(servicio("PL005", "Plan especializado",
                "Plan alimentación vegetariana/vegana", null, "Presencial", 68000,
                "Diseñado para garantizar aporte adecuado de proteínas, hierro, vitamina B12 "
                        + "y calcio sin productos animales."));
        servicios.save(servicio("PL006", "Plan especializado",
                "Plan alimentación infantil (2-12 años)", null, "Presencial", 65000,
                "Evaluación nutricional pediátrica y diseño de plan adaptado a la etapa de "
                        + "desarrollo del niño."));

        // Evaluaciones: las mediciones que el nutricionista anotaba a mano en la ficha de papel.
        servicios.save(servicio("EV001", "Evaluación", "Antropometría completa", 20,
                "Presencial", 18000,
                "Peso, talla, IMC, circunferencia de cintura, cadera, brazo y % de grasa "
                        + "corporal con bioimpedanciometría."));
        servicios.save(servicio("EV002", "Evaluación", "Bioimpedanciometría", 15,
                "Presencial", 12000,
                "Medición de composición corporal: masa grasa, masa muscular, agua corporal "
                        + "y edad metabólica."));
        servicios.save(servicio("EV003", "Evaluación", "Encuesta de hábitos alimentarios", 20,
                "Presencial", 10000,
                "Análisis del patrón alimentario actual. Identificación de déficit y excesos "
                        + "nutricionales."));
        servicios.save(servicio("EV004", "Evaluación", "Análisis de exámenes de laboratorio", 20,
                "Presencial", 15000,
                "Interpretación de hemograma, perfil bioquímico y lipídico en contexto "
                        + "nutricional."));

        // Talleres grupales: atención en grupo, con cupos limitados.
        servicios.save(servicio("TG001", "Taller grupal", "Taller de alimentación saludable", 90,
                "Presencial (grupo)", 15000,
                "Máx. 10 personas. Conceptos básicos de alimentación equilibrada y lectura "
                        + "de etiquetas."));
        servicios.save(servicio("TG002", "Taller grupal", "Taller de cocina nutritiva", 120,
                "Presencial (grupo)", 20000,
                "Preparación de recetas saludables. Incluye degustación. Máx. 8 personas."));
        servicios.save(servicio("TG003", "Taller grupal", "Taller nutrición para deportistas", 90,
                "Presencial (grupo)", 18000,
                "Hidratación, nutrición pre y post entrenamiento, suplementación básica. "
                        + "Máx. 12 personas."));
    }

    private void cargarNutricionistas(NutricionistaRepository nutricionistas) {
        // Los correos son los mismos que carga nutrivida-auth: ese es el vínculo entre
        // la cuenta con la que el profesional inicia sesión y su agenda.
        nutricionistas.save(nutricionista("NUT001", "Nut. Carolina Fuentes M.",
                "carolina.fuentes@nutrivida.cl", "Obesidad y síndrome metabólico",
                "Lunes, Miércoles, Viernes", "09:00 - 17:00"));
        nutricionistas.save(nutricionista("NUT002", "Nut. Rodrigo Sepúlveda A.",
                "rodrigo.sepulveda@nutrivida.cl", "Nutrición deportiva y rendimiento",
                "Martes, Jueves, Sábado", "09:00 - 14:00"));
        nutricionistas.save(nutricionista("NUT003", "Nut. Daniela Morales C.",
                "daniela.morales@nutrivida.cl",
                "Alimentación vegetariana, vegana y trastornos alimentarios",
                "Lunes a Viernes", "08:00 - 13:00"));
        nutricionistas.save(nutricionista("NUT004", "Nut. Felipe Araya R.",
                "felipe.araya@nutrivida.cl", "Nutrición pediátrica y familiar",
                "Martes a Viernes", "14:00 - 19:00"));
    }

    // Estos dos métodos solo arman el objeto: los uso para que la carga de arriba se lea corrida.
    private Servicio servicio(String codigo, String tipo, String nombre, Integer duracion,
            String modalidad, Integer precio, String descripcion) {
        Servicio servicio = new Servicio();
        servicio.setCodigo(codigo);
        servicio.setTipo(tipo);
        servicio.setNombre(nombre);
        servicio.setDuracionMinutos(duracion);
        servicio.setModalidad(modalidad);
        servicio.setPrecio(precio);
        servicio.setDescripcion(descripcion);
        servicio.setActivo(true);
        return servicio;
    }

    private Nutricionista nutricionista(String codigo, String nombre, String correo,
            String especialidad, String diasAtencion, String horario) {
        Nutricionista profesional = new Nutricionista();
        profesional.setCodigo(codigo);
        profesional.setNombre(nombre);
        profesional.setCorreo(correo);
        profesional.setEspecialidad(especialidad);
        profesional.setDiasAtencion(diasAtencion);
        profesional.setHorario(horario);
        profesional.setActivo(true);
        return profesional;
    }
}
