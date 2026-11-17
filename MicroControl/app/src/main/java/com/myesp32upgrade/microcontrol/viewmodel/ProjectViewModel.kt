package com.myesp32upgrade.microcontrol.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.myesp32upgrade.microcontrol.model.Project
import com.myesp32upgrade.microcontrol.utils.StorageManager
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

class ProjectViewModel : ViewModel() {
    var projectName by mutableStateOf("")
    var projectDescription by mutableStateOf("")
    var selectedBoard by mutableStateOf("")
    var connectionType by mutableStateOf("")
    var pinConfigurations by mutableStateOf(mapOf<String, String>())
    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf<String?>(null)

    fun saveProject(context: Context, onSuccess: () -> Unit) {
        if (projectName.isBlank() || selectedBoard.isBlank()) {
            saveError = "Project name and board type are required"
            return
        }

        isSaving = true
        saveError = null

        try {
            // First ensure app folders are created
            if (!StorageManager.createAppFolders(context)) {
                throw Exception("Failed to create app folders. Please check storage permissions.")
            }

            val project = Project(
                name = projectName,
                description = projectDescription,
                boardType = selectedBoard,
                pinConfig = pinConfigurations,
                connectionType = connectionType
            )

            // Create project folder
            val projectFolder = StorageManager.createProjectFolder(context, projectName)
                ?: throw Exception("Failed to create project folder")

            // Save project details
            val projectFile = File(projectFolder, "project.dat")
            ObjectOutputStream(FileOutputStream(projectFile)).use { oos ->
                oos.writeObject(project)
            }

            // Reset form
            projectName = ""
            projectDescription = ""
            selectedBoard = ""
            pinConfigurations = mapOf()
            connectionType = ""
            isSaving = false
            onSuccess()
        } catch (e: Exception) {
            isSaving = false
            saveError = "Failed to save project: ${e.message}"
        }
    }

    fun updatePinConfiguration(pin: String, config: String) {
        val currentConfigs = pinConfigurations.toMutableMap()
        currentConfigs[pin] = config
        pinConfigurations = currentConfigs
    }

    fun clearPinConfiguration(pin: String) {
        val currentConfigs = pinConfigurations.toMutableMap()
        currentConfigs.remove(pin)
        pinConfigurations = currentConfigs
    }
} 