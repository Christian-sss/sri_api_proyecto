package sri.project.sri_project.service;

import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.model.enums.ModoRiego;

public interface EventoRiegoService {

    void registrarInicio(Integer cultivoId, ModoRiego modoRiego, SensorData lecturaInicial);

    void completarRiego(SensorData lecturaFinal);
}
