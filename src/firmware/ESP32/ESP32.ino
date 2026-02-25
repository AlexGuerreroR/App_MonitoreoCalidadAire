#include <WiFi.h>
#include <PubSubClient.h>
#include <WebServer.h>
#include <HTTPClient.h>
#include <Preferences.h>

#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_ILI9341.h>
#include "DHT.h"

// ---------- PANTALLA TFT ----------
#define TFT_CS   5
#define TFT_DC   21
#define TFT_RST  22
#define TFT_MOSI 23
#define TFT_MISO 19
#define TFT_SCK  18
Adafruit_ILI9341 tft(TFT_CS, TFT_DC, TFT_RST);

// ---------- SENSORES ----------
#define DHTPIN   4
#define DHTTYPE  DHT11
DHT dht(DHTPIN, DHTTYPE);
#define MQ135_PIN 35

// ---------- INTERVALOS (AJUSTE FINAL) ----------
const uint32_t SENSOR_INTERVAL_MS = 2500;   // Respetando el límite de hardware del DHT11
const uint32_t MQTT_INTERVAL_MS   = 2000;   
const uint32_t HTTP_INTERVAL_MS   = 30000;  

// ---------- VALORES ----------
float gTemp = NAN;
float gHum  = NAN;
bool  gDhtError = true;
int   gMqPpm = 0; 

// ---------- UMBRALES VISUALES Y CALIDAD ----------
const int MQ135_BASAL = 400;   // Aire fresco típico (PPM)
const int MQ135_MALO  = 2000;  // Nivel peligroso (PPM)
const int MQ135_MAX_VISUAL = 1500; // Alarma visual TFT

// ---------- ÍNDICE DE CALIDAD (LÓGICA PROFESIONAL) ----------
float calcularIndiceCalidadAire(float mq_ppm, float temp, float hum) {
  // 1. Riesgo por GAS (PPM). Este es el factor CRÍTICO.
  float gasScore = 0.0f;
  if (mq_ppm <= MQ135_BASAL)       gasScore = 0.0f;
  else if (mq_ppm >= MQ135_MALO)   gasScore = 100.0f;
  else gasScore = (mq_ppm - MQ135_BASAL) * 100.0f / (MQ135_MALO - MQ135_BASAL);

  // 2. Riesgo por DISCONFORT TÉRMICO
  float humScore = 0.0f;
  if (hum < 40.0f) {
    if (hum <= 20.0f) humScore = 100.0f;
    else humScore = (40.0f - hum) * 100.0f / 20.0f;
  } else if (hum > 60.0f) {
    if (hum >= 80.0f) humScore = 100.0f;
    else humScore = (hum - 60.0f) * 100.0f / 20.0f;
  }

  float tempScore = 0.0f;
  if (temp < 20.0f) {
    if (temp <= 16.0f) tempScore = 100.0f;
    else tempScore = (20.0f - temp) * 100.0f / 4.0f;
  } else if (temp > 26.0f) {
    if (temp >= 30.0f) tempScore = 100.0f;
    else tempScore = (temp - 26.0f) * 100.0f / 4.0f;
  }

  // 3. LA LÓGICA CORRECTA: El gas manda.
  float riesgoClima = (humScore + tempScore) / 2.0f;
  float riesgoFinal = gasScore; 
  
  if (riesgoClima > riesgoFinal) {
      riesgoFinal = riesgoClima * 0.4f; // Penalización máxima de 40% por clima
  }

  if (riesgoFinal > 100.0f) riesgoFinal = 100.0f;
  
  return 100.0f - riesgoFinal; // 100 = Perfecto, 0 = Peligroso
}

// ---------- WIFI AP (CONFIG) ----------
const char* AP_SSID = "SensorAire-ESP32";
const char* AP_PASS = "12345678";

// ---------- MQTT ----------
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
const char* MQTT_HOST = "broker.hivemq.com";
const uint16_t MQTT_PORT = 1883;

// ---------- GLOBALES ----------
Preferences prefs;
WebServer server(80);

String wifiSsid, wifiPass, deviceToken, serverUrl;

bool configurado = false;
bool enModoAP = false;
bool serverIniciado = false;

uint32_t lastWifiRetry = 0;
uint32_t lastMqttRetry = 0;

uint32_t lastSensorAt = 0;
uint32_t lastMqttAt   = 0;
uint32_t lastHttpAt   = 0;

String lastJson = "";

// ---------- TFT ----------
void drawWelcome() {
  tft.fillScreen(ILI9341_BLACK);
  tft.setRotation(1);
  tft.setTextColor(ILI9341_GREEN);
  tft.setTextSize(2);
  tft.setCursor(20, 40);  tft.print("Bienvenido");
  tft.setCursor(20, 70);  tft.print("Configura tu app");
  tft.setCursor(20, 100); tft.print("conectandote a:");
  tft.setCursor(20, 130); tft.print("WiFi: SensorAire-ESP32");
  tft.setCursor(20, 160); tft.print("Clave: 12345678");
}

