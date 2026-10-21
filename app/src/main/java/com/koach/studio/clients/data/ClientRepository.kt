package com.koach.studio.clients.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.ClientStatus
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** Editable client fields; id and "since" are assigned by the repository. */
data class ClientDraft(
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val industry: String,
    val city: String,
    val status: ClientStatus,
    val notes: String
)

class ClientHasProjectsException(count: Int) :
    IllegalStateException("This client has $count linked project${if (count == 1) "" else "s"}. Archive it instead of deleting.")

interface ClientRepository {
    fun observeClients(): Flow<List<Client>>
    fun observeClient(clientId: String): Flow<Client?>
    suspend fun createClient(draft: ClientDraft): Result<Client>
    suspend fun updateClient(clientId: String, draft: ClientDraft): Result<Client>
    suspend fun deleteClient(clientId: String): Result<Unit>
}

class InMemoryClientRepository(
    initial: List<Client>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock,
    /** Number of projects linked to a client; deletion is blocked while any exist. */
    private val linkedProjectCount: suspend (clientId: String) -> Int
) : ClientRepository {

    private val store = MutableStateFlow(initial)

    override fun observeClients(): Flow<List<Client>> =
        combine(store, currentUser) { all, user ->
            if (user == null) emptyList() else all.filter { access.canAccessClient(it.id) }.sortedBy { it.name.lowercase() }
        }

    override fun observeClient(clientId: String): Flow<Client?> =
        observeClients().map { list -> list.find { it.id == clientId } }

    override suspend fun createClient(draft: ClientDraft): Result<Client> = runCatching {
        access.require(Permission.MANAGE_CLIENTS)
        validateOrThrow(draft)
        val client = draft.toClient(id = "cl-" + UUID.randomUUID().toString().take(8), since = LocalDate.now(clock))
        store.update { it + client }
        client
    }

    override suspend fun updateClient(clientId: String, draft: ClientDraft): Result<Client> = runCatching {
        access.require(Permission.MANAGE_CLIENTS)
        validateOrThrow(draft)
        val existing = store.value.find { it.id == clientId } ?: throw IllegalArgumentException("Client not found")
        val updated = draft.toClient(id = existing.id, since = existing.since)
        store.update { list -> list.map { if (it.id == clientId) updated else it } }
        updated
    }

    override suspend fun deleteClient(clientId: String): Result<Unit> = runCatching {
        access.require(Permission.DELETE_CLIENTS)
        if (store.value.none { it.id == clientId }) throw IllegalArgumentException("Client not found")
        val projects = linkedProjectCount(clientId)
        if (projects > 0) throw ClientHasProjectsException(projects)
        store.update { list -> list.filterNot { it.id == clientId } }
    }

    private fun validateOrThrow(draft: ClientDraft) {
        val errors = ClientValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
    }

    private fun ClientDraft.toClient(id: String, since: LocalDate) = Client(
        id = id,
        name = name.trim(),
        contactName = contactName.trim(),
        email = email.trim(),
        phone = phone.trim(),
        industry = industry.trim(),
        city = city.trim(),
        status = status,
        since = since,
        notes = notes.trim()
    )
}
