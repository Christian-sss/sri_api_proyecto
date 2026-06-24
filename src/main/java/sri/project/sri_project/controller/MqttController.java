package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.integration.Esp32MqttConnectionManager;
import sri.project.sri_project.integration.Esp32MqttSensor;

import java.util.Map;

@RestController
@RequestMapping("/api/mqtt")
@RequiredArgsConstructor
public class MqttController {

    private final Esp32MqttConnectionManager mqttManager;
    private final Esp32MqttSensor mqttSensor;

    @GetMapping
    public Map<String, Object> obtenerEstado() {
        return Map.of(
                "conectado", mqttManager.estaConectado(),
                "estado", mqttManager.estaConectado() ? "CONECTADO" : "DESCONECTADO"
        );
    }

    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(@RequestBody MqttCredentialsRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios."));
        }

        try {
            mqttManager.conectar(request.username(), request.password());
            try {
                mqttSensor.iniciar();
            } catch (Exception subscriptionException) {
                mqttManager.desconectar();
                throw new IllegalStateException(
                        "HiveMQ acepto la conexion, pero fallo la suscripcion a upt/riego/datos: "
                                + obtenerCausa(subscriptionException),
                        subscriptionException
                );
            }

            return ResponseEntity.ok(Map.of("mensaje", "Conexion MQTT establecida correctamente."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No se pudo conectar a MQTT: " + obtenerCausa(e)));
        }
    }

    @PostMapping("/disconnect")
    public Map<String, String> disconnect() {
        mqttManager.desconectar();
        return Map.of("mensaje", "Conexion MQTT cerrada correctamente.");
    }

    private String obtenerCausa(Throwable error) {
        Throwable actual = error;
        String mensaje = error.getMessage();

        while (actual.getCause() != null) {
            actual = actual.getCause();
            if (actual.getMessage() != null && !actual.getMessage().isBlank()) {
                mensaje = actual.getMessage();
            }
        }

        return mensaje != null && !mensaje.isBlank() ? mensaje : "causa desconocida";
    }

    private record MqttCredentialsRequest(String username, String password) {
    }
}
