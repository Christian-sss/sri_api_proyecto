package sri.project.sri_project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import sri.project.sri_project.model.enums.EstadoRiego;
import sri.project.sri_project.model.enums.ModoRiego;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "eventos_riego")
public class EventoRiego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cultivo_id", nullable = false)
    private Cultivo cultivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_riego", nullable = false)
    private ModoRiego modoRiego;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "humedad_suelo_inicial", nullable = false)
    private Integer humedadSueloInicial;

    @Column(name = "humedad_suelo_final")
    private Integer humedadSueloFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoRiego estado;
}
