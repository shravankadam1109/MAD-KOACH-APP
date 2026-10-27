package com.koach.studio.core.auth

import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User

enum class Permission {
    VIEW_ALL_CLIENTS,
    MANAGE_CLIENTS,
    DELETE_CLIENTS,
    VIEW_ALL_PROJECTS,
    MANAGE_PROJECTS,
    /** Agency-side deliverable edits: name, notes, draft/in-review status. */
    EDIT_DELIVERABLES,
    /** Client sign-off on deliverables shared for review. */
    REVIEW_DELIVERABLES,
    REQUEST_REVISIONS,
    CREATE_BOOKINGS,
    MANAGE_BOOKINGS,
    VIEW_TASKS,
    MANAGE_TASKS,
    VIEW_INTERNAL_DOCUMENTS,
    UPLOAD_DOCUMENTS,
    SEND_MESSAGES,
    VIEW_ALL_PAYMENTS,
    SUBMIT_FEEDBACK,
    RESPOND_FEEDBACK,
    /** Turning a requested studio sign-in into a team or admin account. */
    MANAGE_TEAM_ACCESS
}

/** Role → permission table. The single source of truth for what each role may do. */
object AccessPolicy {
    private val client = setOf(
        Permission.REVIEW_DELIVERABLES,
        Permission.REQUEST_REVISIONS,
        Permission.CREATE_BOOKINGS,
        Permission.UPLOAD_DOCUMENTS,
        Permission.SEND_MESSAGES,
        Permission.SUBMIT_FEEDBACK
    )
    private val team = setOf(
        Permission.VIEW_ALL_CLIENTS,
        Permission.MANAGE_CLIENTS,
        Permission.VIEW_ALL_PROJECTS,
        Permission.MANAGE_PROJECTS,
        Permission.EDIT_DELIVERABLES,
        Permission.MANAGE_BOOKINGS,
        Permission.VIEW_TASKS,
        Permission.MANAGE_TASKS,
        Permission.VIEW_INTERNAL_DOCUMENTS,
        Permission.UPLOAD_DOCUMENTS,
        Permission.SEND_MESSAGES,
        Permission.RESPOND_FEEDBACK
    )
    private val admin = team + setOf(Permission.DELETE_CLIENTS, Permission.VIEW_ALL_PAYMENTS, Permission.MANAGE_TEAM_ACCESS)

    fun permissionsFor(role: Role): Set<Permission> = when (role) {
        Role.CLIENT -> client
        Role.TEAM -> team
        Role.ADMIN -> admin
    }
}

fun User.can(permission: Permission): Boolean = permission in AccessPolicy.permissionsFor(role)

class PermissionDeniedException(message: String) : SecurityException(message)
class NotSignedInException : SecurityException("Please sign in to continue")

/**
 * Permission checks for repositories. Repositories call this before every read
 * filter and write so rules hold even if a screen forgets to hide an action,
 * mirroring how Firestore security rules would guard a real backend.
 */
class AccessControl(private val currentUser: () -> User?) {

    fun user(): User? = currentUser()

    fun can(permission: Permission): Boolean = currentUser()?.can(permission) == true

    /** Agency users see every client; client users only their own organisation. */
    fun canAccessClient(clientId: String?): Boolean {
        val user = currentUser() ?: return false
        return user.role.isAgency || (clientId != null && user.clientId == clientId)
    }

    /** Returns the signed-in user, or throws if signed out or lacking [permission]. */
    fun require(permission: Permission): User {
        val user = currentUser() ?: throw NotSignedInException()
        if (!user.can(permission)) {
            throw PermissionDeniedException("${user.role.displayName} accounts can't do that")
        }
        return user
    }

    fun requireClientAccess(clientId: String?): User {
        val user = currentUser() ?: throw NotSignedInException()
        if (!canAccessClient(clientId)) throw PermissionDeniedException("You don't have access to this client")
        return user
    }
}
