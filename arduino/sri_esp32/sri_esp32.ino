#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>

// =======================================================
// 1. PINES
// =======================================================
const int PIN_HUMEDAD = 34;
const int TRIG_PIN_ULTRASONICO = 5;
const int ECHO_PIN_ULTRASONICO = 18;
const int PIN_RELE = 26;

// =======================================================
// 2. CALIBRACION
// =======================================================
const int SECO = 2550;
const int AGUA = 930;
const int UMBRAL_VACIO = 18; // cm

// =======================================================
// 3. WIFI Y HIVEMQ
// =======================================================
const char* ssid = "JUANCA 2";
const char* password = "24067549";

const char* mqtt_server = "d293d13a6a7c49bbbc434365cac41121.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_user = "saulupt";
const char* mqtt_pass = "Sistema_riego123";

const char* TOPIC_ORDEN = "upt/riego/orden";
const char* TOPIC_DATOS = "upt/riego/datos";

WiFiClientSecure espClient;
PubSubClient client(espClient);

// =======================================================
// 4. VARIABLES
// =======================================================
bool hayAgua = false;
bool bombaEncendida = false;
unsigned long ultimaLectura = 0;
const unsigned long INTERVALO_LECTURA = 2000;

// =======================================================
// 5. CONTROL DE BOMBA
// =======================================================
void apagarBomba() {
  digitalWrite(PIN_RELE, HIGH);
  bombaEncendida = false;
  Serial.println(">> BOMBA DESACTIVADA");
}

void encenderBomba() {
  if (!hayAgua) {
    digitalWrite(PIN_RELE, HIGH);
    Serial.println(">> BLOQUEADO: TANQUE SIN AGUA");
    return;
  }

  digitalWrite(PIN_RELE, LOW);
  bombaEncendida = true;
  Serial.println(">> BOMBA ACTIVADA");
}

// =======================================================
// 6. RECEPCION DE COMANDOS MQTT
// =======================================================
void callback(char* topic, byte* payload, unsigned int length) {
  String comandoRecibido = "";

  for (unsigned int i = 0; i < length; i++) {
    comandoRecibido += (char)payload[i];
  }

  comandoRecibido.trim();

  Serial.println("Comando recibido: " + comandoRecibido);

  if (comandoRecibido == "1") {
    encenderBomba();
  } else if (comandoRecibido == "0") {
    apagarBomba();
  } else {
    Serial.println(">> COMANDO NO RECONOCIDO");
  }
}

// =======================================================
// 7. CONEXION WIFI
// =======================================================
void conectarWiFi() {
  Serial.print("Conectando a WiFi");

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi OK");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// =======================================================
// 8. CONEXION MQTT
// =======================================================
void reconnect() {
  while (!client.connected()) {
    Serial.print("Conectando a HiveMQ...");

    String clientId = "ESP32-SRI-" + String((uint32_t)ESP.getEfuseMac(), HEX);

    if (client.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
      Serial.println("CONECTADO");
      client.subscribe(TOPIC_ORDEN);

      Serial.print("Suscrito a: ");
      Serial.println(TOPIC_ORDEN);
    } else {
      Serial.print("fallo, rc=");
      Serial.print(client.state());
      Serial.println(" reintentando en 5s");
      delay(5000);
    }
  }
}

// =======================================================
// 9. LECTURA Y ENVIO DE DATOS
// =======================================================
void leerYEnviarDatos() {
  // Humedad
  int valorRaw = analogRead(PIN_HUMEDAD);
  int humedad = map(valorRaw, SECO, AGUA, 0, 100);
  humedad = constrain(humedad, 0, 100);

  // Ultrasonico
  digitalWrite(TRIG_PIN_ULTRASONICO, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN_ULTRASONICO, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN_ULTRASONICO, LOW);

  long duracion = pulseIn(ECHO_PIN_ULTRASONICO, HIGH, 30000);

  int distancia = 999;

  if (duracion > 0) {
    distancia = duracion * 0.034 / 2;
  }

  hayAgua = distancia < UMBRAL_VACIO;

  // Seguridad: si la bomba esta encendida y el tanque se vacio, apagar
  if (bombaEncendida && !hayAgua) {
    Serial.println("!! SEGURIDAD: TANQUE VACIO, BOMBA APAGADA");
    apagarBomba();
  }

  String payload = String(humedad)
    + "," + String(distancia)
    + "," + String(bombaEncendida ? 1 : 0); 

  bool enviado = client.publish(TOPIC_DATOS, payload.c_str());

  if (enviado) {
    Serial.println("Enviado: " + payload);
  } else {
    Serial.println("Error enviando datos MQTT");
  }
}

// =======================================================
// 10. SETUP
// =======================================================
void setup() {
  Serial.begin(115200);

  pinMode(TRIG_PIN_ULTRASONICO, OUTPUT);
  pinMode(ECHO_PIN_ULTRASONICO, INPUT);
  pinMode(PIN_RELE, OUTPUT);

  apagarBomba();

  conectarWiFi();

  espClient.setInsecure();

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

// =======================================================
// 11. LOOP
// =======================================================
void loop() {
  if (!client.connected()) {
    reconnect();
  }

  client.loop();

  unsigned long ahora = millis();

  if (ahora - ultimaLectura >= INTERVALO_LECTURA) {
    ultimaLectura = ahora;
    leerYEnviarDatos();
  }
}
