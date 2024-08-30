#include <SPI.h>
#include <Wire.h>
#include <HCSR04.h>
#include <math.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClient.h>
#include <Arduino_JSON.h>

// Pin Definitions
byte triggerPin = 14;
byte echoPin = 13;
byte pumpPin = 12; // Pin to control the pump
byte buzzerPin = 15; // Pin for the buzzer
byte sensorPin = 2; // Flow sensor pin

// Constants
const char* ssid = "WaterMgmnt";
const char* password = "passc0d6";
const char* serverName = "http://146.190.22.58/api/system-data";

const float tankVolume = 5000.0; // Volume of the tank in liters
const float minWaterLevel = 500.0; // Minimum water level to turn on the pump (in liters)
const float leakThreshold = 0.1; // Allowable discrepancy in liters per minute for leak detection

// Global Variables
float waterLevel;
float waterHeight;
float inflowRate = 0.0;
float outflowRate = 0.0;
float volumeDispensed = 0.0;

unsigned long lastTime = 0;
unsigned long timerDelay = 5000; // 5 seconds
unsigned long pumpDelay = 10000; // Pump delay to turn it on/off every 10 seconds

long currentMillis = 0;
long previousMillis = 0;
int interval = 1000; // Update interval for flow rate
volatile byte pulseCount = 0;
float flowRate;
unsigned long flowMilliLitres;
unsigned int totalMilliLitres;
float flowLitres;
float totalLitres;

// Function Prototypes
void ICACHE_RAM_ATTR pulseCounter();
void setup();
void loop();
void getVolume();
void getFlow();
void postData();
void controlPump();
void checkLeak();
void soundBuzzer();

void ICACHE_RAM_ATTR pulseCounter() {
  pulseCount++;
}

void setup() {
  Serial.begin(9600);
  HCSR04.begin(triggerPin, echoPin);

  pinMode(sensorPin, INPUT_PULLUP);
  pinMode(pumpPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT);
  digitalWrite(pumpPin, LOW); // Ensure the pump is off initially
  digitalWrite(buzzerPin, LOW); // Ensure the buzzer is off initially

  pulseCount = 0;
  flowRate = 0.0;
  flowMilliLitres = 0;
  totalMilliLitres = 0;
  previousMillis = 0;

  attachInterrupt(digitalPinToInterrupt(sensorPin), pulseCounter, RISING);

  WiFi.begin(ssid, password);
  Serial.println("Connecting to WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to WiFi network with IP Address: ");
  Serial.println(WiFi.localIP());

  Serial.println("Setup complete. Timer set to 5 seconds.");
}

void loop() {
  digitalWrite(LED, HIGH);

  getFlow(); // Calculate flow rate
  float previousWaterLevel = waterLevel;
  getVolume(); // Calculate volume of water in tank

  checkLeak(previousWaterLevel, waterLevel); // Check for leaks

  if (waterLevel < 0) {
    Serial.println("Sensor Not Working");
  } else {
    postData(); // Post data to the server
    controlPump(); // Control pump based on water level
  }

  delay(500); // Short delay for stability
}

void getVolume() {
  float ht = 150.0; // Total height of the tank in cm

  double* distances = HCSR04.measureDistanceCm();
  float d = *distances;
  
  Serial.print("Distance: ");
  Serial.print(d);
  Serial.println(" cm");
  
  waterHeight = ht - d; // Calculate water height
  waterLevel = (tankVolume * waterHeight) / ht; // Calculate volume in liters

  Serial.print("Water Volume: ");
  Serial.print(waterLevel);
  Serial.println(" L");
}

void getFlow() {
  currentMillis = millis();
  if (currentMillis - previousMillis > interval) {
    pulseCount = 0;
    flowRate = ((1000.0 / (millis() - previousMillis)) * pulseCount) / calibrationFactor;
    previousMillis = millis();

    flowMilliLitres = (flowRate / 60) * 1000;
    flowLitres = (flowRate / 60);

    totalMilliLitres += flowMilliLitres;
    totalLitres += flowLitres;

    Serial.print("Flow rate: ");
    Serial.print(flowRate);
    Serial.print(" L/min\t");

    Serial.print("Total Quantity: ");
    Serial.print(totalMilliLitres);
    Serial.print(" mL / ");
    Serial.print(totalLitres);
    Serial.println(" L");

    inflowRate = flowRate; // Update inflow rate
  }
}

void postData() {
  if ((millis() - lastTime) > timerDelay) {
    if (WiFi.status() == WL_CONNECTED) {
      WiFiClient client;
      HTTPClient http;

      http.begin(client, serverName);
      http.addHeader("Content-Type", "application/json");

      String payload = "{\"water_level\":" + String(waterLevel) + ",\"flow_rate\":" + String(flowRate) + "}";
      Serial.println(payload);

      int httpResponseCode = http.POST(payload);

      Serial.print("HTTP Response code: ");
      Serial.println(httpResponseCode);

      if (httpResponseCode == 200) {
        digitalWrite(LED, HIGH);
        delay(300);
        digitalWrite(LED, LOW);
      } else {
        digitalWrite(LED, HIGH);
        delay(800);
        digitalWrite(LED, LOW);
      }

      String responsePayload = http.getString();
      Serial.println(responsePayload);

      http.end();
    } else {
      Serial.println("WiFi Disconnected");
    }
    lastTime = millis();
  }
}

void controlPump() {
  if (waterLevel < minWaterLevel) {
    digitalWrite(pumpPin, HIGH); // Turn on the pump
  } else {
    digitalWrite(pumpPin, LOW); // Turn off the pump
  }
}

void checkLeak(float previousVolume, float currentVolume) {
  volumeDispensed = totalLitres; // Total volume dispensed by the flow rate sensor

  // Calculate expected volume change
  float expectedVolumeChange = previousVolume - currentVolume;

  // Detect leak if the actual change in volume is greater than expected by the threshold
  if (abs(expectedVolumeChange - volumeDispensed) > leakThreshold) {
    Serial.println("Leak Detected!");
    soundBuzzer();
  }
}

void soundBuzzer() {
  digitalWrite(buzzerPin, HIGH);
  delay(1000); // Buzzer on for 1 second
  digitalWrite(buzzerPin, LOW);
}
