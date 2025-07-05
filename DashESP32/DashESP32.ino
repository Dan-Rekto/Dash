#include <WiFi.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>

// ——— Wi‑Fi credentials ———————————————————————————————
const char* ssid     = "CBN";
const char* password = "12345678";

// ——— MQTT broker & credentials ————————————————————————
const char* mqtt_host = "b11b4954395c46ceae511d12f7916b17.s1.eu.hivemq.cloud";
const uint16_t mqtt_port = 8883;
const char* mqttUser = "Ddash";
const char* mqttPass = "Smamda123";

// ——— ISRG Root X1 CA — raw string literal —————————————————
static const char* ca_cert = R"EOF(
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv
KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn
OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn
jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw
qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI
rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV
HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq
hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL
ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ
3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK
NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5
ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur
TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC
jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc
oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq
4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA
mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d
emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=
-----END CERTIFICATE-----
)EOF";

// ——— GPIO pins ————————————————————————————————————————
#define PIN_AUTO      15
#define LED_BUILTIN    2

WiFiClientSecure secureClient;
PubSubClient    mqttClient(secureClient);

// forward declarations
void    connectMQTT();
void    mqttCallback(char* topic, byte* payload, unsigned len);

void setup() {
  Serial.begin(115200);
  delay(10);
  Serial.println("\n\n=== ESP32 MQTT TLS Debug ===");

  // 1) Wi‑Fi
  Serial.printf("Wi‑Fi: Connecting to %s\n", ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
  }
  Serial.println("\nWi‑Fi up, IP=" + WiFi.localIP().toString());
secureClient.setCACert(ca_cert);
  // 2) TLS time (required!)
  configTime(7 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  Serial.print("Waiting NTP sync");
  int retries = 0;
  time_t now = time(nullptr);
  while (now < 8*3600 && retries++ < 20) {
    delay(500);
    Serial.print('.');
    now = time(nullptr);
  }
  if (now < 8*3600) {
    Serial.println("\n! Time sync failed, TLS may not work");
  } else {
    Serial.println("\nTime is synced");
  }

  // 4) setup MQTT client
  mqttClient.setServer(mqtt_host, mqtt_port);
  mqttClient.setCallback(mqttCallback);

  // 5) init pins
  pinMode(PIN_AUTO, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(PIN_AUTO, LOW);
  digitalWrite(LED_BUILTIN, LOW);

  // 6) connect MQTT
  connectMQTT();
}

void loop() {
  // blink LED if Wi‑Fi is up
  digitalWrite(LED_BUILTIN, WiFi.status() == WL_CONNECTED);

  // ensure MQTT connected
  if (!mqttClient.connected()) {
    connectMQTT();
  }
  mqttClient.loop();
}

// — reconnect + subscribe —————————————————————————
void connectMQTT() {
  Serial.printf("MQTT: Connecting to %s:%u …\n", mqtt_host, mqtt_port);
  while (!mqttClient.connected()) {
    String clientId = "esp32-";
    clientId += String(random(0xffff), HEX);

    bool ok = mqttClient.connect(clientId.c_str(), mqttUser, mqttPass);
    if (ok) {
      Serial.println("→ MQTT connected!");
      // subscribe
      if (mqttClient.subscribe("Ddash/apk")) {
        Serial.println("→ Subscribed to Ddash/apk");
      } else {
        Serial.println("! Subscribe FAILED");
      }
    } else {
      Serial.printf("! Connect failed, rc=%d, retrying in 5s\n", mqttClient.state());
      delay(5000);
    }
  }
}

// — incoming message handler —————————————————————————
void mqttCallback(char* topic, byte* payload, unsigned len) {
  String msg;
  for (unsigned i = 0; i < len; i++) msg += (char)payload[i];
  Serial.printf("← %s : %s\n", topic, msg.c_str());

  if (String(topic) == "Ddash/apk") {
    if (msg == "A") {
      Serial.println("→ CMD=A: PIN_AUTO HIGH");
      digitalWrite(PIN_AUTO, HIGH);
      // publish timestamp
      time_t t = time(nullptr);
      struct tm tm;
      localtime_r(&t, &tm);
      char buf[32];
      int L = snprintf(buf, sizeof(buf),
        "%04d-%02d-%02d %02d:%02d:%02d",
        tm.tm_year+1900, tm.tm_mon+1, tm.tm_mday,
        tm.tm_hour,    tm.tm_min,    tm.tm_sec
      );
      Serial.printf("→ Publishing timestamp: %s\n", buf);
      if (mqttClient.publish("Ddash/esp", buf, L)) {
        Serial.println("→ Publish OK");
      } else {
        Serial.println("! Publish FAILED");
      }
    }
    else if (msg == "B") {
      Serial.println("→ CMD=B: PIN_AUTO LOW");
      digitalWrite(PIN_AUTO, LOW);
    }
    else {
      Serial.println("! Unknown CMD");
    }
  }
}
