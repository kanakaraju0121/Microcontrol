package com.myesp32upgrade.microcontrol.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Recent : NavRoutes("recent")
    object Tutorial : NavRoutes("tutorial")
    object Code : NavRoutes("code")
    object History : NavRoutes("history")
    object CreateProject : NavRoutes("create_project")
    object Settings : NavRoutes("settings")
    object Controller : NavRoutes("controller/{projectName}") {
        fun createRoute(projectName: String) = "controller/$projectName"
    }
} 