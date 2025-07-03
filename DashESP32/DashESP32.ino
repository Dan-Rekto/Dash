#include <WiFi.h>
#include <ESP32Time.h>
#define LED_BUILTIN 2 
// Ganti dengan SSID & password Wi‑Fi kamu
const char* ssid     = "CBN";
const char* password = "12345678";
const long  gmtOffset_sec = 7 * 3600;
const int   daylightOffset_sec = 0;


// Port TCP yang sama dengan di Android: 8888
const uint16_t TCP_PORT = 8888;

// GPIO pin untuk switchAuto dan switchSiram
const uint8_t PIN_AUTO  = 15;  
const uint8_t PIN_SIRAM = 27;  // misal GPIO27

WiFiServer server(TCP_PORT);

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  pinMode(PIN_AUTO, OUTPUT);
  pinMode(LED_BUILTIN,  OUTPUT);
  digitalWrite(LED_BUILTIN,  LOW);
  digitalWrite(PIN_AUTO,  LOW);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
  }
  Serial.println("\nWiFi connected: " + WiFi.localIP().toString());

  // NTP sync
  configTime(gmtOffset_sec, daylightOffset_sec, "pool.ntp.org", "time.nist.gov");

  // Tunggu hingga sinkron (max ~10 detik)
  time_t now = time(nullptr);
  int retries = 0;
  while (now < 100000 && retries < 20) {
    delay(500);
    Serial.print('.');
    now = time(nullptr);
    retries++;
  }
  if (now < 100000) {
    Serial.println("\nTime sync failed!");
  } else {
    struct tm tmNow;
    localtime_r(&now, &tmNow);
    Serial.printf("\nTime sync: %04d-%02d-%02d %02d:%02d:%02d\n",
                  tmNow.tm_year + 1900, tmNow.tm_mon + 1, tmNow.tm_mday,
                  tmNow.tm_hour, tmNow.tm_min, tmNow.tm_sec);
  }

  server.begin();
  Serial.printf("TCP server listening on port %u\n", TCP_PORT);
}

  int getHour() {
  time_t now = time(nullptr);
  struct tm timeinfo;
  localtime_r(&now, &timeinfo);
  return timeinfo.tm_hour;
}
int getMinute() {
  time_t now = time(nullptr);
  struct tm timeinfo;
  localtime_r(&now, &timeinfo);
  return timeinfo.tm_min;
}

void loop() {

    while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
    WiFi.begin(ssid, password);
  }
  // Cek apakah ada client baru
  WiFiClient client = server.available();
  if (!client) {
    delay(10);
    return;
  }
if (WiFi.status() == WL_CONNECTED){
  digitalWrite(LED_BUILTIN,  HIGH);
}
  Serial.println("Client connected");
  // Tunggu hingga data tersedia (timeout ~2 detik)
  uint32_t start = millis();
  while (!client.available() && millis() - start < 2000) {
    delay(1);
  }

  if (client.available()) {
    char cmd = client.read();  // baca satu karakter
    Serial.printf("Received command: %c\n", cmd);

    switch (cmd) {
      case '1':
        if (getHour() == 10 && getMinute() == 0 || getHour() == 16 && getMinute() == 0){
          digitalWrite(PIN_AUTO, HIGH);
        }else{
          digitalWrite(PIN_AUTO, LOW);
        }
        Serial.println("Auto ON");
       Serial.print("Pin = ");
Serial.println(digitalRead(PIN_AUTO));
        break;
      case '0':
        digitalWrite(PIN_AUTO, LOW);
        Serial.println("Auto OFF");
        Serial.print("Pin = ");
Serial.println(digitalRead(PIN_AUTO));
        break;
      case 'A':
      digitalWrite(PIN_AUTO, HIGH);
        Serial.println("Siram ON");
        Serial.print("Pin = ");
Serial.println(digitalRead(PIN_AUTO));
        break;
      case 'B':
        digitalWrite(PIN_AUTO, LOW);
        Serial.println("Siram OFF");
        Serial.print("Pin = ");
Serial.println(digitalRead(PIN_AUTO));
        break;
      default:
        Serial.println("Unknown command");
        Serial.print("Pin = ");
Serial.println(digitalRead(PIN_AUTO));
        break;
    }
  } else {
    Serial.println("No data received");
  }


}

