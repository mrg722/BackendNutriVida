package cl.duoc.nutrivida.citas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Esta entidad es el directorio de los cuatro profesionales que atienden en la clínica.
@Entity
@Table(name = "nutricionistas")
@Getter
@Setter
public class Nutricionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Conservo el código del directorio del caso: NUT001 a NUT004.
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 10, message = "El código no puede superar los 10 caracteres")
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    // Este correo es el mismo con el que el profesional inicia sesión en nutrivida-auth.
    // No puedo poner una llave foránea entre microservicios, así que el correo es el vínculo.
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 120, message = "La especialidad no puede superar los 120 caracteres")
    @Column(nullable = false, length = 120)
    private String especialidad;

    // Guardo los días y el horario tal como los publica el directorio de la clínica.
    @NotBlank(message = "Los días de atención son obligatorios")
    @Size(max = 80, message = "Los días de atención no pueden superar los 80 caracteres")
    @Column(name = "dias_atencion", nullable = false, length = 80)
    private String diasAtencion;

    @NotBlank(message = "El horario es obligatorio")
    @Size(max = 40, message = "El horario no puede superar los 40 caracteres")
    @Column(nullable = false, length = 40)
    private String horario;

    // Con esto saco de la agenda a un profesional que deja de atender, sin perder su historial.
    @Column(nullable = false)
    private boolean activo = true;
}
