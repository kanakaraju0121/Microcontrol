package com.myesp32upgrade.microcontrol

import com.myesp32upgrade.microcontrol.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ProjectSerializationTest {
    @Test
    fun projectRoundTripsThroughJavaSerialization() {
        val original = Project(
            name = "LED Controller",
            description = "Test project for model persistence",
            boardType = "ESP32",
            pinConfig = mapOf("GPIO2" to "LED"),
            connectionType = "WiFi",
            createdAt = 1_700_000_000_000L
        )

        val bytes = ByteArrayOutputStream().use { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(original) }
            buffer.toByteArray()
        }

        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as Project
        }

        assertEquals(original, restored)
        assertTrue(bytes.isNotEmpty())
    }
}
