void controlPump() {
  if (waterLevel < minWaterLevel) {
    digitalWrite(pumpPin, LOW);
  } else {
    digitalWrite(pumpPin, HIGH);
  }
}

void soundBuzzer() {
  digitalWrite(buzzerPin, HIGH);
  delay(500);
  digitalWrite(buzzerPin, LOW);
}
