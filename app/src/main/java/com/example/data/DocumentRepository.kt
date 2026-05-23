package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: Int): Document? = documentDao.getDocumentById(id)

    suspend fun insertDocument(document: Document): Long = documentDao.insertDocument(document)

    suspend fun updateDocument(document: Document) = documentDao.updateDocument(document)

    suspend fun updateReadPosition(id: Int, position: Int) = documentDao.updateReadPosition(id, position)

    suspend fun deleteDocumentById(id: Int) = documentDao.deleteDocumentById(id)
}
