package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.dto.EstadisticasResumenResponse;
import sri.project.sri_project.dto.RiegoEstadoResponse;
import sri.project.sri_project.dto.TelemetriaResponse;
import sri.project.sri_project.integration.Esp32MqttConnectionManager;
import sri.project.sri_project.integration.Esp32MqttControlRiego;
import sri.project.sri_project.integration.Esp32MqttSensor;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.service.AlertaRiegoService;
import sri.project.sri_project.service.EstadisticasService;
import sri.project.sri_project.service.RiegoControlService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EstadisticasService estadisticasService;
    private final Esp32MqttConnectionManager mqttConnectionManager;
    private final Esp32MqttControlRiego mqttControlRiego;
    private final Esp32MqttSensor mqttSensor;
    private final AlertaRiegoService alertaRiegoService;
    private final RiegoControlService riegoControlService;

    @GetMapping
    public Map<String, Object> obtenerDashboard() {
        SensorData ultimaLectura = mqttSensor.getUltimoDato();
        boolean bombaActiva = ultimaLectura != null && ultimaLectura.bombaActiva() != null
                ? ultimaLectura.bombaActiva()
                : mqttControlRiego.isBombaActiva();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resumen", obtenerResumenSeguro());
        response.put("mqttActivo", mqttConnectionManager.estaConectado());
        response.put("bombaActiva", bombaActiva);
        response.put("humedad", ultimaLectura != null ? ultimaLectura.humedad() : null);
        response.put("distancia", ultimaLectura != null ? ultimaLectura.distancia() : null);
        response.put("lecturaTimestamp", mqttSensor.getUltimaLecturaEn());
        response.put("alerta", alertaRiegoService.obtenerUltimaAlerta());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/live")
    public Map<String, Object> obtenerDashboardLive() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dashboard", obtenerDashboard());
        response.put("estadoRiego", riegoControlService.obtenerEstado());
        response.put("telemetria", estadisticasService.obtenerTelemetriaReciente());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    private EstadisticasResumenResponse obtenerResumenSeguro() {
        try {
            return estadisticasService.obtenerResumen();
        } catch (Exception e) {
            return null;
        }
    }
}
