package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_documents")
data class CapturedDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val rawText: String,
    val markdown: String,
    val structuredJson: String,
    val imagePath: String, // Name of template or mock uri
    val createdAt: Long = System.currentTimeMillis(),
    val category: String, // "Whiteboard", "Book Page", "Meeting Note", "Personal"
    val summary: String,
    val tags: String, // Comma-separated tags
    val actionItems: String = "", // JSON-string of checklist items
    val flashcards: String = "", // JSON-string of flashcards
    val isFavorite: Boolean = false
)
