package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.integration.Esp32MqttConnectionManager;
import sri.project.sri_project.integration.Esp32MqttControlRiego;
import sri.project.sri_project.integration.Esp32MqttSensor;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.service.AlertaRiegoService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EstadoVivoController {

    private final Esp32MqttConnectionManager mqttConnectionManager;
    private final Esp32MqttControlRiego mqttControlRiego;
    private final Esp32MqttSensor mqttSensor;
    private final AlertaRiegoService alertaRiegoService;

    @GetMapping("/api/estado-vivo")
    public Map<String, Object> obtenerEstadoVivo() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("mqtt_activo", mqttConnectionManager.estaConectado());
        SensorData ultimaLectura = mqttSensor.getUltimoDato();
        boolean bombaActiva = ultimaLectura != null && ultimaLectura.bombaActiva() != null
                ? ultimaLectura.bombaActiva()
                : mqttControlRiego.isBombaActiva();

        estado.put("bomba_activa", bombaActiva);
        estado.put("humedad", ultimaLectura != null ? ultimaLectura.humedad() : null);
        estado.put("distancia", ultimaLectura != null ? ultimaLectura.distancia() : null);
        estado.put("lectura_timestamp", mqttSensor.getUltimaLecturaEn());
        estado.put("alerta", alertaRiegoService.obtenerUltimaAlerta());
        estado.put("timestamp", LocalDateTime.now().toString());
        return estado;
    }

    @GetMapping("/api/mqtt/status")
    public Map<String, Boolean> obtenerEstadoMqtt() {
        return Map.of("conectado", mqttConnectionManager.estaConectado());
    }
}
