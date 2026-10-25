package com.koach.studio.clients.data

import com.google.firebase.firestore.FirebaseFirestore
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate

/** Client organisations in `clients/{id}`. Clients only ever load their own document. */
class FirestoreClientRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock
) : ClientRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeClients(): Flow<List<Client>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            if (user.role.isAgency) {
                collection.observe { snap ->
                    snap.documents.mapNotNull { FirestoreMappers.client(it.id, it.data.orEmpty()) }.sortedBy { it.name.lowercase() }
                }
            } else {
                val id = user.clientId ?: return@whenSignedIn flowOf(emptyList())
                collection.document(id).observe { FirestoreMappers.client(it.id, it.data.orEmpty()) }.map { listOfNotNull(it) }
            }
        }

    override fun observeClient(clientId: String): Flow<Client?> = observeClients().map { list -> list.find { it.id == clientId } }

    override suspend fun createClient(draft: ClientDraft): Result<Client> = runCatching {
        access.require(Permission.MANAGE_CLIENTS)
        validateOrThrow(draft)
        val ref = collection.document()
        val client = draft.toClient(ref.id, LocalDate.now(clock))
        ref.set(FirestoreMappers.clientFields(client)).await()
        client
    }

    override suspend fun updateClient(clientId: String, draft: ClientDraft): Result<Client> = runCatching {
        access.require(Permission.MANAGE_CLIENTS)
        validateOrThrow(draft)
        val snap = collection.document(clientId).get().await()
        val existing = FirestoreMappers.client(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Client not found")
        val updated = draft.toClient(existing.id, existing.since)
        collection.document(clientId).set(FirestoreMappers.clientFields(updated)).await()
        updated
    }

    override suspend fun deleteClient(clientId: String): Result<Unit> = runCatching {
        access.require(Permission.DELETE_CLIENTS)
        if (!collection.document(clientId).get().await().exists()) throw IllegalArgumentException("Client not found")
        val linked = firestore.collection("projects").whereEqualTo("clientId", clientId).get().await().size()
        if (linked > 0) throw ClientHasProjectsException(linked)
        collection.document(clientId).delete().await()
        Unit
    }

    private fun validateOrThrow(draft: ClientDraft) {
        val errors = ClientValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
    }

    private fun ClientDraft.toClient(id: String, since: LocalDate) = Client(
        id, name.trim(), contactName.trim(), email.trim(), phone.trim(), industry.trim(), city.trim(), status, since, notes.trim()
    )

    companion object {
        const val COLLECTION = "clients"
    }
}
