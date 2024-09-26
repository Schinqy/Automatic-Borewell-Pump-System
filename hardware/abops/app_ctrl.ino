
// Function to control GPIOs based on JSON response
void appCtrl() {
  // Get the output states from the server
  String responsePayload = httpGET(ctrlAPI);
  Serial.println(responsePayload);

  // Parse the JSON response
  JSONVar parsedJson = JSON.parse(responsePayload);

  // Check if parsing was successful
  if (JSON.typeof(parsedJson) == "undefined") {
    Serial.println(F("Parsing JSON failed!"));
    return;
  }

  // Iterate over the keys in the JSON object
  JSONVar keys = parsedJson.keys();
  for (int i = 0; i < keys.length(); i++) {
    JSONVar pinKey = keys[i];
    JSONVar pinValue = parsedJson[pinKey];
    
    Serial.print(F("GPIO: "));
    Serial.print(pinKey);
    Serial.print(F(" - SET to: "));
    Serial.println(pinValue);
    
    // Convert the key to an integer (GPIO pin) and set it to the value
    int gpioPin = atoi(pinKey);
    int gpioState = atoi(pinValue);
    pinMode(gpioPin, OUTPUT);
    digitalWrite(gpioPin, gpioState);
  }
}



// Function to perform an HTTP GET request and return the response payload
String httpGET(const char* serverName) {
  String payload = "{}"; // Default payload in case of failure

  // Check WiFi connection status
  if (WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;

    // Begin the HTTP GET request
    http.begin(client, serverName);

    // Send the request and get the response code
    int httpResponseCode = http.GET();

    // Check if the response code indicates success
    if (httpResponseCode > 0) {
      Serial.print(F("HTTP Status: "));
      Serial.println(httpResponseCode);

      // Get the response payload
      payload = http.getString();
      Serial.print(F("Response: "));
      Serial.println(payload);
    } else {
        payload = "-1";
      Serial.print(F("Error Code: "));
      Serial.println(httpResponseCode);
    }

    // End the HTTP request
    http.end();
  } else {
    Serial.println(F("WiFi Disconnected"));
    payload = "DISCONNECTED";
  }

  return payload; // Return the response payload
}

