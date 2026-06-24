package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.integration.Esp32MqttSensor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final Esp32MqttSensor mqttSensor;

    @GetMapping("/ultima-lectura")
    public Map<String, Object> obtenerUltimaLectura() {
        SensorData ultimaLectura = mqttSensor.getUltimoDato();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("humedad", ultimaLectura != null ? ultimaLectura.humedad() : null);
        response.put("distancia", ultimaLectura != null ? ultimaLectura.distancia() : null);
        response.put("bombaActiva", ultimaLectura != null ? ultimaLectura.bombaActiva() : null);
        response.put("lecturaTimestamp", mqttSensor.getUltimaLecturaEn());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
