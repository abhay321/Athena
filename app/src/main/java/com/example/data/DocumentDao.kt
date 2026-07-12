package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM captured_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<CapturedDocument>>

    @Query("SELECT * FROM captured_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): CapturedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: CapturedDocument): Long

    @Update
    suspend fun updateDocument(document: CapturedDocument)

    @Query("DELETE FROM captured_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    // Relationships
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: BrainRelationship): Long

    @Query("SELECT * FROM brain_relationships")
    fun getAllRelationships(): Flow<List<BrainRelationship>>

    @Query("DELETE FROM brain_relationships WHERE sourceDocId = :docId OR targetDocId = :docId")
    suspend fun deleteRelationshipsForDoc(docId: Long)

    // Chat History
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}
