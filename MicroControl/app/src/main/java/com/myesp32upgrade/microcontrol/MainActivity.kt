package com.myesp32upgrade.microcontrol

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myesp32upgrade.microcontrol.navigation.NavRoutes
import com.myesp32upgrade.microcontrol.ui.screens.*
import com.myesp32upgrade.microcontrol.ui.theme.MicroControlTheme
import com.myesp32upgrade.microcontrol.viewmodel.ProjectViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.myesp32upgrade.microcontrol.utils.PermissionHandler
import com.myesp32upgrade.microcontrol.utils.StorageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        setContent {
            var themeType by remember { mutableStateOf("Default") }
            var darkMode by remember { mutableStateOf(false) }
            MicroControlTheme(themeType = themeType, darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var showPermissionRequest by remember { 
                        mutableStateOf(!PermissionHandler.hasRequiredPermissions(this))
                    }
                    var showPathDialog by remember { mutableStateOf(false) }
                    var folderPath by remember { mutableStateOf("") }

                    if (showPathDialog) {
                        AlertDialog(
                            onDismissRequest = { showPathDialog = false },
                            title = { Text("App Folders Created") },
                            text = { 
                                Text(
                                    "Your app folders have been created at:\n\n" +
                                    folderPath + "\n\n" +
                                    "Projects will be saved in the 'Projects' subfolder."
                                )
                            },
                            confirmButton = {
                                Button(onClick = { showPathDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    if (showPermissionRequest) {
                        PermissionHandler.RequestStoragePermission(
                            onPermissionGranted = {
                                // For Android 11 and above, request all files access
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (!android.os.Environment.isExternalStorageManager()) {
                                        StorageManager.requestAllFilesAccess(context)
                                        Toast.makeText(
                                            context,
                                            "Please grant 'All files access' permission in the next screen",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@RequestStoragePermission
                                    }
                                }

                                // Create app folders when permissions are granted
                                if (StorageManager.createAppFolders(context)) {
                                    val appFolder = StorageManager.getAppFolder(context)
                                    folderPath = appFolder?.absolutePath ?: "Unknown path"
                                    showPathDialog = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to create app folders. Please grant all permissions and try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                showPermissionRequest = false
                            },
                            onPermissionDenied = {
                                Toast.makeText(
                                    context,
                                    "Storage permission is required for app functionality",
                                    Toast.LENGTH_LONG
                                ).show()
                                showPermissionRequest = false
                            }
                        )
                    }

                    val navController = rememberNavController()
                    val projectViewModel: ProjectViewModel = viewModel()
                    
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Home.route
                    ) {
                        composable(NavRoutes.Home.route) {
                            MainScreen(navController)
                        }
                        composable(NavRoutes.Recent.route) {
                            RecentScreen(navController)
                        }
                        composable(NavRoutes.Tutorial.route) {
                            TutorialScreen()
                        }
                        composable(NavRoutes.Code.route) {
                            CodeScreen(navController)
                        }
                        composable(NavRoutes.History.route) {
                            SharedScreen(navController)
                        }
                        composable(NavRoutes.CreateProject.route) {
                            CreateProjectScreen(navController, projectViewModel)
                        }
                        composable(NavRoutes.Settings.route) {
                            SettingsScreen(
                                navController,
                                themeType = themeType,
                                onThemeChange = { themeType = it },
                                darkMode = darkMode,
                                onDarkModeChange = { darkMode = it }
                            )
                        }
                        composable(
                            route = NavRoutes.Controller.route,
                            arguments = listOf(
                                navArgument("projectName") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                            ControllerScreen(navController, projectViewModel, projectName)
                        }
                    }
                    
                    // Hide splash screen after content is loaded
                    keepSplashScreen = false
                }
            }
        }
    }
} 