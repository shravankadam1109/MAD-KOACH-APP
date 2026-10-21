package com.koach.studio.core.model

import java.time.LocalDate

enum class Role(val displayName: String) {
    CLIENT("Client"),
    TEAM("Agency Team"),
    ADMIN("Studio Admin");

    val isAgency: Boolean get() = this != CLIENT
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: Role,
    val title: String,
    /** Set for client users: the client organisation they belong to. */
    val clientId: String? = null,
    val phone: String = ""
) {
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    val firstName: String get() = name.substringBefore(" ")
}

enum class ClientStatus(val displayName: String) {
    LEAD("Lead"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    ARCHIVED("Archived")
}

data class Client(
    val id: String,
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val industry: String,
    val city: String,
    val status: ClientStatus,
    val since: LocalDate,
    val notes: String = ""
) {
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}
