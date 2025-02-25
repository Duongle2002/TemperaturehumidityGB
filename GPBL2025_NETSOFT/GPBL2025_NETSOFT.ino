#include <DHT.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <TM1637Display.h>

// Cấu hình Firebase
#define FIREBASE_HOST "https://temperaturehumidity-aca88-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define FIREBASE_AUTH "1tnrYtqASw3FTtLIvpoE76KoDSx1Ea9YbnagpKGV"

// Wi-Fi
const char* ssid = "Redmi K60E";
const char* password = "passlachi";

// Chân kết nối
#define DHT_PIN 14
#define DHTTYPE DHT11
#define BUZZER_PIN 15
#define PIR_PIN 26
#define LED_WHITE 2
#define LED_WHITE2 4
#define BUTTON_PIN 13
#define CLK 19
#define DIO 18
#define WATER_SENSOR_PIN 34  // ADC trên ESP32

DHT dht(DHT_PIN, DHTTYPE);
TM1637Display display(CLK, DIO);
FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

unsigned long lastUpdate = 0;
unsigned long lastDisplaySwitch = 0;
const unsigned long updateInterval = 2000;
const unsigned long displaySwitchInterval = 5000;
const unsigned long debounceDelay = 300;

bool manualMode = false; // Mặc định chế độ tự động
unsigned long lastButtonPress = 0;
float lastTemperature = 0;
float lastHumidity = 0;

void setup() {
    Serial.begin(115200);
    dht.begin();

    pinMode(BUTTON_PIN, INPUT_PULLUP);
    pinMode(LED_WHITE, OUTPUT);
    pinMode(LED_WHITE2, OUTPUT);
    pinMode(PIR_PIN, INPUT);
    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(WATER_SENSOR_PIN, INPUT);

    display.setBrightness(7);
    display.showNumberDec(0);

    connectToWiFi();
    setupFirebase();
}

void loop() {
    unsigned long now = millis();

    handleButtonPress(now);
    readAndSendSensorData(now);
    updateDisplay(now);
    int rainValue = analogRead(WATER_SENSOR_PIN);

    // Lắng nghe manualMode từ Firebase
    if (Firebase.getBool(firebaseData, "/manualMode")) {
        manualMode = firebaseData.boolData();
    }

    // Lắng nghe trạng thái LED_WHITE từ Firebase
    if (Firebase.getBool(firebaseData, "/light")) {
        bool firebaseLightState = firebaseData.boolData();
        if (!manualMode) {
            digitalWrite(LED_WHITE, firebaseLightState ? HIGH : LOW);
        }
    }

    // Điều khiển LED_WHITE2 bằng cảm biến PIR khi ở chế độ tự động
    if (!manualMode) {
        int motionDetected = digitalRead(PIR_PIN);
        digitalWrite(LED_WHITE2, motionDetected ? HIGH : LOW);
    }

    bool isRaining = (rainValue > 2000); // Cập nhật giá trị phù hợp với ESP32 ADC (0 - 4095)

    // Gửi dữ liệu lên Firebase
    if (Firebase.setBool(firebaseData, "/RainSensor/isRaining", isRaining)) {
        Serial.println("Gửi dữ liệu thành công!");
    } else {
        Serial.println("Lỗi gửi dữ liệu!");
        Serial.println(firebaseData.errorReason());
    }
}

void handleButtonPress(unsigned long now) {
    if (digitalRead(BUTTON_PIN) == LOW && (now - lastButtonPress > debounceDelay)) {
        delay(50); // Chống dội nút
        if (digitalRead(BUTTON_PIN) == LOW) {
            lastButtonPress = now;
            manualMode = !manualMode;

            if (manualMode) {
                digitalWrite(LED_WHITE, HIGH);
                Firebase.setBool(firebaseData, "/manualMode", true);
                Firebase.setBool(firebaseData, "/light", true);
                Serial.println("🔘 Chế độ TAY (Bật LED_WHITE)");
            } else {
                digitalWrite(LED_WHITE, LOW);
                Firebase.setBool(firebaseData, "/manualMode", false);
                Firebase.setBool(firebaseData, "/light", false);
                Serial.println("🔘 Chế độ TỰ ĐỘNG (Đèn điều khiển bằng Firebase & PIR)");
            }
        }
    }
}

void readAndSendSensorData(unsigned long now) {
    if (now - lastUpdate >= updateInterval) {
        lastUpdate = now;
        float h = dht.readHumidity();
        float t = dht.readTemperature();
        if (isnan(h) || isnan(t)) {
            Serial.println("❌ Lỗi đọc DHT11!");
            return;
        }
        lastTemperature = t;
        lastHumidity = h;
        Serial.printf("🌡 %.1f°C | 💧 %.1f%%\n", t, h);
        sendDataToFirebase(t, h, digitalRead(PIR_PIN));
    }
}

void updateDisplay(unsigned long now) {
    if (now - lastDisplaySwitch >= displaySwitchInterval) {
        lastDisplaySwitch = now;
        display.showNumberDec((int)(lastDisplaySwitch % 2 == 0 ? lastTemperature : lastHumidity));
    }
}

void sendDataToFirebase(float temperature, float humidity, int motion) {
    FirebaseJson json;
    json.set("temperature", temperature);
    json.set("humidity", humidity);
    json.set("motion", motion ? "Có người" : "Không có người");
    json.set("led_white", manualMode ? "ON" : (motion ? "ON" : "OFF"));

    if (Firebase.setJSON(firebaseData, "/sensor", json)) {
        Serial.println("✅ Gửi Firebase thành công!");
    } else {
        Serial.println("❌ Lỗi Firebase: " + firebaseData.errorReason());
    }
}

void connectToWiFi() {
    Serial.print("🔄 Kết nối Wi-Fi...");
    WiFi.begin(ssid, password);
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n✅ Wi-Fi đã kết nối!");
        Serial.print("IP: ");
        Serial.println(WiFi.localIP());
    } else {
        Serial.println("\n❌ Không kết nối Wi-Fi!");
    }
}

void setupFirebase() {
    Serial.println("🔄 Kết nối Firebase...");
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    Serial.println("✅ Firebase kết nối thành công!");
}
