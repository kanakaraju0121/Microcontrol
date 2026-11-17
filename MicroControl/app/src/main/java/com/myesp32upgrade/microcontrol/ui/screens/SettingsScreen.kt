package com.myesp32upgrade.microcontrol.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myesp32upgrade.microcontrol.R
import com.myesp32upgrade.microcontrol.utils.StorageManager
import java.io.File
import androidx.compose.foundation.clickable
import coil.compose.rememberAsyncImagePainter
import com.myesp32upgrade.microcontrol.ui.theme.*
import androidx.compose.foundation.horizontalScroll
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeType: String = "Default",
    onThemeChange: (String) -> Unit = {},
    darkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE) }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showSavedProjectsDialog by remember { mutableStateOf(false) }
    var savedProjects by remember { mutableStateOf<List<String>>(emptyList()) }

    // Account details state (persisted)
    var showAccountDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var email by remember { mutableStateOf(prefs.getString("email", "") ?: "") }
    var profilePicUri by remember { mutableStateOf(prefs.getString("profilePicUri", null)?.let { Uri.parse(it) }) }
    var showWarning by remember { mutableStateOf(false) }

    // For button label
    val hasAccount = name.isNotBlank() && email.isNotBlank() && profilePicUri != null

    val pickImageLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            profilePicUri = uri
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section with Profile Picture
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showAccountDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(profilePicUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(100.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Account Button
                    Button(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasAccount) "Edit Account" else "Add Account")
                    }
                }
            }

            // Saved Projects Button
            Button(
                onClick = {
                    val folder = StorageManager.getSavedProjectsFolder(context)
                    savedProjects = folder?.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
                    showSavedProjectsDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Create,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saved Projects")
            }

            // Theme Bubbles
            val themeOptions = listOf(
                Triple("Default", Purple40, "Default"),
                Triple("Blue", Blue40, "Blue"),
                Triple("Green", Green40, "Green"),
                Triple("Red", Red40, "Red"),
                Triple("Orange", Orange40, "Orange"),
                Triple("Yellow", Yellow40, "Yellow"),
                Triple("Teal", Teal40, "Teal"),
                Triple("Pink", PinkBright40, "Pink")
            )
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                themeOptions.forEach { (theme, color, label) ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (themeType == theme) 4.dp else 2.dp,
                                color = if (themeType == theme) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { onThemeChange(theme) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (themeType == theme) {
                Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "$label theme selected",
                                tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                        }
                    }
                }
            }

            // Dark Mode Toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Mode")
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }
            }

            // Info Button
            Button(
                onClick = { showInfoDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("About")
            }
        }
    }

    // Account Details Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account Details") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Picture Picker
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { pickImageLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(profilePicUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showWarning && (name.isBlank() || email.isBlank() || profilePicUri == null)) {
                        Text("Please fill in all fields and add a profile picture.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank() || email.isBlank() || profilePicUri == null) {
                        showWarning = true
                    } else {
                        showWarning = false
                        // Save to SharedPreferences
                        prefs.edit()
                            .putString("name", name)
                            .putString("email", email)
                            .putString("profilePicUri", profilePicUri.toString())
                            .apply()
                        showAccountDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSavedProjectsDialog) {
        AlertDialog(
            onDismissRequest = { showSavedProjectsDialog = false },
            title = { Text("Saved Projects") },
            text = {
                if (savedProjects.isEmpty()) {
                    Text("No saved projects found.")
                } else {
                    Column {
                        savedProjects.forEach { projectName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = {
                                    showSavedProjectsDialog = false
                                    navController.navigate(
                                        com.myesp32upgrade.microcontrol.navigation.NavRoutes.Controller.createRoute(projectName)
                                    )
                                }) {
                                    Text(projectName)
                                }
                                IconButton(onClick = {
                                    // Remove project from SavedProjects
                                    val folder = StorageManager.getSavedProjectsFolder(context)
                                    val projectFolder = java.io.File(folder, projectName)
                                    if (projectFolder.exists()) {
                                        projectFolder.deleteRecursively()
                                        // Update the list
                                        val updatedFolder = StorageManager.getSavedProjectsFolder(context)
                                        savedProjects = updatedFolder?.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove from saved")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedProjectsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About MicroControl") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Creator:", style = MaterialTheme.typography.titleMedium)
                    Text("Kanakaraju N P", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Email:", style = MaterialTheme.typography.titleMedium)
                    Text("kanakaraju3262@gmail.com", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Customer Care:", style = MaterialTheme.typography.titleMedium)
                    Text("xxxxxxx", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Details:", style = MaterialTheme.typography.titleMedium)
                    Text("MicroControl is an all-in-one project management and controller app for ESP32/ESP8266. Easily create, configure, and control your IoT projects with modern UI, theme switching, import/export, and more. Designed for makers, students, and professionals.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
} 