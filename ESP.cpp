#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 32
#define OLED_RESET    -1       // no usamos pin de reset dedicado
#define OLED_ADDR     0x3C     // dirección I²C típica para 128×32

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// ——— Configuración Wi-Fi y MQTT ———
const char* ssid       = "ZTEV50";
const char* password   = "Jagdtiger8";
const char* mqttServer = "broker.hivemq.com";
const uint16_t mqttPort= 1883;

// ——— Pines ———
const uint8_t PIN_GREEN   = D7;  // GPIO13
const uint8_t PIN_WHITE   = D0;  // (no usado aquí)
const uint8_t PIN_RED     = D3;  // GPIO0
const uint8_t PIN_BUZZER = D8;  // GPIO15


const uint8_t BTN1        = D5;  // GPIO14
const uint8_t BTN2        = D6;  // GPIO12
const uint8_t BTN3        = D4; 


// ——— Variables de configuración ———
String symbol1, symbol2;
float  threshold1 = 0, threshold2 = 0;
uint32_t alarmDuration = 500;   // ms

// Moneda activa (la que disparará alarma)
String activeSymbol;

// Email configuration (instead of WhatsApp)
String notificationEmail = "";


// Para comparar precio anterior vs actual
float lastPrices[2] = { NAN, NAN };


// Para almacenar DX para Json
StaticJsonDocument<256> jsonDoc;

// ——— Conexión MQTT ———
WiFiClient   net;
PubSubClient mqtt(net);

// ——— Ayudará a cambiar suscripciones ———
String subscribedPriceTopics[2] = {"",""};

// ——— Forward declarations ——— TODO:AÑADIR LAS NUEVAS IMPLEMENTADAS
void connectWiFi();
void connectMQTT();
void applyConfiguration(const JsonObject& cfg);
void subscribePrices();
void onConfigMessage(char* topic, byte* payload, unsigned int len);
void onPriceMessage(char* topic, byte* payload, unsigned int len);
void checkButtons();
void onEmailConfigMessage(char* topic, byte* payload, unsigned int len);
// functions that were left to be put 
void onLedMessage(char* topic, byte* payload, unsigned int length);
void onBuzzerIntensity(byte* payload, unsigned int length);
void mqttCallback(char* topic, byte* payload, unsigned int len);



void setup() {
  Serial.begin(115200);
  delay(100);  
  Serial.println("Arrancó el ESP, Serial OK!");

  // Pines
  pinMode(PIN_GREEN,  OUTPUT);
  pinMode(PIN_WHITE, OUTPUT);
  pinMode(PIN_RED,    OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_GREEN, LOW);
  digitalWrite(PIN_RED,   LOW);
  digitalWrite(PIN_BUZZER,LOW);

  pinMode(BTN1, INPUT_PULLUP);
  pinMode(BTN2, INPUT_PULLUP);
  pinMode(BTN3, INPUT_PULLUP);

  analogWriteRange(1023); // buzzer config
  analogWriteFreq(1000); // buzzer config

    // --- Inicializar I2C y pantalla ---
  Wire.begin(D2, D1);  // SDA = D2 (GPIO4), SCL = D1 (GPIO5)
  if (!display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR)) {
    Serial.println("ERROR: OLED no encontrado");
    for (;;);  // nos detenemos si falla
  }
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("Iniciando...");
  display.display();
  delay(500);
  // es importante que la pantalla inicie antes de conectar al wifi, si no, se queda esperando la conexión y no da señales de vida

  // Wi-Fi y MQTT
  connectWiFi();
  mqtt.setServer(mqttServer, mqttPort);
  mqtt.setCallback([](char* t, byte* p, unsigned int l){
    String topic(t);
    if (topic == "config/monitor01")            onConfigMessage(t,p,l);
    else if (topic == subscribedPriceTopics[0] ||
             topic == subscribedPriceTopics[1]) onPriceMessage(t,p,l);
  });
  connectMQTT();
}

void loop() {
  if (!mqtt.connected()) connectMQTT();
  mqtt.loop();
  checkButtons();
}

// ——— Funciones ———

void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
  }
  Serial.println("WiFi conectado");
}

// Handle email configuration
void onEmailConfigMessage(char* topic, byte* payload, unsigned int len) {
  String jsonStr;
  for (unsigned int i = 0; i < len; i++) {
    jsonStr += char(payload[i]);
  }
  
  Serial.print("Email config received: ");
  Serial.println(jsonStr);

  DeserializationError error = deserializeJson(jsonDoc, jsonStr);
  if (error) {
    Serial.println("Error parsing email JSON");
    return;
  }
  
  JsonObject obj = jsonDoc.as<JsonObject>();
  notificationEmail = obj["email"].as<const char*>();
  
  Serial.print("Email saved: ");
  Serial.println(notificationEmail);

  // Confirmation beep
  tone(PIN_BUZZER, 1000);
  delay(1000);
  noTone(PIN_BUZZER);
}

