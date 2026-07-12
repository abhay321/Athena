package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val dao: DocumentDao) {
    val allDocuments: Flow<List<CapturedDocument>> = dao.getAllDocuments()
    val allRelationships: Flow<List<BrainRelationship>> = dao.getAllRelationships()
    val allChatMessages: Flow<List<ChatMessage>> = dao.getAllChatMessages()

    suspend fun getDocumentById(id: Long): CapturedDocument? = dao.getDocumentById(id)

    suspend fun insertDocument(document: CapturedDocument): Long = dao.insertDocument(document)

    suspend fun updateDocument(document: CapturedDocument) = dao.updateDocument(document)

    suspend fun deleteDocumentById(id: Long) {
        dao.deleteDocumentById(id)
        dao.deleteRelationshipsForDoc(id)
    }

    suspend fun insertRelationship(relationship: BrainRelationship): Long = dao.insertRelationship(relationship)

    suspend fun insertChatMessage(message: ChatMessage): Long = dao.insertChatMessage(message)

    suspend fun clearChatHistory() = dao.clearChatHistory()
}
