package sri.project.sri_project.integration;


import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.event.RiegoFinalizadoEvent;
import sri.project.sri_project.service.serviceImpl.ProcesarDatosService;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Component
public class Esp32MqttSensor {

    private final Esp32MqttConnectionManager mqtt;
    private final ProcesarDatosService procesarDatosService;
    private final Esp32MqttControlRiego mqttControlRiego;
    private final ApplicationEventPublisher eventPublisher;

    @Setter
    private Consumer<SensorData> onDataReceived;

    @Getter
    private volatile SensorData ultimoDato;

    @Getter
    private volatile LocalDateTime ultimaLecturaEn;

    private MqttClient clienteSuscrito;

    public Esp32MqttSensor(
            Esp32MqttConnectionManager mqtt,
            ProcesarDatosService procesarDatosService,
            Esp32MqttControlRiego mqttControlRiego,
            ApplicationEventPublisher eventPublisher
    ) {
        this.mqtt = mqtt;
        this.procesarDatosService = procesarDatosService;
        this.mqttControlRiego = mqttControlRiego;
        this.eventPublisher = eventPublisher;
    }


    public synchronized void iniciar() {

        try {
            if (!mqtt.estaConectado() || mqtt.getClient() == null) {
                throw new IllegalStateException("MQTT debe estar conectado antes de suscribir los sensores.");
            }

            MqttClient client = mqtt.getClient();

            if (client == clienteSuscrito) {
                return;
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    clienteSuscrito = null;
                    System.err.println("[MQTT] Conexión perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {

                    String texto = new String(message.getPayload());
                    parsearLinea(texto);
                }

                @Override
                public void deliveryComplete(
                        IMqttDeliveryToken token
                ) {

                }
            });

            client.subscribe("upt/riego/datos");
            clienteSuscrito = client;

            System.out.println("[MQTT] Suscrito a sensores");

        } catch (MqttException e) {
            throw new IllegalStateException("[MQTT] Error al suscribir los sensores", e);
        }

    }

    private void parsearLinea(String linea) {

        try {

            String payload = linea == null ? "" : linea.trim();
            String[] partes = payload.split(",", -1);

            if (partes.length < 2 || partes[0].isBlank() || partes[1].isBlank()) {
                System.err.println("[MQTT] Payload de sensor invalido: " + payload);
                return;
            }

            int humedad = Integer.parseInt(partes[0].trim());

            double distancia = Double.parseDouble(partes[1].trim());

            Boolean bombaActiva = partes.length >= 3 && !partes[2].isBlank()
                    ? "1".equals(partes[2].trim())
                    : null;

            SensorData sensorData = new SensorData(humedad, distancia, bombaActiva);
            ultimoDato = sensorData;
            ultimaLecturaEn = LocalDateTime.now();
            procesarDatosService.procesar(sensorData);
            detectarTanqueVacio(sensorData);

            if (onDataReceived != null) {
                onDataReceived.accept(sensorData);
            }

            System.out.println(
                    "[ESP32] H="
                            + humedad
                            + "% | D="
                            + distancia
                            + "cm"
            );

        } catch (Exception e) {

            System.err.println(
                    "[MQTT] Error parseando"
            );

            e.printStackTrace();
        }
    }

    private void detectarTanqueVacio(SensorData sensorData) {
        if (sensorData.distancia() < 18 || !mqttControlRiego.isBombaActiva()) {
            return;
        }

        mqttControlRiego.confirmarBombaApagada();
        eventPublisher.publishEvent(new RiegoFinalizadoEvent("tanque_vacio", sensorData));
    }


}
