package sri.project.sri_project.integration;


import org.springframework.stereotype.Component;

@Component
public class Esp32MqttControlRiego implements ControlRiego{

    private final Esp32MqttConnectionManager mqtt;
    private volatile boolean bombaActiva;


    public Esp32MqttControlRiego(Esp32MqttConnectionManager mqtt) {
        this.mqtt = mqtt;
    }

    @Override
    public void enviarComando(int comando) {


        if (!mqtt.estaConectado()) {
            throw new IllegalStateException("MQTT no esta conectado.");
        }

        mqtt.publish("upt/riego/orden", String.valueOf(comando));
        bombaActiva = comando == 1;

        System.out.println(
                "[MQTT] Comando enviado: "
                        + comando
        );




    }

    public boolean isBombaActiva() {
        return bombaActiva;
    }

    public void confirmarBombaApagada() {
        bombaActiva = false;
    }
}
