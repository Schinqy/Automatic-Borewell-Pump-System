void calculateFlow() {
  unsigned long currentTime = millis();
  if (currentTime - lastTimeFlow >= 1000) {
    detachInterrupt(sensorPin);
    flowRate = ((1000.0 / (currentTime - lastTimeFlow)) * pulseCount) / calibrationFactor;
    pulseCount = 0;
    lastTimeFlow = currentTime;
    attachInterrupt(digitalPinToInterrupt(sensorPin), pulseCounter, FALLING);

    Serial.print("Flow rate: ");
    Serial.print(flowRate);
    Serial.println(" L/min");
  }
}


void getVolume() {
  float ht = 150.0;
  float d = sonar.ping_cm();
  
  waterHeight = ht - d;
  waterLevel = (tankVolume * waterHeight) / ht;

  // Serial.print("Water Volume: ");
  // Serial.print(waterLevel);
  // Serial.println(" L");
}


void checkLeak() {
    static float previousFlowRate = 0;
    static unsigned long leakTimer = 0;

    // Set the leakStatus flag
    if (flowRate > 0 && abs(flowRate - previousFlowRate) > leakThreshold) {
        leakStatus = true; // Leak detected
        soundBuzzer();
        Serial.println("Potential leak detected!");
    } else {
        leakStatus = false; // No leak detected
    }

    previousFlowRate = flowRate;

    // Reset the buzzer if no leak is detected for 3 seconds
    if (millis() - leakTimer > 3000) {
        digitalWrite(buzzerPin, LOW);
        leakTimer = millis();
    }
}
