package com.myesp32upgrade.microcontrol.model

import java.io.Serializable

data class Project(
    val name: String,
    val description: String,
    val boardType: String,
    val pinConfig: Map<String, String>,
    val connectionType: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable 