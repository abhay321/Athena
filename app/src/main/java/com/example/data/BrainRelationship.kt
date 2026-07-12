package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brain_relationships")
data class BrainRelationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceDocId: Long,
    val targetDocId: Long,
    val relationshipType: String, // e.g., "Related", "Prerequisite", "Summary", "Contradicts"
    val strength: Float = 1.0f
)
