package com.myesp32upgrade.microcontrol.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.myesp32upgrade.microcontrol.R
import com.myesp32upgrade.microcontrol.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var selectedItem by remember { mutableStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Log.d("MainScreen", "Current route: $currentRoute")
    
    val items = listOf(
        NavigationItem("Recent", Icons.AutoMirrored.Filled.List, NavRoutes.Recent.route),
        NavigationItem("Tutorial", Icons.Default.Info, NavRoutes.Tutorial.route),
        NavigationItem("Code", Icons.Default.Create, NavRoutes.Code.route),
        NavigationItem("Shared", Icons.Default.Share, NavRoutes.History.route)
    )
    
    Log.d("MainScreen", "Selected navigation item: $selectedItem")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_playstore),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text("MicroControl")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        navController.navigate(NavRoutes.Settings.route) {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    navController.navigate(NavRoutes.CreateProject.route) {
                        launchSingleTop = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedItem) {
                0 -> RecentScreen(navController)
                1 -> TutorialScreen()
                2 -> CodeScreen(navController)
                3 -> SharedScreen(navController)
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
) 