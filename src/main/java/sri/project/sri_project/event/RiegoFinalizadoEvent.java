package sri.project.sri_project.event;

import sri.project.sri_project.dto.SensorData;

public class RiegoFinalizadoEvent {

    private final String motivo;
    private final SensorData lecturaFinal;

    public RiegoFinalizadoEvent(String motivo, SensorData lecturaFinal) {
        this.motivo = motivo;
        this.lecturaFinal = lecturaFinal;
    }

    public String getMotivo() {
        return motivo;
    }

    public SensorData getLecturaFinal() {
        return lecturaFinal;
    }
}
