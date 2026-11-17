package com.myesp32upgrade.microcontrol.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHandler {
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Composable
    fun PermissionRequestDialog(
        onDismiss: () -> Unit,
        onRequestPermission: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Storage Permission Required") },
            text = { 
                Text(
                    "MicroControl needs access to storage to save your projects and related files. " +
                    "This permission is essential for the app to function properly."
                )
            },
            confirmButton = {
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Not Now")
                }
            }
        )
    }

    @Composable
    fun RequestStoragePermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        val context = LocalContext.current
        var showDialog by remember { mutableStateOf(true) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

        if (showDialog) {
            PermissionRequestDialog(
                onDismiss = {
                    showDialog = false
                    onPermissionDenied()
                },
                onRequestPermission = {
                    showDialog = false
                    permissionLauncher.launch(requiredPermissions)
                }
            )
        }
    }
} 