package com.myesp32upgrade.microcontrol.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dropdown state
    val boardOptions = listOf("ESP32", "ESP8266")
    var selectedBoard by remember { mutableStateOf(boardOptions[0]) }
    var boardExpanded by remember { mutableStateOf(false) }

    val connectionOptions = listOf("Bluetooth", "WiFi")
    var selectedConnection by remember { mutableStateOf(connectionOptions[0]) }
    var connectionExpanded by remember { mutableStateOf(false) }
    
    val esp32WifiCode = """
#include <WiFi.h>
#include <WebServer.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// --- WiFi Credentials ---
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// --- Web Server ---
WebServer server(80);

// --- OLED 128x64 setup (optional) ---
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// --- Pin config (customize as needed) ---
const int LED_PINS[] = {12, 14};              // Add more if needed
const int SERVO_PINS[] = {13, 15};            // Servo1, Servo2
const int NUM_LEDS = sizeof(LED_PINS) / sizeof(LED_PINS[0]);
const int NUM_SERVOS = sizeof(SERVO_PINS) / sizeof(SERVO_PINS[0]);
Servo servos[NUM_SERVOS];                     // Array of Servo objects

// --- OLED Text and Scroll Setup ---
String displayText = "";
int verticalScrollIndex = 0;
const int lineHeight = 8; // 8 pixels per line for text size 1

void setup() {
  Serial.begin(115200);

  // --- Connect to Wi-Fi ---
  WiFi.begin(ssid, password);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConnected to Wi-Fi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // --- Setup LEDs ---
  for (int i = 0; i < NUM_LEDS; i++) {
    pinMode(LED_PINS[i], OUTPUT);
    digitalWrite(LED_PINS[i], LOW);
  }

  // --- Setup Servos ---
  for (int i = 0; i < NUM_SERVOS; i++) {
    servos[i].setPeriodHertz(50);
    servos[i].attach(SERVO_PINS[i], 500, 2400);
    servos[i].write(0);
  }

  // --- OLED setup (optional) ---
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("OLED not found"));
    // Comment out the next line if you don't use OLED
    // while (true);
  } else {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("ESP32 Ready");
    display.display();
  }

  // --- Setup Web Server ---
  server.on("/control", HTTP_POST, handleControl);
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  server.handleClient();
}

void handleControl() {
  if (server.method() != HTTP_POST) {
    server.send(405, "text/plain", "Method Not Allowed");
    return;
  }

  // Print the raw JSON received for debugging
  Serial.print("Received JSON: ");
  Serial.println(server.arg("plain"));

  StaticJsonDocument<1024> doc;
  DeserializationError error = deserializeJson(doc, server.arg("plain"));
  if (error) {
    server.send(400, "text/plain", "Invalid JSON");
    Serial.print("JSON parse error: ");
    Serial.println(error.c_str());
    return;
  }

  // --- Handle connection test ---
  if (doc.containsKey("action") && doc["action"] == "connect_test") {
    server.send(200, "text/plain", "done");
    Serial.println("Connection test received, responded with 'done'");
    return;
  }

  // --- Handle batch actions ---
  if (!doc.containsKey("actions")) {
    server.send(400, "text/plain", "Missing actions");
    Serial.println("Missing 'actions' key in JSON");
    return;
  }

  JsonArray actions = doc["actions"].as<JsonArray>();
  for (JsonObject action : actions) {
    int pin = action["pin"];
    String type = action["type"].as<String>();
    auto value = action["value"];

    if (type == "LED") {
      pinMode(pin, OUTPUT);
      digitalWrite(pin, value.as<int>() ? HIGH : LOW);
      Serial.printf("LED on pin %d set to %d\n", pin, value.as<int>());
    }
    else if (type == "SERVO") {
      bool servoFound = false;
      for (int i = 0; i < NUM_SERVOS; i++) {
        if (SERVO_PINS[i] == pin) {
          servos[i].write(value.as<int>());
          servoFound = true;
          Serial.printf("Servo on pin %d set to %d\n", pin, value.as<int>());
          break;
        }
      }
      if (!servoFound) {
        Serial.println("Servo pin not found");
      }
    }
    else if (type == "BUTTON") {
      pinMode(pin, OUTPUT);
      digitalWrite(pin, value.as<int>() ? HIGH : LOW);
      Serial.printf("Button on pin %d set to %d\n", pin, value.as<int>());
    }
    else if (type == "DISPLAY") {
      String text = value.as<String>();
      displayText = text;
      verticalScrollIndex = 0;
      if (display.width() > 0) { // Only if OLED is present
        display.clearDisplay();
        display.setCursor(0, 0);
        display.println(displayText);
        display.display();
      }
      Serial.printf("Display text: %s\n", text.c_str());
    }
    else if (type == "SCROLL") {
      verticalScrollIndex += lineHeight;
      if (verticalScrollIndex > displayText.length() * 6) {
        verticalScrollIndex = 0;
      }
      if (display.width() > 0) {
        display.clearDisplay();
        display.setCursor(0, -verticalScrollIndex);
        display.println(displayText);
        display.display();
      }
      Serial.println("Scrolled display");
    }
    else {
      Serial.println("Unknown type received");
    }
  }

  server.send(200, "text/plain", "OK");
}
   """.trimIndent()

    val esp8266WifiCode = """
       #include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <Servo.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// --- WiFi Credentials ---
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// --- Web Server ---
ESP8266WebServer server(80);

// --- OLED 128x64 setup (optional) ---
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// --- Pin config (customize as needed) ---
const int LED_PINS[] = {12, 14};              // Add more if needed
const int SERVO_PINS[] = {13, 15};            // Servo1, Servo2
const int NUM_LEDS = sizeof(LED_PINS) / sizeof(LED_PINS[0]);
const int NUM_SERVOS = sizeof(SERVO_PINS) / sizeof(SERVO_PINS[0]);
Servo servos[NUM_SERVOS];                     // Array of Servo objects

// --- OLED Text and Scroll Setup ---
String displayText = "";
int verticalScrollIndex = 0;
const int lineHeight = 8; // 8 pixels per line for text size 1

void setup() {
  Serial.begin(115200);

  // --- Connect to Wi-Fi ---
  WiFi.begin(ssid, password);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConnected to Wi-Fi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // --- Setup LEDs ---
  for (int i = 0; i < NUM_LEDS; i++) {
    pinMode(LED_PINS[i], OUTPUT);
    digitalWrite(LED_PINS[i], LOW);
  }

  // --- Setup Servos ---
  for (int i = 0; i < NUM_SERVOS; i++) {
    servos[i].attach(SERVO_PINS[i]);
    servos[i].write(0);
  }

  // --- OLED setup (optional) ---
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("OLED not found"));
    // Comment out the next line if you don't use OLED
    // while (true);
  } else {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("ESP8266 Ready");
    display.display();
  }

  // --- Setup Web Server ---
  server.on("/control", HTTP_POST, handleControl);
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  server.handleClient();
}

void handleControl() {
  if (server.method() != HTTP_POST) {
    server.send(405, "text/plain", "Method Not Allowed");
    return;
  }

  // Print the raw JSON received for debugging
  Serial.print("Received JSON: ");
  Serial.println(server.arg("plain"));

  StaticJsonDocument<1024> doc;
  DeserializationError error = deserializeJson(doc, server.arg("plain"));
  if (error) {
    server.send(400, "text/plain", "Invalid JSON");
    Serial.print("JSON parse error: ");
    Serial.println(error.c_str());
    return;
  }

  // --- Handle connection test ---
  if (doc.containsKey("action") && doc["action"] == "connect_test") {
    server.send(200, "text/plain", "done");
    Serial.println("Connection test received, responded with 'done'");
    return;
  }

  // --- Handle batch actions ---
  if (!doc.containsKey("actions")) {
    server.send(400, "text/plain", "Missing actions");
    Serial.println("Missing 'actions' key in JSON");
    return;
  }

  JsonArray actions = doc["actions"].as<JsonArray>();
  for (JsonObject action : actions) {
    int pin = action["pin"];
    String type = action["type"].as<String>();
    auto value = action["value"];

    if (type == "LED") {
      pinMode(pin, OUTPUT);
      digitalWrite(pin, value.as<int>() ? HIGH : LOW);
      Serial.printf("LED on pin %d set to %d\n", pin, value.as<int>());
    }
    else if (type == "SERVO") {
      bool servoFound = false;
      for (int i = 0; i < NUM_SERVOS; i++) {
        if (SERVO_PINS[i] == pin) {
          servos[i].write(value.as<int>());
          servoFound = true;
          Serial.printf("Servo on pin %d set to %d\n", pin, value.as<int>());
          break;
        }
      }
      if (!servoFound) {
        Serial.println("Servo pin not found");
      }
    }
    else if (type == "BUTTON") {
      pinMode(pin, OUTPUT);
      digitalWrite(pin, value.as<int>() ? HIGH : LOW);
      Serial.printf("Button on pin %d set to %d\n", pin, value.as<int>());
    }
    else if (type == "DISPLAY") {
      String text = value.as<String>();
      displayText = text;
      verticalScrollIndex = 0;
      if (display.width() > 0) { // Only if OLED is present
        display.clearDisplay();
        display.setCursor(0, 0);
        display.println(displayText);
        display.display();
      }
      Serial.printf("Display text: %s\n", text.c_str());
    }
    else if (type == "SCROLL") {
      verticalScrollIndex += lineHeight;
      if (verticalScrollIndex > displayText.length() * 6) {
        verticalScrollIndex = 0;
      }
      if (display.width() > 0) {
        display.clearDisplay();
        display.setCursor(0, -verticalScrollIndex);
        display.println(displayText);
        display.display();
      }
      Serial.println("Scrolled display");
    }
    else {
      Serial.println("Unknown type received");
    }
  }

  server.send(200, "text/plain", "OK");
}
    """.trimIndent()

    val bluetoothSerialCode = """
#include <BluetoothSerial.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// ✅ Bluetooth
BluetoothSerial SerialBT;

// ✅ OLED 128x64 setup
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// ✅ Pin config
const int LED_PINS[] = {12, 14};              // Add more if needed
const int SERVO_PINS[] = {13, 15};            // Servo1, Servo2

const int NUM_LEDS = sizeof(LED_PINS) / sizeof(LED_PINS[0]);
const int NUM_SERVOS = sizeof(SERVO_PINS) / sizeof(SERVO_PINS[0]);

Servo servos[NUM_SERVOS];                     // Array of Servo objects

// ✅ Input buffer
const int BUFFER_SIZE = 128;
char inputBuffer[BUFFER_SIZE];
int bufferIndex = 0;

// ✅ OLED Text and Scroll Setup
String displayText = "";
int verticalScrollIndex = 0;
const int lineHeight = 8; // 8 pixels per line for text size 1

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_MultiControl");

  // Setup LEDs
  for (int i = 0; i < NUM_LEDS; i++) {
    pinMode(LED_PINS[i], OUTPUT);
    digitalWrite(LED_PINS[i], LOW);
  }

  // Setup Servos
  for (int i = 0; i < NUM_SERVOS; i++) {
    servos[i].setPeriodHertz(50);
    servos[i].attach(SERVO_PINS[i], 500, 2400);
    servos[i].write(0);
  }

  // OLED setup
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("OLED not found"));
    while (true);
  }

  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("ESP32 Ready");
  display.display();

  Serial.println("ESP32 Ready via Bluetooth");
}

void loop() {
  // ✅ Read from Bluetooth
  while (SerialBT.available()) {
    char c = SerialBT.read();
    if (c == '\n' || c == '\r') {
      if (bufferIndex > 0) {
        inputBuffer[bufferIndex] = '\0';
        Serial.print("CMD: ");
        Serial.println(inputBuffer);
        processCommand(inputBuffer);
        bufferIndex = 0;
      }
    } else if (bufferIndex < BUFFER_SIZE - 1) {
      inputBuffer[bufferIndex++] = c;
    }
  }
}

void processCommand(const char* cmd) {
  // ✅ LED: "LED <pin> <value>"
  if (strncmp(cmd, "LED ", 4) == 0) {
    int pin, value;
    if (sscanf(cmd + 4, "%d %d", &pin, &value) == 2) {
      digitalWrite(pin, value ? HIGH : LOW);
      SerialBT.println("OK");
    } else {
      SerialBT.println("ERROR: LED command format");
    }
    return;
  }

  // ✅ SERVO: "SERVO <pin> <angle>"
  if (strncmp(cmd, "SERVO ", 6) == 0) {
    int pin, angle;
    if (sscanf(cmd + 6, "%d %d", &pin, &angle) == 2) {
      bool servoFound = false;
      for (int i = 0; i < NUM_SERVOS; i++) {
        if (SERVO_PINS[i] == pin) {
          servos[i].write(angle);
          SerialBT.println("OK");
          servoFound = true;
          break;
        }
      }
      if (!servoFound) {
        SerialBT.println("ERROR: Servo pin not found");
      }
    } else {
      SerialBT.println("ERROR: SERVO command format");
    }
    return;
  }

  // ✅ BUTTON: "BUTTON <pin> <value>"
  if (strncmp(cmd, "BUTTON ", 7) == 0) {
    int pin, value;
    if (sscanf(cmd + 7, "%d %d", &pin, &value) == 2) {
      digitalWrite(pin, value ? HIGH : LOW);
      SerialBT.println("OK");
    } else {
      SerialBT.println("ERROR: BUTTON command format");
    }
    return;
  }

  // ✅ DISPLAY: "DISPLAY <pin> <text>"
  if (strncmp(cmd, "DISPLAY ", 8) == 0) {
    int pin;
    char text[64];
    if (sscanf(cmd + 8, "%d %[^\n]", &pin, text) == 2) {
      // You can use 'pin' for multiple displays (future)
      displayText = String(text);
      verticalScrollIndex = 0; // Reset scroll position

      display.clearDisplay();
      display.setCursor(0, 0);
      display.println(displayText);
      display.display();

      SerialBT.println("OK");
    } else {
      SerialBT.println("ERROR: DISPLAY command format");
    }
    return;
  }

  // ✅ SCROLL: Scroll text vertically by one line
  if (strncmp(cmd, "SCROLL", 6) == 0) {
    verticalScrollIndex += lineHeight;

    // Reset to top if scrolling past text (optional)
    if (verticalScrollIndex > displayText.length() * 6) {
      verticalScrollIndex = 0;
    }

    display.clearDisplay();
    display.setCursor(0, -verticalScrollIndex);
    display.println(displayText);
    display.display();

    SerialBT.println("OK");
    return;
  }

  SerialBT.println("ERROR: Unknown command");
}

    """.trimIndent()

    val allModesCode = """
// === Universal Example for ESP32 (Bluetooth or WiFi) ===
// This code shows how to handle Switch, Button, Slider, Text (Display), and Sensor modes.
// NOTE: For sensors, the app only receives and displays data sent from your microcontroller. It does not send any commands to the sensor.

#define LED_PIN 12
#define SERVO_PIN 14
#define SENSOR_PIN 15

void setup() {
  pinMode(LED_PIN, OUTPUT); // For LED (Switch/Button/Slider)
  pinMode(SERVO_PIN, OUTPUT); // For Servo (Slider)
  // Setup for display and sensor as needed
}

void loop() {
  // --- Switch Mode ---
  // digitalWrite(LED_PIN, HIGH); // ON
  // digitalWrite(LED_PIN, LOW);  // OFF

  // --- Button Mode ---
  // digitalWrite(LED_PIN, HIGH); // ON
  // delay(100);                  // Short pulse
  // digitalWrite(LED_PIN, LOW);  // OFF

  // --- Slider Mode ---
  // analogWrite(LED_PIN, value); // value from app (0-255 for LED, 0-180 for Servo)

  // --- Text Mode (Display) ---
  // display.print(receivedText); // Print text received from app

  // --- Sensor Mode ---
  // int sensorValue = analogRead(SENSOR_PIN);
  // Send sensorValue back to app
  // The app will only receive and display this value; it does not send commands to the sensor.
}
""".trimIndent()

    val codeText = when {
        selectedBoard == "ESP32" && selectedConnection == "Bluetooth" -> bluetoothSerialCode
        selectedBoard == "ESP32" && selectedConnection == "WiFi" -> esp32WifiCode
        selectedBoard == "ESP8266" && selectedConnection == "WiFi" -> esp8266WifiCode
        else -> allModesCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LED Control Code", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Select your board and communication method to get the right code.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // Board Dropdown
            ExposedDropdownMenuBox(
                expanded = boardExpanded,
                onExpandedChange = { boardExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedBoard,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Board") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boardExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = boardExpanded,
                    onDismissRequest = { boardExpanded = false }
                ) {
                    boardOptions.forEach { board ->
                        DropdownMenuItem(
                            text = { Text(board) },
                            onClick = {
                                selectedBoard = board
                                boardExpanded = false
                                // Reset connection type if switching to ESP8266
                                if (board == "ESP8266") selectedConnection = "WiFi"
                            }
                )
            }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Connection Type Dropdown (only for ESP32)         <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_playstore</item>
            if (selectedBoard == "ESP32") {
                ExposedDropdownMenuBox(
                    expanded = connectionExpanded,
                    onExpandedChange = { connectionExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedConnection,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Connection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = connectionExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = connectionExpanded,
                        onDismissRequest = { connectionExpanded = false }
                    ) {
                        connectionOptions.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                onClick = {
                                    selectedConnection = method
                                    connectionExpanded = false
                        }
                    )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            // Code Card with Copy Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Copy and Share Buttons (top right inside card)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ESP Code", codeText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, codeText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share code via")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        // Code Display
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Text(
                                text = codeText,
                                modifier = Modifier
                                    .verticalScroll(scrollState)
                                    .weight(1f),
                    fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                        }
                    }
                }
            }
        }
    }
} 
