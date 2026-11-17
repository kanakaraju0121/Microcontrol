package com.myesp32upgrade.microcontrol.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myesp32upgrade.microcontrol.utils.StorageManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.automirrored.outlined.Send

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(navController: NavController) {
    val context = LocalContext.current
    var importResult by remember { mutableStateOf<String?>(null) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<File?>(null) }
    var shareHistory by remember { mutableStateOf(listOf<com.myesp32upgrade.microcontrol.utils.StorageManager.ShareHistoryEntry>()) }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val success = importProjectFromUri(context, uri)
            val name = uri.lastPathSegment?.substringAfterLast(":")?.substringBeforeLast(".") ?: "ImportedProject"
            StorageManager.logShareHistory(
                context,
                StorageManager.ShareHistoryEntry(
                    type = "import",
                    projectName = name,
                    fileName = uri.lastPathSegment ?: "",
                    timestamp = System.currentTimeMillis()
                )
            )
            importResult = if (success) "Project imported successfully" else "Import failed"
            shareHistory = StorageManager.getShareHistory(context)
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null && selectedProject != null) {
            val success = exportProjectToUri(context, selectedProject!!, uri)
            StorageManager.logShareHistory(
                context,
                StorageManager.ShareHistoryEntry(
                    type = "export",
                    projectName = selectedProject!!.name,
                    fileName = uri.lastPathSegment ?: "",
                    timestamp = System.currentTimeMillis()
                )
            )
            exportResult = if (success) "Project exported successfully" else "Export failed"
            shareHistory = StorageManager.getShareHistory(context)
        }
    }

    // Show toast for import/export result
    LaunchedEffect(importResult) {
        importResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            importResult = null
        }
    }
    LaunchedEffect(exportResult) {
        exportResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            exportResult = null
        }
    }

    // Load projects when export dialog is shown
    LaunchedEffect(showExportDialog) {
        if (showExportDialog) {
            val folder = StorageManager.getProjectsFolder(context)
            projects = folder?.listFiles()?.filter { it.isDirectory } ?: emptyList()
        }
    }
    // Load history on first composition
    LaunchedEffect(Unit) {
        shareHistory = StorageManager.getShareHistory(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Share & Import Projects",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Input, contentDescription = "Import", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Export", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Sharing History", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (shareHistory.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No sharing history yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shareHistory.reversed().forEachIndexed { idx, entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.type == "import") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (entry.type == "import") Icons.AutoMirrored.Outlined.Input else Icons.AutoMirrored.Outlined.Send,
                                contentDescription = entry.type,
                                modifier = Modifier.size(22.dp),
                                tint = if (entry.type == "import") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.type.replaceFirstChar { it.uppercase() }} - ${entry.projectName}", style = MaterialTheme.typography.titleMedium)
                                Text(entry.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                StorageManager.formatTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Bottom)
                            )
                        }
                    }
                    if (idx < shareHistory.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }
    // Export project selection dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Select Project to Export", style = MaterialTheme.typography.titleLarge) },
            text = {
                if (projects.isEmpty()) {
                    Text("No projects found.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        projects.forEach { projectFolder ->
                            val projectName = projectFolder.name
                            OutlinedButton(
                                onClick = {
                                    selectedProject = projectFolder
                                    showExportDialog = false
                                    // Launch export picker with project name
                                    exportLauncher.launch("$projectName.dat")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(projectName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.large
        )
    }
}

private fun importProjectFromUri(context: Context, uri: Uri): Boolean {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return false
        val projectsFolder = StorageManager.getProjectsFolder(context) ?: return false
        // Try to get project name from file name (fallback to "ImportedProject")
        var baseName = uri.lastPathSegment?.substringAfterLast(":")?.substringBeforeLast(".") ?: "ImportedProject"
        var name = baseName
        var counter = 1
        while (File(projectsFolder, name).exists()) {
            name = "${baseName}_$counter"
            counter++
        }
        val projectFolder = File(projectsFolder, name)
        if (!projectFolder.exists()) projectFolder.mkdirs()
        val outFile = File(projectFolder, "project.dat")
        FileOutputStream(outFile).use { output ->
            inputStream.copyTo(output)
        }
        // Always update the project name in project.dat to match the folder name
        var valid = false
        try {
            val fis = FileInputStream(outFile)
            val ois = java.io.ObjectInputStream(fis)
            val project = ois.readObject() as com.myesp32upgrade.microcontrol.model.Project
            ois.close()
            fis.close()
            val updatedProject = project.copy(name = name)
            val fos = FileOutputStream(outFile)
            val oos = java.io.ObjectOutputStream(fos)
            oos.writeObject(updatedProject)
            oos.close()
            fos.close()
            valid = true
        } catch (_: Exception) {
            // If deserialization fails, create a default/unknown project
            val unknownProject = com.myesp32upgrade.microcontrol.model.Project(
                name = name,
                description = "Imported project (unknown format)",
                boardType = "ESP32",
                pinConfig = emptyMap(),
                connectionType = "Bluetooth"
            )
            val fos = FileOutputStream(outFile)
            val oos = java.io.ObjectOutputStream(fos)
            oos.writeObject(unknownProject)
            oos.close()
            fos.close()
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun exportProjectToUri(context: Context, projectFolder: File, uri: Uri): Boolean {
    return try {
        val projectFile = File(projectFolder, "project.dat")
        if (!projectFile.exists()) return false
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
        FileInputStream(projectFile).use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
} 