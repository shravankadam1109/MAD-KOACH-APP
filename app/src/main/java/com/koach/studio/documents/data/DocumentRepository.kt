package com.koach.studio.documents.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/** Metadata for a new document. The file itself is handled by [DocumentStorage]. */
data class DocumentDraft(
    val name: String,
    val category: DocumentCategory,
    val projectId: String?,
    val clientId: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val visibility: DocumentVisibility
)

/**
 * Where file bytes would live (e.g. Firebase Storage). The demo build has no
 * storage, so documents are saved as metadata only and nothing is uploaded.
 */
interface DocumentStorage {
    val isAvailable: Boolean
}

object DemoDocumentStorage : DocumentStorage {
    override val isAvailable: Boolean = false
}

interface DocumentRepository {
    /** Agency users see everything; clients only documents shared with their organisation. */
    fun observeDocuments(): Flow<List<StudioDocument>>
    suspend fun addDocument(draft: DocumentDraft): Result<StudioDocument>
    suspend fun setVisibility(documentId: String, visibility: DocumentVisibility): Result<Unit>
    suspend fun deleteDocument(documentId: String): Result<Unit>
}

class InMemoryDocumentRepository(
    initial: List<StudioDocument>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock
) : DocumentRepository {

    private val store = MutableStateFlow(initial)

    override fun observeDocuments(): Flow<List<StudioDocument>> =
        combine(store, currentUser) { all, user ->
            when {
                user == null -> emptyList()
                user.can(Permission.VIEW_INTERNAL_DOCUMENTS) -> all
                else -> all.filter { it.visibility == DocumentVisibility.SHARED && it.clientId != null && it.clientId == user.clientId }
            }.sortedByDescending { it.uploadedAt }
        }

    override suspend fun addDocument(draft: DocumentDraft): Result<StudioDocument> = runCatching {
        val user = access.require(Permission.UPLOAD_DOCUMENTS)
        require(draft.name.isNotBlank()) { "Give the document a name" }
        require(draft.sizeBytes >= 0) { "Invalid file size" }
        // Clients can only add shared documents to their own organisation.
        val clientId = if (user.role.isAgency) draft.clientId else user.clientId
        access.requireClientAccess(clientId ?: user.clientId)
        val visibility = if (user.role.isAgency) draft.visibility else DocumentVisibility.SHARED
        val document = StudioDocument(
            id = "doc-" + UUID.randomUUID().toString().take(8),
            name = draft.name.trim(),
            category = draft.category,
            projectId = draft.projectId,
            clientId = clientId,
            mimeType = draft.mimeType.ifBlank { "application/octet-stream" },
            sizeBytes = draft.sizeBytes,
            uploadedBy = user.id,
            uploadedAt = LocalDateTime.now(clock),
            visibility = visibility,
            storagePath = null
        )
        store.update { listOf(document) + it }
        document
    }

    override suspend fun setVisibility(documentId: String, visibility: DocumentVisibility): Result<Unit> = runCatching {
        access.require(Permission.VIEW_INTERNAL_DOCUMENTS)
        if (store.value.none { it.id == documentId }) throw IllegalArgumentException("Document not found")
        store.update { list -> list.map { if (it.id == documentId) it.copy(visibility = visibility) else it } }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> = runCatching {
        val user = access.user() ?: throw PermissionDeniedException("Please sign in")
        val document = store.value.find { it.id == documentId } ?: throw IllegalArgumentException("Document not found")
        if (!user.can(Permission.VIEW_INTERNAL_DOCUMENTS) && document.uploadedBy != user.id) {
            throw PermissionDeniedException("You can only remove documents you added")
        }
        store.update { list -> list.filterNot { it.id == documentId } }
    }
}