void sendEmailNotification() {
  if (notificationEmail.length() == 0) {
    Serial.println("No email configured!");
    // Error beep
    for (int i = 0; i < 3; i++) {
      tone(PIN_BUZZER, 500);
      delay(200);
      noTone(PIN_BUZZER);
      delay(100);
    }
    return;
  }
  
  // Get current price for active symbol
  int idx = (activeSymbol == symbol1) ? 0 : 1;
  float currentPrice = lastPrices[idx];
  
  if (isnan(currentPrice)) {
    Serial.println("No price data available!");
    return;
  }
  
  // Create JSON payload for email notification
  StaticJsonDocument<300> doc;
  doc["to"] = notificationEmail;
  doc["subject"] = "Crypto Alert: " + activeSymbol;
  doc["symbol"] = activeSymbol;
  doc["price"] = currentPrice;
  doc["timestamp"] = millis(); // Simple timestamp
  
  char buffer[400];
  size_t messageSize = serializeJson(doc, buffer);
  
  // Publish to MQTT topic for email service
  if (mqtt.publish("notify/email", (uint8_t*)buffer, messageSize, false)) {
    Serial.println("Email notification sent: " + String(buffer));
    
    // Success beep
    tone(PIN_BUZZER, 1000);
    delay(500);
    noTone(PIN_BUZZER);
    
  } else {
    Serial.println("Failed to send email notification");
    
    // Error beep
    for (int i = 0; i < 2; i++) {
      tone(PIN_BUZZER, 300);
      delay(300);
      noTone(PIN_BUZZER);
      delay(200);
    }
  }
}
//changin buzzer state
void onBuzzerIntensity(byte* payload, unsigned int length) {
  // Convertir payload a int
  char buf[8];
  int len = min((int)length, 7);
  memcpy(buf, payload, len);
  buf[len] = '\0';
  int duty = atoi(buf);
  // Asegurar rango 0–1023
  duty = constrain(duty, 0, 1023);

  // Configurar PWM si no lo has hecho ya
  analogWrite(PIN_BUZZER, duty);

  Serial.printf("Buzzer intensity set to %d\n", duty);
}

//pin-view handle
void onLedMessage(char* topic, byte* payload, unsigned int length) {
  // Convertimos payload a String ("ON"/"OFF")
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += char(payload[i]);
  msg.trim();

  bool state = (msg.equalsIgnoreCase("ON"));

  if (String(topic) == "led/green") {
    digitalWrite(PIN_GREEN, state ? HIGH : LOW);
  }
  else if (String(topic) == "led/white") {
    digitalWrite(PIN_WHITE, state ? HIGH : LOW);
  }
  else if (String(topic) == "led/red") {
    digitalWrite(PIN_RED, state ? HIGH : LOW);
  }

  Serial.printf("LED %s -> %s\n", topic, state ? "ON" : "OFF");
}

// changelog: variable lenght changed to len
void mqttCallback(char* topic, byte* payload, unsigned int len) {
  String t = String(topic);

  if (t == "config/monitor01") {
    onConfigMessage(topic, payload, len);
  }
  else if (t.startsWith("crypto/")) {
    onPriceMessage(topic, payload, len);
  }
  // importante para pin-view
  else if (t == "led/green" || t == "led/white" || t == "led/red") {
    onLedMessage(topic, payload, len);
  }
  else if (t == "buzzer/intensity") {
    onBuzzerIntensity(payload, len);
  }
  else if (t == "config/email") {
    onEmailConfigMessage(topic, payload, len);
  }
  
}


void connectMQTT() { // MONITORING SUBSCRIPTIONS HERE
  mqtt.setServer(mqttServer, mqttPort);
  mqtt.setCallback(mqttCallback);

  while (!mqtt.connected()) {
    Serial.print("Intentando conectar MQTT… ");
    if (mqtt.connect("ESP8266Mon")) {
      Serial.println("¡Conectado!");
      
      // Aquí confirmas la suscripción
      if (mqtt.subscribe("config/monitor01")) {
        Serial.println("✔ Suscripción a config/monitor01 exitosa");
      }else {
        Serial.println("✖ Error al suscribir a config/monitor01");
      }

      if(mqtt.subscribe("led/green") && mqtt.subscribe("led/white") && mqtt.subscribe("led/red")){
        Serial.println("✔ Suscripción a led/... exitosa");

      }else {
        Serial.println("✖ Error al suscribir a led/... o uno de los LEDs");
      }

      if(mqtt.subscribe("buzzer/intensity")){
        Serial.println("✔ Suscripción a buzzer/intensity exitosa");

      }else {
        Serial.println("✖ Error al suscribir a buzzer/intensity");
      }

      if (mqtt.subscribe("config/email")) {
        Serial.println("✔ Suscripción a config/email exitosa");
      } else {
        Serial.println("✖ Error al suscribir a email ");
      }

    } else {
      Serial.print("Fallo (rc=");
      Serial.print(mqtt.state());
      Serial.println("). Reintentando en 1s");
      delay(1000);
    }
  }

}


