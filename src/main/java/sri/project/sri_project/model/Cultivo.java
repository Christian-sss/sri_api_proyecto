package sri.project.sri_project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "perfiles_cultivo")
public class Cultivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "humedad_min_optima", nullable = false)
    private Integer humedadMinOptima;

    @Column(name = "humedad_max_optima", nullable = false)
    private Integer humedadMaxOptima;

    @Column(name = "duracion_riego_minutos", nullable = false)
    private Integer duracionRiegoMinutos;

    @Column(name = "trato_recomendado", columnDefinition = "TEXT")
    private String tratoRecomendado;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