void drawLayout() {
  tft.fillScreen(ILI9341_BLACK);
  tft.setRotation(1);

  tft.fillRect(0, 0, 320, 30, ILI9341_BLUE);
  tft.setTextColor(ILI9341_WHITE);
  tft.setTextSize(2);
  tft.setCursor(5, 7); tft.print("Monitoreo de Aire");

  tft.drawRect(10, 40, 180, 60, ILI9341_WHITE);
  tft.setCursor(20, 62); tft.print("Estado:");

  tft.drawRect(10, 110, 140, 40, ILI9341_WHITE);
  tft.drawRect(170, 110, 140, 40, ILI9341_WHITE);
  tft.drawRect(10, 160, 300, 40, ILI9341_WHITE);

  tft.setCursor(15, 115);  tft.print("Temp:");
  tft.setCursor(175, 115); tft.print("Hum:");
  tft.setCursor(15, 165);  tft.print("MQ135:");
}

void updateDisplay() {
  bool alarma = (gMqPpm > MQ135_MAX_VISUAL);

  uint16_t color = alarma ? ILI9341_RED : ILI9341_GREEN;
  tft.fillRect(110, 45, 70, 50, color);
  tft.setTextColor(ILI9341_BLACK);
  tft.setTextSize(2);
  tft.setCursor(125, 62); tft.print(alarma ? "ALR" : " OK");

  tft.fillRect(80, 110, 70, 40, ILI9341_BLACK);
  tft.setTextColor(ILI9341_YELLOW);
  tft.setTextSize(2);
  tft.setCursor(85, 115);
  if (gDhtError) tft.print("---");
  else { tft.print(gTemp, 1); tft.print("C"); }

  tft.fillRect(235, 110, 80, 40, ILI9341_BLACK);
  tft.setCursor(225, 115);
  if (gDhtError) tft.print("---");
  else { tft.print(gHum, 1); tft.print("%"); }

  tft.fillRect(110, 160, 200, 40, ILI9341_BLACK);
  tft.setCursor(115, 165);
  tft.print(gMqPpm);
  tft.print(" PPM est."); 
}

// ---------- MQ135 (CÁLCULO MATEMÁTICO A PPM) ----------
int leerMQPromedio() {
  long sum = 0;
  for (int i = 0; i < 20; i++) {
    sum += analogRead(MQ135_PIN);
    delay(2);
  }
  float adc_promedio = sum / 20.0f;
  
  if (adc_promedio == 0) return 400; 
  
  float voltaje = adc_promedio * (3.3f / 4095.0f);
  if (voltaje >= 3.3f) voltaje = 3.29f; 
  
  float RS_gas = ((3.3f * 10.0f) / voltaje) - 10.0f; 
  float R0 = 41.76f; 
  float ratio = RS_gas / R0;
  
  float ppm = 110.47f * pow(ratio, -2.862f);
  
  if (ppm < 400.0f) ppm = 400.0f; 
  if (ppm > 5000.0f) ppm = 5000.0f;

  return (int)ppm;
}

// ---------- SENSORES ----------
void leerSensores() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  if (isnan(h) || isnan(t)) {
    gDhtError = true;
  } else {
    gDhtError = false;
    gTemp = t;

    // Lectura directa sin alteraciones artificiales
    if (h < 0.0f) h = 0.0f;
    if (h > 100.0f) h = 100.0f;
    gHum = h;
  }

  gMqPpm = leerMQPromedio();
  updateDisplay();
}

String buildJson() {
  float co2 = (float)gMqPpm;
  float temp = gDhtError ? 0.0f : gTemp;
  float hum  = gDhtError ? 0.0f : gHum;
  float pm25 = 0.0f;

  float indice = calcularIndiceCalidadAire(co2, temp, hum);

  String json =
    String("{\"token\":\"") + deviceToken + "\"," +
    "\"co2\":" + String(co2, 1) + "," +
    "\"temperatura\":" + String(temp, 1) + "," +
    "\"humedad\":" + String(hum, 1) + "," +
    "\"pm25\":" + String(pm25, 1) + "," +
    "\"calidad_aire_indice\":" + String(indice, 1) +
    "}";

  return json;
}

// ---------- PREFERENCIAS ----------
void cargarConfig() {
  prefs.begin("config", true);
  wifiSsid    = prefs.getString("ssid", "");
  wifiPass    = prefs.getString("pass", "");
  deviceToken = prefs.getString("token", "");
  serverUrl   = prefs.getString("server", "");
  prefs.end();

  configurado = (wifiSsid.length() > 0) && (deviceToken.length() > 0) && (serverUrl.length() > 0);
}

void guardarConfig(const String& ssid, const String& pass, const String& token, const String& server) {
  prefs.begin("config", false);
  prefs.putString("ssid", ssid);
  prefs.putString("pass", pass);
  prefs.putString("token", token);
  prefs.putString("server", server);
  prefs.end();
}

void factoryReset() {
  prefs.begin("config", false);
  prefs.clear();
  prefs.end();
  delay(800);
  ESP.restart();
}

// ---------- HTTP CONFIG SERVER ----------
void handleRoot() {
  String html = "<html><body><h1>Sensor Aire ESP32</h1>"
                "<p>Configuracion via /config (POST).</p>"
                "<p>Factory reset via /factory_reset</p></body></html>";
  server.send(200, "text/html", html);
}

