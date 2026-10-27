package com.koach.studio.core.firebase

import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User

/**
 * Converts a trusted `users/{uid}` Firestore document into a [User].
 *
 * Profiles are written only by studio admins (enforced in firestore.rules), so
 * the role here is trusted data, never something the app or user supplied at
 * sign-in. Invalid or incomplete profiles map to null and the session is refused.
 */
object UserProfileMapper {

    fun fromMap(uid: String, data: Map<String, Any?>?): User? {
        if (data == null) return null
        val role = when (data["role"] as? String) {
            "client" -> Role.CLIENT
            "team" -> Role.TEAM
            "admin" -> Role.ADMIN
            else -> return null
        }
        val name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val phone = (data["phone"] as? String).orEmpty()
        // Phone sign-ups may have no email; every account needs one way to reach it.
        val email = (data["email"] as? String).orEmpty()
        if (email.isBlank() && phone.isBlank()) return null
        val clientId = (data["clientId"] as? String)?.takeIf { it.isNotBlank() }
        // A client account must belong to a client organisation.
        if (role == Role.CLIENT && clientId == null) return null
        return User(
            id = uid,
            name = name,
            email = email,
            role = role,
            title = (data["title"] as? String).orEmpty(),
            clientId = if (role == Role.CLIENT) clientId else null,
            phone = phone
        )
    }

    fun toMap(user: User): Map<String, Any?> = mapOf(
        "name" to user.name,
        "email" to user.email,
        "role" to when (user.role) {
            Role.CLIENT -> "client"
            Role.TEAM -> "team"
            Role.ADMIN -> "admin"
        },
        "title" to user.title,
        "clientId" to user.clientId,
        "phone" to user.phone
    )
}
