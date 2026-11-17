package com.myesp32upgrade.microcontrol.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment

@Composable
fun TutorialScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "App Tutorial",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Welcome to MicroControl!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("This quick tutorial will help you get started with creating, configuring, and controlling your ESP32/ESP8266 projects.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
        TutorialStep(
            number = 1,
            icon = Icons.Default.AddCircle,
            title = "Create a New Project",
            content = listOf(
                "Tap the '+' button or 'Create Project' on the main screen.",
                "Enter a project name and description.",
                "Select your board (e.g., ESP32, Arduino).",
                "Choose the connection type: WiFi, Bluetooth, or Serial."
            )
        )
        TutorialStep(
            number = 2,
            icon = Icons.Default.Settings,
            title = "Add & Configure Components",
            content = listOf(
                "In 'Pin Configuration', tap 'Add Pin Configuration'.",
                "Enter a component name (e.g., LED1, ServoA).",
                "Select the component type (LED, Button, Servo, Display, Sensor).",
                "Enter the GPIO pin number (e.g., 12).",
                "Choose the mode: Switch, Button, Slider, or Text.",
                "For Slider, the range is set automatically (e.g., 0-255 for LED, 0-180 for Servo)."
            ),
            extra = {
                Text("Example Pin Config JSON:", style = MaterialTheme.typography.bodyMedium)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("""{"type":"LED","mode":"Slider","range":"0-255"}""", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                Text("You do not need to edit this manually; the app handles it for you.", style = MaterialTheme.typography.bodySmall)
            }
        )
        TutorialStep(
            number = 3,
            icon = Icons.Default.Save,
            title = "Save & Use Your Project",
            content = listOf(
                "Tap 'Create Project' at the bottom to save.",
                "Your project appears in the Recent list.",
                "Tap a project to open the Controller screen."
            )
        )
        TutorialStep(
            number = 4,
            icon = Icons.Default.Tune,
            title = "Control Your Components",
            content = listOf(
                "Connect to your board using the selected connection type.",
                "Each configured pin shows the correct control:",
                "• Switch: Toggle ON/OFF in real time.",
                "• Button: Tap to send a momentary ON signal.",
                "• Slider: Drag to send values in real time.",
                "• Text: Enter and send text to displays."
            )
        )
        TutorialStep(
            number = 5,
            icon = Icons.Default.Code,
            title = "How to Change Code for Components/Modes",
            content = listOf(
                "The app generates and stores pin configuration as JSON strings.",
                "To change a pin's behavior, edit its configuration in the project or create a new one.",
                "Example: To make an LED toggle instead of slider, choose 'Switch' mode for that pin.",
                "Example: To control a Servo with a slider, select 'Servo' as type and 'Slider' as mode."
            ),
            extra = {
                Text("Sample Arduino/ESP32 Code Snippet:", style = MaterialTheme.typography.bodyMedium)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("""
// Example for LED on pin 12
#define LED_PIN 12
void setup() {
  pinMode(LED_PIN, OUTPUT);
}
void loop() {
  // For Switch/Button: digitalWrite(LED_PIN, HIGH/LOW)
  // For Slider: analogWrite(LED_PIN, value); // value from 0-255
}
""".trimIndent(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }
        )
        TutorialStep(
            number = 6,
            icon = Icons.Default.TipsAndUpdates,
            title = "Tips & Troubleshooting",
            content = listOf(
                "Make sure your board is powered and connected.",
                "Check that the correct pins and modes are selected.",
                "For Bluetooth, pair your device first if needed.",
                "For WiFi, ensure both devices are on the same network.",
                "You can edit or delete projects from the Recent screen."
            )
        )
        TutorialStep(
            number = 7,
            icon = Icons.Default.Info,
            title = "What to Change in Your Microcontroller Code",
            content = listOf("Below are examples of what to change in your Arduino/ESP32 code for each mode:")
        )
        TutorialCodeExample("Switch Mode (Toggle ON/OFF)", """
// For a Switch (toggle) on pin 12
#define PIN 12
void setup() {
  pinMode(PIN, OUTPUT);
}
void loop() {
  // Receive ON/OFF command from app
  // Example: digitalWrite(PIN, HIGH); // ON
  //          digitalWrite(PIN, LOW);  // OFF
}
""")
        TutorialCodeExample("Button Mode (Momentary)", """
// For a Button (momentary) on pin 13
#define PIN 13
void setup() {
  pinMode(PIN, OUTPUT);
}
void loop() {
  // When app button is pressed:
  // digitalWrite(PIN, HIGH); // ON
  // delay(100);              // Short pulse
  // digitalWrite(PIN, LOW);  // OFF
}
""")
        TutorialCodeExample("Slider Mode (Analog Value)", """
// For a Slider (e.g., LED brightness or Servo angle)
#define PIN 14
void setup() {
  pinMode(PIN, OUTPUT);
}
void loop() {
  // Receive value from app (e.g., 0-255 for LED, 0-180 for Servo)
  // analogWrite(PIN, value); // value sent from app
}
""")
        TutorialCodeExample("Text Mode (Display)", """
// For a Display (e.g., LCD, OLED)
// Receive text from app and print to display
void loop() {
  // Example: display.print(receivedText);
}
""")
        TutorialCodeExample("Sensor Mode (Read Value)", """
// For a Sensor (e.g., temperature, light)
#define SENSOR_PIN 15
void setup() {
  // Setup code for sensor
}
void loop() {
  int sensorValue = analogRead(SENSOR_PIN);
  // Send sensorValue back to app
  // The app will only receive and display this value; it does not send commands to the sensor.
}
""")
        Text("Note: For sensors, the app only receives and displays the data sent from your microcontroller. It does not send any commands to the sensor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        TutorialStep(
            number = 8,
            icon = Icons.Default.UploadFile,
            title = "Import & Export Projects",
            content = listOf(
                "Go to the Sharing screen from the main menu.",
                "Tap 'Import' to load a project file from your device.",
                "Tap 'Export' to save a project file for backup or sharing.",
                "The app uses Android's file picker (Storage Access Framework) for secure file access."
            )
        )
        TutorialStep(
            number = 9,
            icon = Icons.Default.Edit,
            title = "Edit or Delete Projects",
            content = listOf(
                "You can edit pin configurations or delete projects from the Recent screen.",
                "Editing a project lets you change component types, modes, or pin numbers as needed."
            )
        )
        TutorialStep(
            number = 10,
            icon = Icons.Default.Bluetooth,
            title = "Connection Types",
            content = listOf(
                "When creating a project, select the connection type: Bluetooth, WiFi, or Serial.",
                "For Bluetooth, pair your device if needed before connecting.",
                "For WiFi, ensure both your phone and board are on the same network.",
                "For Serial, connect your device via USB OTG if supported."
            )
        )
        TutorialStep(
            number = 11,
            icon = Icons.Default.Code,
            title = "Component Code Functions",
            content = listOf(
                "The app auto-generates the correct UI and code mapping for each component and mode.",
                "Switch: digitalWrite(PIN, HIGH/LOW)",
                "Button: digitalWrite(PIN, HIGH); delay(100); digitalWrite(PIN, LOW);",
                "Slider: analogWrite(PIN, value); (0-255 for LED, 0-180 for Servo)",
                "Text: display.print(receivedText); (for displays)",
                "Sensor: int sensorValue = analogRead(PIN); (read and send to app)"
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Need more help?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Visit the official documentation or contact support for further assistance.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun TutorialStep(number: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: List<String>, extra: @Composable (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Step $number: $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            content.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
            }
            extra?.invoke()
        }
    }
}

@Composable
fun TutorialCodeExample(label: String, code: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(code.trim(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
} 