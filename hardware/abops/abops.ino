#include <SPI.h>
#include <Wire.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClient.h>
#include <NewPing.h>
#include <Arduino_JSON.h>

// Pin Definitions
byte triggerPin = 18;
byte echoPin = 19;
byte pumpPin = 22;
byte buzzerPin = 23;
byte sensorPin = 21;
byte LED = 27;

// Flow sensor variables
volatile int pulseCount = 0;
const float calibrationFactor = 7.5;
unsigned long lastTimeFlow = 0;
float flowRate = 0;
float totalLiters = 0; // Not sending totalLiters

// WiFi credentials
const char* ssid = "Sch! Phone";
const char* password = "passc0d9";
const char* serverName = "http://lui.co.zw/abops/post.php";
// API endpoints
const char* postAPI = "http://lui.co.zw/abops/post.php";
const char* ctrlAPI = "http://lui.co.zw/abops/ctrl.php?board=ABOPS_ID0001";
const char* notifyAPI = "http://lui.co.zw/abops/sendNotification.php";
const char* auto_hwAPI = "http://lui.co.zw/abops/auto_hw.php"; 
String apiKey = "tPmAT5Ab3j7F9";
String boardId = "ABOPS_ID0001";

const float tankVolume = 5000.0;
const float minWaterLevel = 500.0;
const float leakThreshold = 0.1;

// Ultrasonic sensor parameters
const float maxDistance = 200.0;

// Global Variables
float waterLevel;
float waterHeight;
bool leakStatus = false; 

unsigned long timerDelay = 5000;
unsigned long previousMillis = 0;
int interval = 5000;


NewPing sonar(triggerPin, echoPin, maxDistance);

void IRAM_ATTR pulseCounter();
void checkSensors();
void autoCtrl();
void postData(float waterLevel, float flowRate);
void postNotification(float waterLevel, float flowRate);
void controlPump();
void soundBuzzer();
void checkLeak();
void calculateFlow();
void getVolume();

void IRAM_ATTR pulseCounter() {
  pulseCount++;
}

void setup() {
  Serial.begin(115200);
  pinMode(sensorPin, INPUT_PULLUP);
  pinMode(pumpPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT);
  pinMode(LED, OUTPUT);
  digitalWrite(pumpPin, LOW);
  digitalWrite(buzzerPin, LOW);

  attachInterrupt(digitalPinToInterrupt(sensorPin), pulseCounter, FALLING);
  lastTimeFlow = millis();

  // Connect to WiFi
  WiFi.begin(ssid, password);
  Serial.println("Connecting to WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to WiFi network with IP Address: ");
  Serial.println(WiFi.localIP());
}

void loop() {
  checkSensors();
      unsigned long currentMillis = millis();

  if (currentMillis - previousMillis >= interval) {
        if (httpAutoStatus() == "1") {
          Serial.println("AUTO MODE DISABLED");
            appCtrl();
         
        } else if (httpAutoStatus() == "0") {
          Serial.println("AUTO MODE ENABLED");
            autoCtrl();
        } else {
            // OFFLINE DO SOMETHING
            Serial.println(F("OFFLINE: AUTO MODE ON"));
            autoCtrl();
        }

          // Send sensor data
    postData(waterLevel, flowRate);

    // Send notification
    postNotification(waterLevel, flowRate);

        // Update previousMillis to the current time
        previousMillis = currentMillis;
    }
}

void checkSensors() {
  calculateFlow();  // Calculate flow rate
  getVolume();      // Calculate water volume
 // checkLeak();      // Check for leakage
}










