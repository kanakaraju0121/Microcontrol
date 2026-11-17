package com.myesp32upgrade.microcontrol.models

data class ProjectModel(
    val name: String,
    val boardType: String,
    val connectionType: String,
    val description: String,
    val components: List<ComponentModel>
)

data class ComponentModel(
    val name: String,
    val type: String,
    val gpioPin: Int,
    val mode: String,
    val initialState: String
) 