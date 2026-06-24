package sri.project.sri_project.integration;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class Esp32MqttConnectionManager {

    @Value("${mqtt.broker:ssl://d293d13a6a7c49bbbc434365cac41121.s1.eu.hivemq.cloud:8883}")
    private String broker;

    @Value("${mqtt.client-id:JavaAppClient}")
    private String clientId;

    @Getter
    private MqttClient client;

    public synchronized void conectar(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario MQTT es obligatorio.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contrasena MQTT es obligatoria.");
        }

        if (estaConectado()) {
            throw new IllegalStateException("MQTT ya esta conectado.");
        }

        cerrarClienteAnterior();

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username.trim());
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);

            System.out.println("[MQTT] Conectando...");
            client.connect(options);
            System.out.println("[MQTT] Conexión exitosa");
        } catch (MqttException ex) {
            cerrarClienteAnterior();
            throw new IllegalStateException(
                    "Error al conectar con HiveMQ (codigo " + ex.getReasonCode() + "): " + mensajeSeguro(ex),
                    ex
            );
        }
    }

    public synchronized void desconectar() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect();
                System.out.println("MQTT Desconectado");
            }
        } catch (MqttException ex) {
            System.err.println("[MQTT] Error al desconectar: " + ex.getMessage());
        } finally {
            cerrarClienteAnterior();
        }
    }

    public void publish(String topic, String payload) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("El tópico MQTT es obligatorio.");
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("El payload MQTT es obligatorio.");
        }

        if (!estaConectado()) {
            throw new IllegalStateException("MQTT no está conectado.");
        }

        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic.trim(), message);
        } catch (MqttException e) {
            throw new IllegalStateException("[MQTT] Error publicando mensaje", e);
        }
    }

    public boolean estaConectado() {
        return client != null && client.isConnected();
    }

    private void cerrarClienteAnterior() {
        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (MqttException ex) {
            System.err.println("[MQTT] No se pudo cerrar el cliente anterior: " + ex.getMessage());
        } finally {
            client = null;
        }
    }

    private String mensajeSeguro(MqttException exception) {
        if (exception.getReasonCode() == 3) {
            return "el cluster HiveMQ no esta disponible; revise que este activo en HiveMQ Cloud";
        }

        if (exception.getReasonCode() == 4) {
            return "usuario o contrasena MQTT incorrectos";
        }

        if (exception.getReasonCode() == 5) {
            return "el usuario MQTT no tiene autorizacion para conectarse";
        }

        Throwable causa = exception.getCause();
        if (causa != null && causa.getMessage() != null && !causa.getMessage().isBlank()) {
            return causa.getMessage();
        }

        return exception.getMessage() != null ? exception.getMessage() : "causa desconocida";
    }
}
