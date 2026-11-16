package cl.duoc.nutrivida.citas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Esta entidad es la ficha clínica digital: reemplaza la hoja de papel del archivador.
@Entity
@Table(name = "mediciones")
@Getter
@Setter
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El paciente dueño de la medición. Con este dato controlo que nadie vea fichas ajenas.
    @Column(name = "correo_paciente", nullable = false, length = 120)
    private String correoPaciente;

    // El profesional que tomó la medición: solo él y el paciente pueden verla.
    @Column(name = "correo_nutricionista", nullable = false, length = 120)
    private String correoNutricionista;

    @Column(nullable = false)
    private LocalDate fecha;

    // Uso Double porque el peso y las medidas corporales llevan decimales.
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "talla_cm", nullable = false)
    private Double tallaCm;

    @Column(name = "cintura_cm")
    private Double cinturaCm;

    // No guardo el IMC: es un dato derivado del peso y la talla, y guardarlo rompería la 3FN.
    // Lo calculo al momento de responder, en MedicionResponse.

    @Column(length = 400)
    private String observaciones;

    // Aquí queda el plan alimenticio que antes se entregaba impreso y el paciente podía perder.
    @Column(name = "plan_alimenticio", length = 1000)
    private String planAlimenticio;
}
