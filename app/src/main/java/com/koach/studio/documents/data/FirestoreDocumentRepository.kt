package com.koach.studio.documents.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.auth.can
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDateTime

/**
 * Document metadata in `documents/{id}`. Files are not uploaded yet: records
 * keep storagePath = null and the UI keeps saying so until Storage uploads land.
 */
class FirestoreDocumentRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock
) : DocumentRepository {

    private val collection get() = firestore.collection(COLLECTION)
    private val _events = MutableSharedFlow<DocumentEvent>(extraBufferCapacity = 8)
    override val events: Flow<DocumentEvent> = _events.asSharedFlow()

    override fun observeDocuments(): Flow<List<StudioDocument>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val query = if (user.can(Permission.VIEW_INTERNAL_DOCUMENTS)) collection
            else collection.whereEqualTo("clientId", user.clientId).whereEqualTo("visibility", DocumentVisibility.SHARED.name)
            query.orderBy("uploadedAt", Query.Direction.DESCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.document(it.id, it.data.orEmpty()) } }
        }

    override suspend fun addDocument(draft: DocumentDraft): Result<StudioDocument> = runCatching {
        val user = access.require(Permission.UPLOAD_DOCUMENTS)
        DocumentValidator.nameError(draft.name)?.let { throw IllegalArgumentException(it) }
        require(draft.sizeBytes >= 0) { "Invalid file size" }
        val clientId = if (user.role.isAgency) draft.clientId else user.clientId
        access.requireClientAccess(clientId ?: user.clientId)
        val ref = collection.document()
        val document = StudioDocument(
            id = ref.id, name = draft.name.trim(), category = draft.category, projectId = draft.projectId, clientId = clientId,
            mimeType = draft.mimeType.ifBlank { "application/octet-stream" }, sizeBytes = draft.sizeBytes, uploadedBy = user.id,
            uploadedAt = LocalDateTime.now(clock),
            visibility = if (user.role.isAgency) draft.visibility else DocumentVisibility.SHARED,
            storagePath = null
        )
        ref.set(FirestoreMappers.documentFields(document)).await()
        _events.tryEmit(DocumentEvent.Added(document.name))
        document
    }

    override suspend fun setVisibility(documentId: String, visibility: DocumentVisibility): Result<Unit> = runCatching {
        access.require(Permission.VIEW_INTERNAL_DOCUMENTS)
        collection.document(documentId).update("visibility", visibility.name).await()
        Unit
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> = runCatching {
        val user = access.user() ?: throw PermissionDeniedException("Please sign in")
        val snap = collection.document(documentId).get().await()
        val document = FirestoreMappers.document(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Document not found")
        if (!user.can(Permission.VIEW_INTERNAL_DOCUMENTS) && document.uploadedBy != user.id) {
            throw PermissionDeniedException("You can only remove documents you added")
        }
        collection.document(documentId).delete().await()
        Unit
    }

    companion object {
        const val COLLECTION = "documents"
    }
}
