package com.myesp32upgrade.microcontrol.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myesp32upgrade.microcontrol.model.Project
import com.myesp32upgrade.microcontrol.utils.StorageManager
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import com.myesp32upgrade.microcontrol.viewmodel.ProjectViewModel
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import android.content.IntentFilter
import java.io.OutputStream
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.driver.UsbSerialDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    navController: NavController,
    projectViewModel: ProjectViewModel,
    projectName: String
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Not Connected") }
    val coroutineScope = rememberCoroutineScope()
    var bluetoothDeviceName by remember { mutableStateOf("") }
    var espIpAddress by remember { mutableStateOf("192.168.4.1") }
    // State for sensor values
    val sensorValues = remember { mutableStateMapOf<String, String>() }
    // --- Batch changes state ---
    val pendingChanges = remember { mutableStateMapOf<String, Triple<String, String, Any>>() }

    // --- USB Serial State ---
    var usbSerialPort by remember { mutableStateOf<UsbSerialPort?>(null) }
    var usbSerialInput by remember { mutableStateOf("") }
    var usbSerialOutput by remember { mutableStateOf("") }
    var showSerialMonitor by remember { mutableStateOf(false) }
    val serialMonitorActive = remember { AtomicBoolean(false) }
    // Baud rate selection
    val baudRates = listOf(9600, 19200, 38400, 57600, 115200, 230400, 250000, 460800, 921600)
    var selectedBaudRate by remember { mutableStateOf(9600) }
    var baudDropdownExpanded by remember { mutableStateOf(false) }

    // Helper to send serial command (must be inside composable to access usbSerialPort)
    fun sendSerialCommand(pin: String, state: Any) {
        val port = usbSerialPort ?: return
        val command = when (state) {
            is Boolean -> "LED $pin ${if (state) 1 else 0}"
            is Int -> "SERVO $pin $state"
            is String -> "DISPLAY $pin $state"
            else -> null
        }
        if (command != null) {
            try {
                port.write((command + "\n").toByteArray(), 1000)
            } catch (e: Exception) {
                // Optionally update connectionStatus or show a Toast
            }
        }
    }

    // Load project details when entering the screen
    LaunchedEffect(projectName) {
        val project = loadProjectByName(context, projectName)
        project?.let {
            projectViewModel.projectName = it.name
            projectViewModel.projectDescription = it.description
            projectViewModel.selectedBoard = it.boardType
            projectViewModel.connectionType = it.connectionType
            projectViewModel.pinConfigurations = it.pinConfig
        }
    }

    // Bluetooth permission launcher
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initiateBluetoothConnection(context)
        } else {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == -1) { // RESULT_OK
            initiateBluetoothConnection(context)
        } else {
            Toast.makeText(context, "Bluetooth needs to be enabled", Toast.LENGTH_SHORT).show()
        }
    }

    // Start Bluetooth read coroutine when connected
    LaunchedEffect(isConnected) {
        if (isConnected && bluetoothSocket != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream = bluetoothSocket!!.inputStream
                    val reader = inputStream.bufferedReader()
                    while (isConnected) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("SENSOR ")) {
                            val value = line.removePrefix("SENSOR ").trim()
                            // For this example, assume only one sensor pin (find the pin with type Sensor)
                            val sensorPin = projectViewModel.pinConfigurations.entries.firstOrNull { entry ->
                                try {
                                    val obj = org.json.JSONObject(entry.value)
                                    obj.optString("type") == "Sensor"
                                } catch (_: Exception) { false }
                            }?.key
                            if (sensorPin != null) {
                                sensorValues[sensorPin] = value
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Helper to request USB permission
    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("com.myesp32upgrade.microcontrol.USB_PERMISSION"),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    // Add UI for baud rate selection (place this in your Serial Monitor UI section):
    // Serial (USB) connection logic
    suspend fun connectUsbSerial(): Boolean = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) return@withContext false
        val driver = availableDrivers[0]
        val device = driver.device
        // Check permission before opening
        if (!usbManager.hasPermission(device)) {
            withContext(Dispatchers.Main) {
                requestUsbPermission(context, device)
                Toast.makeText(context, "Requesting USB permission...", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
        val connection = usbManager.openDevice(device) ?: return@withContext false
        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(selectedBaudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            serialMonitorActive.set(true)
            // Start reading in background
            coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(64)
                val maxOutputLength = 4096 // Limit output to 4KB
                while (serialMonitorActive.get()) {
                    try {
                        val n = port.read(buffer, 100)
                        if (n > 0) {
                            val received = String(buffer, 0, n)
                            withContext(Dispatchers.Main) {
                                // Limit output length
                                usbSerialOutput = (usbSerialOutput + received).takeLast(maxOutputLength)
                            }
                        }
                        delay(30) // Add a small delay to avoid busy loop
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            connectionStatus = "Serial read error: ${e.localizedMessage}"
                        }
                        break
                    }
                }
            }
            true
        } catch (e: Exception) {
            try { port.close() } catch (_: Exception) {}
            usbSerialPort = null
            false
        }
    }

    fun disconnectUsbSerial() {
        serialMonitorActive.set(false)
        usbSerialPort?.let {
            try { it.close() } catch (_: Exception) {}
        }
        usbSerialPort = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controller: $projectName") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 8.dp, vertical = 4.dp), // Small gap at left and right, minimal top/bottom
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Project Details Card (make compact)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp), // Minimal padding
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Project Name: ${projectViewModel.projectName}", style = MaterialTheme.typography.titleSmall)
                    Text("Description: ${projectViewModel.projectDescription}", style = MaterialTheme.typography.bodySmall)
                    Text("Board: ${projectViewModel.selectedBoard}", style = MaterialTheme.typography.bodySmall)
                    Text("Connection: ${projectViewModel.connectionType}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Connection Status Card (make compact)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp), // Minimal padding
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (projectViewModel.connectionType) {
                                "WiFi" -> Icons.Default.Wifi
                                "Serial" -> Icons.Default.Usb
                                else -> Icons.Default.Bluetooth
                            },
                            contentDescription = "Connection Type"
                        )
                        Text(projectViewModel.connectionType, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: $connectionStatus", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Bluetooth device name input (only for Bluetooth projects)
            if (projectViewModel.connectionType == "Bluetooth") {
                OutlinedTextField(
                    value = bluetoothDeviceName,
                    onValueChange = { bluetoothDeviceName = it },
                    label = { Text("Bluetooth Device Name (exact)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ESP IP input for WiFi
            if (projectViewModel.connectionType == "WiFi") {
                OutlinedTextField(
                    value = espIpAddress,
                    onValueChange = { espIpAddress = it },
                    label = { Text("ESP IP Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Connect Button
            Button(
                onClick = {
                    when (projectViewModel.connectionType) {
                        "WiFi" -> {
                            if (!isConnected && !isConnecting) {
                                isConnecting = true
                                connectionStatus = "Connecting via WiFi..."
                                coroutineScope.launch {
                                    val client = OkHttpClient()
                                    val json = JSONObject().put("action", "connect_test").toString()
                                    val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                    val ip = if (espIpAddress.isBlank()) "testing" else espIpAddress
                                    val url = "http://$ip/control"
                                    val request = Request.Builder()
                                        .url(url)
                                        .post(requestBody)
                                        .build()
                                    try {
                                        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                                        val responseBody = response.body?.string()?.trim()
                                        if (response.isSuccessful && responseBody == "done") {
                            isConnected = true
                                            connectionStatus = "Connected via WiFi"
                                            withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Connected via WiFi", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            isConnected = false
                                            connectionStatus = "Connection failed: HTTP ${response.code}, ${responseBody ?: "No response"}"
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        response.close()
                                    } catch (e: Exception) {
                                        isConnected = false
                                        connectionStatus = "Connection failed: ${e.localizedMessage}"
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    isConnecting = false
                                }
                            } else if (isConnected) {
                                isConnected = false
                                connectionStatus = "Not Connected"
                                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
                            }
                        }
                        "Bluetooth" -> {
                            if (!isConnected && !isConnecting) {
                                if (bluetoothDeviceName.isBlank()) {
                                    Toast.makeText(context, "Please enter the Bluetooth device name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isConnecting = true
                                connectionStatus = "Scanning for device..."
                                coroutineScope.launch {
                                    val result = scanAndConnectBluetoothDevice(context, bluetoothDeviceName)
                                    isConnecting = false
                                    if (result) {
                            isConnected = true
                                        connectionStatus = "Connected via Bluetooth"
                            Toast.makeText(context, "Connected via Bluetooth", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isConnected = false
                                        connectionStatus = "Device not found or connection failed"
                                        Toast.makeText(context, "Device not found or connection failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (isConnected) {
                                isConnected = false
                                connectionStatus = "Not Connected"
                                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
                            }
                        }
                        "Serial" -> {
                            if (!isConnected && !isConnecting) {
                                isConnecting = true
                                connectionStatus = "Connecting via USB Serial..."
                                coroutineScope.launch {
                                    val result = connectUsbSerial()
                                    isConnecting = false
                                    if (result) {
                                        isConnected = true
                                        connectionStatus = "Connected via USB Serial"
                                        Toast.makeText(context, "Connected via USB Serial", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isConnected = false
                                        connectionStatus = "USB Serial device not found or connection failed"
                                        Toast.makeText(context, "USB Serial device not found or connection failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (isConnected) {
                                disconnectUsbSerial()
                                isConnected = false
                                connectionStatus = "Not Connected"
                                showSerialMonitor = false
                                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting
            ) {
                Text(if (isConnected) "Disconnect" else if (isConnecting) "Connecting..." else "Connect")
            }

            // Show baud rate selection above pin controls for Serial connection
            if (projectViewModel.connectionType == "Serial") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    Text("Baud Rate:", modifier = Modifier.padding(end = 8.dp))
                    Box {
                        Button(onClick = { baudDropdownExpanded = true }) {
                            Text(selectedBaudRate.toString())
                        }
                        DropdownMenu(
                            expanded = baudDropdownExpanded,
                            onDismissRequest = { baudDropdownExpanded = false }
                        ) {
                            baudRates.forEach { rate ->
                                DropdownMenuItem(
                                    text = { Text(rate.toString()) },
                                    onClick = {
                                        selectedBaudRate = rate
                                        baudDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { showSerialMonitor = !showSerialMonitor }) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = if (showSerialMonitor) "Hide Serial Monitor" else "Show Serial Monitor"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Serial Monitor")
                    }
                }
                Spacer(modifier = Modifier.height(0.dp))// No gap below baud rate selector
            }

            // Pin Controls Section (make prominent)
            if (isConnected) {
                Spacer(modifier = Modifier.height(0.dp)) // No gap above pin controls
                Text(
                    text = "Pin Controls",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp)) // Minimal gap below title
                // Display pin controls based on configurations
                projectViewModel.pinConfigurations.forEach { (pin, config) ->
                    val parsed = try { org.json.JSONObject(config) } catch (e: Exception) { null }
                    val type = parsed?.optString("type") ?: ""
                    val mode = parsed?.optString("mode") ?: ""
                    val rangeRaw = parsed?.optString("range")?.split("-")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                    val range = if (rangeRaw.isNotEmpty()) rangeRaw else listOf(0, 100)
                    var sliderValue by remember { mutableStateOf(range.first()) }
                    var textValue by remember { mutableStateOf("") }
                    var isButtonPressed by remember { mutableStateOf(false) }

                    PinControlCard(
                        pin = pin,
                        config = config,
                        onPinStateChange = { newState ->
                            when (mode) {
                                "Button", "Switch" -> {
                                    val boolValue = (newState as? Boolean) ?: false
                                    when (projectViewModel.connectionType) {
                                        "WiFi" -> {
                                            val change = Triple(pin, "LED", if (boolValue) 1 else 0)
                                            pendingChanges[pin] = change
                                            sendWiFiBatchCommands(listOf(change), espIpAddress)
                                        }
                                        "Bluetooth" -> sendBluetoothCommand(pin, boolValue, projectViewModel.pinConfigurations)
                                        "Serial" -> sendSerialCommand(pin, boolValue)
                                    }
                                }
                                "Slider" -> {
                                    val intValue = (newState as? Int) ?: 0
                                    when (projectViewModel.connectionType) {
                                        "WiFi" -> {
                                            val change = Triple(pin, if (type == "Servo") "SERVO" else "LED", intValue)
                                            pendingChanges[pin] = change
                                            sendWiFiBatchCommands(listOf(change), espIpAddress)
                                        }
                                        "Bluetooth" -> sendBluetoothCommand(pin, intValue, projectViewModel.pinConfigurations)
                                        "Serial" -> sendSerialCommand(pin, intValue)
                                    }
                                }
                                "Text" -> {
                                    val strValue = (newState as? String) ?: ""
                            when (projectViewModel.connectionType) {
                                        "WiFi" -> {
                                            val change = Triple(pin, "DISPLAY", strValue)
                                            pendingChanges[pin] = change
                                            sendWiFiBatchCommands(listOf(change), espIpAddress)
                                        }
                                        "Bluetooth" -> sendBluetoothCommand(pin, strValue, projectViewModel.pinConfigurations)
                                        "Serial" -> sendSerialCommand(pin, strValue)
                                    }
                                }
                            }
                        },
                        sensorValue = sensorValues[pin]
                    )
                }
                // --- Apply All button for WiFi batch ---
                if (projectViewModel.connectionType == "WiFi" && pendingChanges.isNotEmpty()) {
                    Button(
                        onClick = {
                            sendWiFiBatchCommands(pendingChanges.values.toList(), espIpAddress)
                            pendingChanges.clear()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Apply All")
                    }
                }
            }

            // Serial Monitor UI
            if (showSerialMonitor && isConnected && projectViewModel.connectionType == "Serial") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("Serial Monitor", style = MaterialTheme.typography.titleMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = usbSerialOutput,
                            onValueChange = {},
                            label = { Text("Received Data") },
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            readOnly = true,
                            maxLines = 6
                        )
                        IconButton(
                            onClick = { usbSerialOutput = "" },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Received Data"
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = usbSerialInput,
                            onValueChange = { usbSerialInput = it },
                            label = { Text("Send Data") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val port = usbSerialPort
                                if (port != null && usbSerialInput.isNotBlank()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            port.write((usbSerialInput + "\n").toByteArray(), 1000)
                                            withContext(Dispatchers.Main) { usbSerialInput = "" }
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            enabled = isConnected && usbSerialInput.isNotBlank()
                        ) { Text("Send") }
                    }
                }
            }
        }
    }

    // On dispose, close USB serial if open
    DisposableEffect(Unit) {
        onDispose {
            disconnectUsbSerial()
        }
    }
}

@Composable
fun PinControlCard(
    pin: String,
    config: String,
    onPinStateChange: (Any) -> Unit, // Accepts Boolean for Button, Int for Slider, String for Text
    sensorValue: String? = null
) {
    // Parse config string
    val parsed = remember(config) {
        try {
            JSONObject(config)
        } catch (e: Exception) {
            null
        }
    }
    val type = parsed?.optString("type") ?: ""
    val mode = parsed?.optString("mode") ?: ""
    val rangeRaw = parsed?.optString("range")?.split("-")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    val range = if (rangeRaw.isNotEmpty()) rangeRaw else listOf(0, 100)
    var sliderValue by remember { mutableStateOf(range.first()) }
    var textValue by remember { mutableStateOf("") }
    var isButtonPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Text(
                    text = "Pin $pin",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                text = "$type - $mode",
                    style = MaterialTheme.typography.bodyMedium
                )
            when (mode) {
                "Button" -> {
                    Button(
                        onClick = {
                            isButtonPressed = true
                            onPinStateChange(true)
                            // Simulate momentary press
                            isButtonPressed = false
                            onPinStateChange(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Push")
                    }
                }
                "Slider" -> {
                    Column {
                        Slider(
                            value = sliderValue.toFloat(),
                            onValueChange = {
                                sliderValue = it.toInt()
                                onPinStateChange(sliderValue)
                            },
                            valueRange = range.first().toFloat()..range.last().toFloat(),
                            steps = (range.last() - range.first() - 1).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${range.first()}")
                            Text("Value: $sliderValue")
                            Text("${range.last()}")
                        }
                    }
                }
                "Text" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Text") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onPinStateChange(textValue) }) { Text("Send") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { textValue = "" }) { Text("Clear") }
                    }
                }
                "Switch" -> {
                    var isSwitchOn by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Toggle", modifier = Modifier.weight(1f))
            Switch(
                            checked = isSwitchOn,
                            onCheckedChange = {
                                isSwitchOn = it
                                onPinStateChange(it)
                }
            )
                    }
                }
                else -> {
                    if (type == "Sensor") {
                        Text("Sensor value: ${sensorValue ?: "-"}", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Unknown mode", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun initiateWiFiConnection(context: Context) {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    if (wifiManager == null) {
        Toast.makeText(context, "WiFi not supported on this device", Toast.LENGTH_SHORT).show()
        return
    }
    if (!wifiManager.isWifiEnabled) {
        Toast.makeText(context, "Please enable WiFi from system settings", Toast.LENGTH_LONG).show()
        // Optionally, open WiFi settings for the user
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Fallback if settings cannot be opened
        }
        return
    }
    Toast.makeText(context, "WiFi connection logic not implemented yet", Toast.LENGTH_SHORT).show()
}

private fun initiateBluetoothConnection(context: Context) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter
    if (bluetoothAdapter == null) {
        Toast.makeText(context, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
        return
    }
    if (!bluetoothAdapter.isEnabled) {
        Toast.makeText(context, "Please enable Bluetooth from system settings", Toast.LENGTH_LONG).show()
        // Optionally, open Bluetooth settings for the user
        try {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Fallback if settings cannot be opened
        }
        return
    }
    Toast.makeText(context, "Bluetooth connection logic not implemented yet", Toast.LENGTH_SHORT).show()
}

private fun initiateSerialConnection(context: Context) {
    Toast.makeText(context, "Serial connection logic not implemented yet", Toast.LENGTH_SHORT).show()
}

private val httpClient = OkHttpClient()

private fun sendWiFiBatchCommands(actions: List<Triple<String, String, Any>>, espIp: String?) {
    val jsonArray = org.json.JSONArray()
    for ((pin, type, value) in actions) {
        val obj = org.json.JSONObject()
        obj.put("pin", pin)
        obj.put("type", type)
        obj.put("value", value)
        jsonArray.put(obj)
    }
    val json = org.json.JSONObject()
    json.put("actions", jsonArray)
    val ip = if (espIp.isNullOrBlank()) "192.168.4.1" else espIp
    val url = "http://$ip/control"
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder().url(url).post(body).build()
    httpClient.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
    })
}

// Persistent Bluetooth socket reference
var bluetoothSocket: BluetoothSocket? = null

suspend fun scanAndConnectBluetoothDevice(context: Context, deviceName: String): Boolean {
    return withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter == null) return@withContext false
        if (!bluetoothAdapter.isEnabled) {
            return@withContext false
        }
        val pairedDevice = bluetoothAdapter.bondedDevices?.firstOrNull { it.name == deviceName }
        val device = if (pairedDevice != null) {
            pairedDevice
        } else {
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            val foundDevice = CompletableDeferred<android.bluetooth.BluetoothDevice?>(null)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    if (action == android.bluetooth.BluetoothDevice.ACTION_FOUND) {
                        val device = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE, android.bluetooth.BluetoothDevice::class.java)
                        if (device != null && device.name == deviceName) {
                            foundDevice.complete(device)
                        }
                    }
                }
            }
            val filter = IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)
            bluetoothAdapter.startDiscovery()
            val discoveredDevice = withTimeoutOrNull(15000) { foundDevice.await() }
            bluetoothAdapter.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            discoveredDevice
        }
        if (device == null) return@withContext false
        // Try to connect (SPP)
        return@withContext try {
            val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            socket.connect()
            bluetoothSocket = socket // Keep the socket open for sending commands
            true
        } catch (e: Exception) {
            bluetoothSocket = null
            false
        }
    }
}

private fun sendBluetoothCommand(pin: String, state: Any, pinConfigs: Map<String, String>) {
    val socket = bluetoothSocket ?: return
    val outputStream: OutputStream = try {
        socket.outputStream
    } catch (e: Exception) {
        return
    }
    val config = currentPinConfig(pin, pinConfigs)
    val type = config?.optString("type") ?: ""
    val command = when (type) {
        "LED" -> {
            val value = if (state is Boolean) if (state) 1 else 0 else state.toString().toIntOrNull() ?: 0
            "LED $pin $value"
        }
        "Servo" -> {
            val value = state.toString().toIntOrNull() ?: 0
            "SERVO $pin $value"
        }
        "Button" -> {
            val value = if (state is Boolean) if (state) 1 else 0 else state.toString().toIntOrNull() ?: 0
            "BUTTON $pin $value"
        }
        "Display" -> {
            val value = state.toString()
            "DISPLAY $pin $value"
        }
        else -> null
    }
    if (command != null) {
        try {
            outputStream.write((command + "\n").toByteArray())
            outputStream.flush()
        } catch (_: Exception) {}
    }
}

// Helper to get pin config as JSONObject
private fun currentPinConfig(pin: String, pinConfigs: Map<String, String>): JSONObject? {
    val configStr = pinConfigs[pin] ?: return null
    return try { JSONObject(configStr) } catch (_: Exception) { null }
}

private fun loadProjectByName(context: Context, projectName: String): Project? {
    val projectsFolder = StorageManager.getProjectsFolder(context) ?: return null
    val projectFile = File(File(projectsFolder, projectName), "project.dat")
    if (projectFile.exists()) {
        ObjectInputStream(FileInputStream(projectFile)).use { ois ->
            return ois.readObject() as Project
        }
    }
    return null
} 