void handleConfig() {
  if (server.method() != HTTP_POST) {
    server.send(405, "application/json", "{\"success\":false,\"message\":\"method\"}");
    return;
  }

  String ssid  = server.arg("ssid");
  String pass  = server.arg("password");
  String token = server.arg("token");
  String serv  = server.arg("serverUrl");
  serv.trim();

  if (ssid == "" || token == "" || serv == "") {
    server.send(400, "application/json", "{\"success\":false,\"message\":\"missing\"}");
    return;
  }

  guardarConfig(ssid, pass, token, serv);
  server.send(200, "application/json", "{\"success\":true}");
  delay(800);
  ESP.restart();
}

void handleFactoryReset() {
  server.send(200, "application/json", "{\"success\":true}");
  delay(500);
  factoryReset();
}

void iniciarServidorHTTP() {
  if (serverIniciado) return;
  server.on("/", handleRoot);
  server.on("/config", handleConfig);
  server.on("/factory_reset", handleFactoryReset);
  server.begin();
  serverIniciado = true;
}

// ---------- MODO AP ----------
void iniciarModoAP() {
  if (enModoAP) return;
  enModoAP = true;
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASS);
  iniciarServidorHTTP();
  drawWelcome();
}

// ---------- WIFI STA ----------
void conectarWiFiSTA() {
  enModoAP = false;

  WiFi.mode(WIFI_STA);
  WiFi.begin(wifiSsid.c_str(), wifiPass.c_str());

  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED && intentos < 60) {
    delay(500);
    intentos++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    iniciarServidorHTTP();
    drawLayout();
  } else {
    iniciarModoAP();
  }
}

void asegurarWiFi() {
  if (enModoAP) return;
  if (WiFi.status() == WL_CONNECTED) return;

  uint32_t now = millis();
  if (now - lastWifiRetry < 5000) return;
  lastWifiRetry = now;

  WiFi.disconnect();
  WiFi.reconnect();
}

// ---------- MQTT ----------
void setupMqtt() {
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
}

void mqttReconnect() {
  if (enModoAP) return;
  if (WiFi.status() != WL_CONNECTED) return;
  if (mqttClient.connected()) return;
  if (deviceToken.length() == 0) return;

  String clientId = "AIRE_ESP32_" + deviceToken;
  clientId.replace(" ", "_");

  mqttClient.connect(clientId.c_str());
}

void mqttLoopSeguro() {
  if (enModoAP) return;
  if (WiFi.status() != WL_CONNECTED) return;

  if (!mqttClient.connected()) {
    uint32_t now = millis();
    if (now - lastMqttRetry > 8000) {
      lastMqttRetry = now;
      mqttReconnect();
    }
  }
  mqttClient.loop();
}

void mqttPublishTelemetry(const String& jsonData) {
  mqttReconnect();
  mqttClient.loop();
  if (!mqttClient.connected()) return;

  String topic = "monitoreoAire/telemetry/" + deviceToken;
  mqttClient.publish(topic.c_str(), jsonData.c_str());
}

// ---------- ENVÍO A BD (HTTP) ----------
void httpPostTelemetry(const String& jsonData) {
  if (enModoAP) return;
  if (WiFi.status() != WL_CONNECTED) return;
  if (!configurado) return;

  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "application/json");
  int code = http.POST(jsonData);

  if (code > 0) {
    String payload = http.getString();
    if (payload.indexOf("Token no válido") != -1 || payload.indexOf("Token no valido") != -1) {
      http.end();
      delay(800);
      factoryReset();
      return;
    }
  }
  http.end();
}

// ---------- SETUP / LOOP ----------
void setup() {
  Serial.begin(115200);
  delay(500);

  SPI.begin(TFT_SCK, TFT_MISO, TFT_MOSI);
  tft.begin();
  tft.setRotation(1);

  dht.begin();

  analogReadResolution(12);
  analogSetPinAttenuation(MQ135_PIN, ADC_11db);
  pinMode(MQ135_PIN, INPUT);

  setupMqtt();

  cargarConfig();
  if (!configurado) iniciarModoAP();
  else conectarWiFiSTA();

  lastSensorAt = millis();
  lastMqttAt   = millis();
  lastHttpAt   = millis();
}

void loop() {
  if (serverIniciado) server.handleClient();

  if (enModoAP) {
    delay(5);
    return;
  }

  asegurarWiFi();
  mqttLoopSeguro();

  uint32_t now = millis();

  if (now - lastSensorAt >= SENSOR_INTERVAL_MS) {
    lastSensorAt = now;
    leerSensores();
    lastJson = buildJson();
  }

  if (now - lastMqttAt >= MQTT_INTERVAL_MS) {
    lastMqttAt = now;
    if (lastJson.length() > 0) mqttPublishTelemetry(lastJson);
  }

  if (now - lastHttpAt >= HTTP_INTERVAL_MS) {
    lastHttpAt = now;
    if (lastJson.length() > 0) httpPostTelemetry(lastJson);
  }
}