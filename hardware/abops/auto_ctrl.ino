void autoCtrl() {
    checkSensors(); // Call to check the sensors
    digitalWrite(buzzerPin, leakStatus ? HIGH : LOW); // Turn on buzzer if leak detected

    if (leakStatus) {
        digitalWrite(pumpPin, HIGH);  // Turn off the pump if there is a leak
    } else {
        // Control pump based on water level only if there is no leak
        if (waterLevel < minWaterLevel) {
            digitalWrite(pumpPin, LOW);  // Turn on the pump if water level is low
        } else {
            digitalWrite(pumpPin, HIGH);   // Turn off the pump if water level is sufficient
        }
    }
}
