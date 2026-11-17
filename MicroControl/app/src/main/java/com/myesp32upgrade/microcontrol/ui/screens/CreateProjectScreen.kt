package com.myesp32upgrade.microcontrol.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myesp32upgrade.microcontrol.models.ComponentModel
import com.myesp32upgrade.microcontrol.models.ProjectModel
import com.myesp32upgrade.microcontrol.navigation.NavRoutes
import com.myesp32upgrade.microcontrol.viewmodel.ProjectViewModel

enum class ConnectionState { Disconnected, Connecting, Connected, Error }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    navController: NavController,
    projectViewModel: ProjectViewModel
) {
    val context = LocalContext.current
    // Reset all fields when this screen is first composed
    LaunchedEffect(Unit) {
        projectViewModel.projectName = ""
        projectViewModel.projectDescription = ""
        projectViewModel.selectedBoard = ""
        projectViewModel.connectionType = ""
        projectViewModel.pinConfigurations = mapOf()
    }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showPinConfigDialog by remember { mutableStateOf(false) }
    var selectedPin by remember { mutableStateOf("") }
    var selectedConfig by remember { mutableStateOf("") }
    var componentName by remember { mutableStateOf("") }
    var componentType by remember { mutableStateOf("") }
    var connectionType by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ConnectionState.Disconnected) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Project") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    if (projectViewModel.projectName.isNotBlank() && 
                        projectViewModel.selectedBoard.isNotBlank() && 
                        connectionType.isNotBlank()) {
                        showSaveDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "Please fill in all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !projectViewModel.isSaving
            ) {
                if (projectViewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Project")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Name
                OutlinedTextField(
                    value = projectViewModel.projectName,
                    onValueChange = { projectViewModel.projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Project Description
                OutlinedTextField(
                    value = projectViewModel.projectDescription,
                    onValueChange = { projectViewModel.projectDescription = it },
                    label = { Text("Project Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Board Selection
                var boardExpanded by remember { mutableStateOf(false) }
                val boards = listOf(
                    "ESP32",
                    "ESP8266",
                    "Arduino",
                    "STM32",
                    "Raspberry Pi"
                )

                ExposedDropdownMenuBox(
                    expanded = boardExpanded,
                    onExpandedChange = { boardExpanded = it }
                ) {
                    OutlinedTextField(
                        value = projectViewModel.selectedBoard,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Board") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = boardExpanded,
                        onDismissRequest = { boardExpanded = false }
                    ) {
                        boards.forEach { board ->
                            DropdownMenuItem(
                                text = { Text(board) },
                                onClick = {
                                    projectViewModel.selectedBoard = board
                                    boardExpanded = false
                                }
                            )
                        }
                    }
                }

                // Connection Type Selection
                var connectionExpanded by remember { mutableStateOf(false) }
                val connectionTypes = listOf("WiFi", "Bluetooth", "Serial")

                ExposedDropdownMenuBox(
                    expanded = connectionExpanded,
                    onExpandedChange = { connectionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = connectionType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Connection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = connectionExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = connectionExpanded,
                        onDismissRequest = { connectionExpanded = false }
                    ) {
                        connectionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    connectionType = type
                                    projectViewModel.connectionType = type
                                    connectionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Pin Configuration Section
                Text(
                    text = "Pin Configuration",
                    style = MaterialTheme.typography.titleMedium
                )

                // Add Pin Configuration Button
                Button(
                    onClick = { showPinConfigDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Pin Configuration")
                }

                // Display configured pins
                projectViewModel.pinConfigurations.forEach { (pin, config) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pin $pin: $config")
                            IconButton(onClick = { projectViewModel.clearPinConfiguration(pin) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }

        // Pin Configuration Dialog
        if (showPinConfigDialog) {
            var componentTypeExpanded by remember { mutableStateOf(false) }
            var pinExpanded by remember { mutableStateOf(false) }
            var modeExpanded by remember { mutableStateOf(false) }
            
            val componentTypes = listOf("LED", "Button", "Servo", "Display", "Sensor")
            // Dynamic modes based on componentType
            val customModes = when (componentType) {
                "LED" -> listOf("Switch", "Button", "Slider")
                "Servo" -> listOf("Slider")
                "Display" -> listOf("Text")
                "Sensor" -> emptyList()
                else -> listOf("Switch", "Button")
            }
            val sliderRange = when (componentType) {
                "LED" -> 0 to 255
                "Servo" -> 0 to 180
                else -> 0 to 100
            }
            AlertDialog(
                onDismissRequest = { showPinConfigDialog = false },
                title = { Text("Configure Pin") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Component Name Input
                        OutlinedTextField(
                            value = componentName,
                            onValueChange = { componentName = it },
                            label = { Text("Component Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Component Type Selection
                        ExposedDropdownMenuBox(
                            expanded = componentTypeExpanded,
                            onExpandedChange = { componentTypeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = componentType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Component Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = componentTypeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = componentTypeExpanded,
                                onDismissRequest = { componentTypeExpanded = false }
                            ) {
                                componentTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            componentType = type
                                            componentTypeExpanded = false
                                                selectedConfig = ""
                                        }
                                    )
                                }
                            }
                        }
                        // Pin Number Input
                        OutlinedTextField(
                            value = selectedPin,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    selectedPin = it
                                }
                            },
                            label = { Text("Pin Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Mode Selection (dynamic)
                        if (componentType == "Sensor") {
                            Text("Sensor: The app will only receive and display data sent from your microcontroller. No mode selection is needed.", style = MaterialTheme.typography.bodySmall)
                        } else if (customModes.size == 1) {
                            OutlinedTextField(
                                value = customModes[0],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mode") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            selectedConfig = customModes[0]
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = modeExpanded,
                                onExpandedChange = { modeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedConfig,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mode") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = modeExpanded,
                                    onDismissRequest = { modeExpanded = false }
                                ) {
                                    customModes.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode) },
                                            onClick = {
                                                selectedConfig = mode
                                                modeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Slider range preview if needed
                        if (selectedConfig == "Slider") {
                            Text("Slider range: ${sliderRange.first} to ${sliderRange.second}", style = MaterialTheme.typography.bodySmall)
                        }
                        // Text mode preview if needed
                        if (selectedConfig == "Text") {
                            Text("Text input will be available in controller screen with Send and Clear buttons.", style = MaterialTheme.typography.bodySmall)
                        }
                        // Sensor mode preview if needed
                        if (componentType == "Sensor") {
                            Text("Sensor mode: The app will only receive and display data sent from your microcontroller. It does not send commands to the sensor.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val valid = if (componentType == "Sensor") {
                                componentName.isNotBlank() && componentType.isNotBlank() && selectedPin.isNotBlank()
                            } else {
                                componentName.isNotBlank() && componentType.isNotBlank() && selectedPin.isNotBlank() && selectedConfig.isNotBlank()
                            }
                            if (valid) {
                                val config = buildString {
                                    append("{\"type\":\"").append(componentType).append("\"")
                                    if (componentType != "Sensor") {
                                        append(",\"mode\":\"").append(selectedConfig).append("\"")
                                        if (selectedConfig == "Slider") {
                                            append(",\"range\":\"")
                                            append(sliderRange.first).append("-").append(sliderRange.second)
                                            append("\"")
                                        }
                                    }
                                    append("}")
                                }
                                projectViewModel.updatePinConfiguration(selectedPin, config)
                                componentName = ""
                                componentType = ""
                                selectedPin = ""
                                selectedConfig = ""
                                showPinConfigDialog = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please fill in all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinConfigDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Save Confirmation Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Create Project") },
                text = { Text("Are you sure you want to create this project?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSaveDialog = false
                            projectViewModel.saveProject(context) {
                                Toast.makeText(context, "Project created successfully", Toast.LENGTH_SHORT).show()
                                navController.navigate(NavRoutes.Controller.route) {
                                    popUpTo(NavRoutes.Home.route)
                                    launchSingleTop = true
                                }
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error Dialog
        projectViewModel.saveError?.let { error ->
            AlertDialog(
                onDismissRequest = { projectViewModel.saveError = null },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = { projectViewModel.saveError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
} 