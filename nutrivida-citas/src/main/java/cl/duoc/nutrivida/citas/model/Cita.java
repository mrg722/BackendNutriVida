package cl.duoc.nutrivida.citas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

// Esta entidad es el "pedido" del caso: la solicitud de hora que hace un paciente.
@Entity
@Table(name = "citas")
@Getter
@Setter
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifico al paciente por el correo que viene dentro del token, no por un dato que él escriba.
    // Su nombre y teléfono viven en nutrivida-auth, así que aquí no los repito.
    @Column(name = "correo_paciente", nullable = false, length = 120)
    private String correoPaciente;

    // Llave foránea al profesional: uso LAZY para no traer el directorio en cada consulta.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutricionista_id", nullable = false)
    private Nutricionista nutricionista;

    // Llave foránea al catálogo: así el precio y la duración no se duplican en cada cita.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 300)
    private String motivo;

    // Aquí guardo en qué parte del flujo va la cita, usando las constantes de EstadoCita.
    @Column(nullable = false, length = 20)
    private String estado = EstadoCita.SOLICITADA;

    // Registro cuándo se pidió la hora: me sirve para las estadísticas por período.
    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();
}
