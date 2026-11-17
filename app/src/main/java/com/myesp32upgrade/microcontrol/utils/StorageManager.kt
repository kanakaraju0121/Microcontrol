package com.myesp32upgrade.microcontrol.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageManager {
    private const val APP_FOLDER_NAME = "MicroControl"
    private const val PROJECTS_FOLDER = "Projects"
    private const val TEMP_FOLDER = "Temp"
    private const val HISTORY_FILE = "share_history.json"

    data class ShareHistoryEntry(
        val type: String, // "import" or "export"
        val projectName: String,
        val fileName: String,
        val timestamp: Long
    )

    fun createAppFolders(context: Context): Boolean {
        try {
            // For Android 11+ (API 30+) and above, do not check for all files access, just use app-specific storage
            val appFolder = getAppFolder(context) ?: return false

            // Create main app folder
            if (!appFolder.exists()) {
                if (!appFolder.mkdirs()) {
                    return false
                }
            }

            // Create Projects folder
            val projectsFolder = File(appFolder, PROJECTS_FOLDER)
            if (!projectsFolder.exists()) {
                if (!projectsFolder.mkdirs()) {
                    return false
                }
            }

            // Create Temp folder
            val tempFolder = File(appFolder, TEMP_FOLDER)
            if (!tempFolder.exists()) {
                if (!tempFolder.mkdirs()) {
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getAppFolder(context: Context): File? {
        // Always use app-specific storage for Android 11+ (API 30+) and above
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            File(context.getExternalFilesDir(null), APP_FOLDER_NAME)
        } else {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(Environment.getExternalStorageDirectory(), APP_FOLDER_NAME)
            } else {
                File(context.getExternalFilesDir(null), APP_FOLDER_NAME)
            }
        }
    }

    fun getProjectsFolder(context: Context): File? {
        val appFolder = getAppFolder(context) ?: return null
        return File(appFolder, PROJECTS_FOLDER)
    }

    fun getTempFolder(context: Context): File? {
        val appFolder = getAppFolder(context) ?: return null
        return File(appFolder, TEMP_FOLDER)
    }

    fun createProjectFolder(context: Context, projectName: String): File? {
        val projectsFolder = getProjectsFolder(context) ?: return null
        val projectFolder = File(projectsFolder, projectName)
        
        if (!projectFolder.exists()) {
            if (!projectFolder.mkdirs()) {
                return null
            }
        }
        
        return projectFolder
    }

    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun getSavedProjectsFolder(context: Context): File? {
        val appFolder = getAppFolder(context) ?: return null
        val savedProjectsFolder = File(appFolder, "SavedProjects")
        if (!savedProjectsFolder.exists()) {
            savedProjectsFolder.mkdirs()
        }
        return savedProjectsFolder
    }

    fun saveProjectToSavedFolder(context: Context, projectName: String): Boolean {
        val projectsFolder = getProjectsFolder(context) ?: return false
        val savedProjectsFolder = getSavedProjectsFolder(context) ?: return false
        val sourceFolder = File(projectsFolder, projectName)
        val destFolder = File(savedProjectsFolder, projectName)
        if (!sourceFolder.exists()) return false
        if (destFolder.exists()) destFolder.deleteRecursively()
        return sourceFolder.copyRecursively(destFolder, overwrite = true)
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun createPermanentProjectIfNotExists(context: Context) {
        if (!hasAllFilesAccess()) return
        val projectsFolder = getProjectsFolder(context) ?: return
        val projectName = "All In One"
        val projectFolder = File(projectsFolder, projectName)
        if (projectFolder.exists()) {
            projectFolder.deleteRecursively()
        }
        projectFolder.mkdirs()
        val projectFile = File(projectFolder, "project.dat")
        val codeFile = File(projectFolder, "main.cpp")

        // Standard ESP32 BLE LED control sketch
        val code = """
#include <BluetoothSerial.h>\nBluetoothSerial SerialBT;\n#define LED1 12\n#define LED2 13\n#define LED3 14\n#define LED4 26\n#define LED5 27\nvoid setup() {\n  Serial.begin(115200);\n  SerialBT.begin(\"ESP32_LED_Control\");\n  pinMode(LED1, OUTPUT);\n  pinMode(LED2, OUTPUT);\n  pinMode(LED3, OUTPUT);\n  pinMode(LED4, OUTPUT);\n  pinMode(LED5, OUTPUT);\n}\nvoid loop() {\n  if (SerialBT.available()) {\n    String input = SerialBT.readStringUntil('\n');\n    int pin = input.substring(0, 2).toInt();\n    String cmd = input.substring(2);\n    if (cmd.startsWith(\"B\")) {\n      digitalWrite(pin, cmd.endsWith(\"1\") ? HIGH : LOW);\n    } else if (cmd.startsWith(\"S\")) {\n      int value = cmd.substring(1).toInt();\n      analogWrite(pin, value);\n    }\n  }\n}\n"""
        try {
        codeFile.writeText(code)
        } catch (e: Exception) {
            // Fallback: write to app's internal storage if external fails
            val internalCodeFile = File(context.filesDir, "main.cpp")
            internalCodeFile.writeText(code)
        }
        val project = com.myesp32upgrade.microcontrol.model.Project(
            name = projectName,
            description = "Bluetooth Serial LED Control Example for ESP32",
            boardType = "ESP32",
            pinConfig = mapOf(
                "12" to "{\"type\":\"LED\",\"mode\":\"Switch\"}",
                "13" to "{\"type\":\"Servo\",\"mode\":\"Slider\",\"range\":\"0-180\"}",
                "14" to "{\"type\":\"Display\",\"mode\":\"Text\"}",
                "15" to "{\"type\":\"Button\",\"mode\":\"Button\"}",
                "16" to "{\"type\":\"Sensor\",\"mode\":\"Sensor\"}"
            ),
            connectionType = "Bluetooth"
        )
        ObjectOutputStream(FileOutputStream(projectFile)).use { oos ->
            oos.writeObject(project)
        }
    }

    fun logShareHistory(context: Context, entry: ShareHistoryEntry) {
        val appFolder = getAppFolder(context) ?: return
        val file = File(appFolder, HISTORY_FILE)
        val history = if (file.exists()) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }
        val obj = JSONObject().apply {
            put("type", entry.type)
            put("projectName", entry.projectName)
            put("fileName", entry.fileName)
            put("timestamp", entry.timestamp)
        }
        history.put(obj)
        file.writeText(history.toString())
    }

    fun getShareHistory(context: Context): List<ShareHistoryEntry> {
        val appFolder = getAppFolder(context) ?: return emptyList()
        val file = File(appFolder, HISTORY_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                ShareHistoryEntry(
                    type = obj.getString("type"),
                    projectName = obj.getString("projectName"),
                    fileName = obj.getString("fileName"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 