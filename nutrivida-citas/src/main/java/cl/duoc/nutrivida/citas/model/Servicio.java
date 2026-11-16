package cl.duoc.nutrivida.citas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Esta entidad es el catálogo de la clínica: consultas, planes, evaluaciones y talleres.
@Entity
@Table(name = "servicios")
@Getter
@Setter
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El código es el identificador del catálogo (CN001, PL001, EV001, TG001) y no se repite.
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 10, message = "El código no puede superar los 10 caracteres")
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    // Agrupo el servicio por tipo para poder filtrar el catálogo en el frontend.
    @NotBlank(message = "El tipo es obligatorio")
    @Size(max = 30, message = "El tipo no puede superar los 30 caracteres")
    @Column(nullable = false, length = 30)
    private String tipo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    // Dejo la duración en minutos y la permito nula, porque los planes mensuales no la tienen.
    @Min(value = 0, message = "La duración no puede ser negativa")
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @NotBlank(message = "La modalidad es obligatoria")
    @Size(max = 40, message = "La modalidad no puede superar los 40 caracteres")
    @Column(nullable = false, length = 40)
    private String modalidad;

    // Guardo el precio en pesos chilenos como entero: en Chile no se usan decimales.
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    @Column(nullable = false)
    private Integer precio;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 400, message = "La descripción no puede superar los 400 caracteres")
    @Column(nullable = false, length = 400)
    private String descripcion;

    // Con este campo retiro un servicio del catálogo sin borrar las citas que ya lo usaron.
    @Column(nullable = false)
    private boolean activo = true;
}