// Procesa el JSON de configuración y resuscribe precios
void onConfigMessage(char* topic, byte* payload, unsigned int len) {
  Serial.print("Config recibida: ");
  String js;
  for (unsigned int i=0;i<len;i++) js += char(payload[i]);
  Serial.println(js);
  

  DeserializationError err = deserializeJson(jsonDoc, js);
  if (err) {
    Serial.println("Error parsing JSON");
    return;
  }
  JsonObject cfg = jsonDoc.as<JsonObject>();
  applyConfiguration(cfg);
  subscribePrices();

  Serial.println("Config aplicada, emitiendo beep de confirmación");
  tone(PIN_BUZZER, 1500);
  delay(500);
  noTone(PIN_BUZZER);
}

// Guarda variables y prepara topics
void applyConfiguration(const JsonObject& c) {
  symbol1 = c["button1"].as<const char*>();
  symbol2 = c["button2"].as<const char*>();
  threshold1 = c["thresholds"][symbol1].as<float>();
  threshold2 = c["thresholds"][symbol2].as<float>();
  alarmDuration = c["alarmDurationMs"].as<uint32_t>();

  // Inicializa moneda activa en symbol1
  activeSymbol = symbol1;

  // Prepara strings de suscripción
  subscribedPriceTopics[0] = String("crypto/") + symbol1;
  subscribedPriceTopics[1] = String("crypto/") + symbol2;

  Serial.printf("Asignado btn1→%s (umbral %.2f), btn2→%s (umbral %.2f), durAc %lu\n",
    symbol1.c_str(), threshold1,
    symbol2.c_str(), threshold2,
    alarmDuration
  );
}

// (Re)Suscribe a los dos tópicos de precio
void subscribePrices() {
  for (auto &t : subscribedPriceTopics) {
    mqtt.subscribe(t.c_str());
    Serial.println("Subscribed to "+t);
  }
}

// Lógica al recibir precio
void onPriceMessage(char* topic, byte* payload, unsigned int len) {
  // Convertir payload a float
  String pl; 
  for (unsigned int i = 0; i < len; i++) pl += char(payload[i]);
  float price = pl.toFloat();

  // Determinar índice (0 o 1) según el tópico
  int idx = -1;
  if (String(topic) == subscribedPriceTopics[0]) idx = 0;
  else if (String(topic) == subscribedPriceTopics[1]) idx = 1;
  else return;  // no es precio que nos importe

  float prev = lastPrices[idx];
  // Apagar todos antes de encender sólo uno
  digitalWrite(PIN_GREEN, LOW);
  digitalWrite(PIN_RED,   LOW);
  digitalWrite(PIN_WHITE, LOW);

  if (isnan(prev)) {
    // Primera lectura: consideramos “estable”
    digitalWrite(PIN_WHITE, HIGH);
  }
  else if (price > prev) {
    digitalWrite(PIN_GREEN, HIGH);
  }
  else if (price < prev) {
    digitalWrite(PIN_RED, HIGH);
  }
  else {
    digitalWrite(PIN_WHITE, HIGH);
  }

  // Guardar para la siguiente comparación
  lastPrices[idx] = price;

  // — ACTUALIZACIÓN DE PANTALLA SOLO SI ES LA MONEDA ACTIVA —
  if (String(topic).endsWith(activeSymbol)) {
    display.clearDisplay();
    display.setCursor(0, 0);
    display.setTextSize(1);
    display.print("Moneda: ");
    display.println(activeSymbol);
    display.print("Precio: ");
    display.println(price, 2);
    display.display();
  }

  display.display();

  // Debug 
  Serial.printf("LED %s: price=%.2f prev=%.2f → %s\n",
                topic, price, prev,
                price>prev ? "VERDE↗" : price<prev ? "ROJO↘" : "BLANCO→");

  

  // si hemos pasado el umbral, pitar brevemente
  // Determinar umbral según moneda activa
  float thr = (activeSymbol == symbol1) ? threshold1 : threshold2;

  // Si este mensaje es de la moneda activa y supera el umbral entonces que suene el buzzer
  if ( String(topic).endsWith(activeSymbol) && price >= thr ) {
    Serial.printf("Precio %.2f ≥ umbral %.2f → activando buzzer\n", price, thr);
    tone(PIN_BUZZER, 1000);
    delay(alarmDuration);
    noTone(PIN_BUZZER);
  }
}


// Cambia moneda activa según botones
void checkButtons() {
  static bool prev1 = HIGH, prev2 = HIGH, prev3 = HIGH;
  bool cur1 = digitalRead(BTN1), cur2 = digitalRead(BTN2), cur3 = digitalRead(BTN3);

  if (prev1==HIGH && cur1==LOW) {
    activeSymbol = symbol1;
    Serial.println("Activo → "+symbol1);
  }
  if (prev2==HIGH && cur2==LOW) {
    activeSymbol = symbol2;
    Serial.println("Activo → "+symbol2);
  }

  // Button 3: Send email notification
    if (prev3 == HIGH && cur3 == LOW) {
      sendEmailNotification();
    }

    prev1 = cur1;
    prev2 = cur2;
    prev3 = cur3;
}
