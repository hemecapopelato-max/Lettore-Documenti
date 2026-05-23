package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReadPosition: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f
) {
    // Helper to get formatted reading time (assumes ~200 words per minute speaking rate)
    val wordCount: Int
        get() = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

    val speechDurationMinutes: Int
        get() = (wordCount / 150).coerceAtLeast(1)
}
