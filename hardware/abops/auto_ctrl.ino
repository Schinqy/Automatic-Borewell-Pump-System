void autoCtrl() {
    checkSensors(); // Call to check the sensors
    digitalWrite(buzzerPin, leakStatus ? HIGH : LOW); // Turn on buzzer if leak detected

    if (leakStatus) {
        digitalWrite(pumpPin, LOW);  // Turn off the pump if there is a leak
    } else {
        // Control pump based on water level only if there is no leak
        if (waterLevel < minWaterLevel) {
            digitalWrite(pumpPin, HIGH);  // Turn on the pump if water level is low
        } else {
            digitalWrite(pumpPin, LOW);   // Turn off the pump if water level is sufficient
        }
    }
}
