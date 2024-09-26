void postData(float waterLevel, float flowRate) {
  if (WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;

    http.begin(client, serverName);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"api_key\":\"" + apiKey + "\",\"water_level\":" + String(waterLevel) + ",\"flow_rate\":" + String(flowRate) + ",\"board_id\":\"" + boardId + "\"}";
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
  }
}

void postNotification(float waterLevel, float flowRate) {
  // Implement notification logic if needed
}



String httpAutoStatus(){
    // Construct the URL by concatenating parts as Strings
    String url = String(auto_hwAPI) + "?board_id=" + String(boardId);

    // Convert the String URL to const char*
    const char* urlChar = url.c_str();

    // Perform the HTTP GET request
    String payload = httpGET(urlChar);

    // Print the result for debugging
    if (payload == "-1") {
        Serial.println("Error retrieving auto status.");
    } else {
        Serial.println("Auto Status: " + payload);
    }

    return payload;
}
