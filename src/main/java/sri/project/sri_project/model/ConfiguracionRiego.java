package sri.project.sri_project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import sri.project.sri_project.model.enums.ModoOperacion;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "configuracion_riego")
public class ConfiguracionRiego {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_operacion", nullable = false, length = 20)
    private ModoOperacion modoOperacion;

    @ManyToOne
    @JoinColumn(name = "cultivo_activo_id")
    private Cultivo cultivoActivo;

    @Column(name = "hora_riego_programada")
    private LocalTime horaRiegoProgramada;
}
