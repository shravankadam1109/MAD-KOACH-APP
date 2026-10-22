package com.koach.studio.auth

import com.koach.studio.core.model.Role

/**
 * Which door the user came through. It only decides copy and which accounts are
 * offered; the role itself always comes from the signed-in account.
 */
enum class AuthPortal(val roles: Set<Role>) {
    CLIENT(setOf(Role.CLIENT)),
    AGENCY(setOf(Role.TEAM, Role.ADMIN));

    fun accepts(role: Role): Boolean = role in roles
}
