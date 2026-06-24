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
@Table(name = "lecturas_sensor")
public class LecturaSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "humedad_suelo", nullable = false)
    private Integer humedadSuelo;

    @Column(name = "distancia_agua", nullable = false)
    private Double distanciaAgua;

    @Column(name = "cultivo_id")
    private Integer cultivoId;

    @Column(name = "fecha_lectura", insertable = false, updatable = false)
    private LocalDateTime fechaLectura;
}
