package com.koach.studio.core

import com.koach.studio.core.firebase.UserProfileMapper
import com.koach.studio.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileMapperTest {

    private val client = mapOf("role" to "client", "name" to "Maya Rao", "email" to "maya@example.com", "clientId" to "cl-1", "title" to "Label Manager")

    @Test
    fun validProfiles_mapToTheirTrustedRole() {
        val user = UserProfileMapper.fromMap("uid-1", client)!!
        assertEquals(Role.CLIENT, user.role)
        assertEquals("cl-1", user.clientId)
        assertEquals("uid-1", user.id)
        assertEquals(Role.ADMIN, UserProfileMapper.fromMap("uid-2", mapOf("role" to "admin", "name" to "A", "email" to "a@x.io"))?.role)
    }

    @Test
    fun missingOrUnknownRole_isRejected() {
        assertNull(UserProfileMapper.fromMap("u", client - "role"))
        assertNull(UserProfileMapper.fromMap("u", client + ("role" to "superuser")))
        assertNull(UserProfileMapper.fromMap("u", null))
    }

    @Test
    fun clientWithoutOrganisation_orMissingIdentity_isRejected() {
        assertNull(UserProfileMapper.fromMap("u", client - "clientId"))
        assertNull(UserProfileMapper.fromMap("u", client + ("name" to " ")))
        assertNull(UserProfileMapper.fromMap("u", client - "email"))
    }

    @Test
    fun agencyAccounts_neverCarryAClientId() {
        val team = UserProfileMapper.fromMap("u", mapOf("role" to "team", "name" to "Z", "email" to "z@x.io", "clientId" to "cl-1"))!!
        assertNull(team.clientId)
    }

    @Test
    fun toMap_roundTrips() {
        val user = UserProfileMapper.fromMap("uid-1", client)!!
        assertEquals(user, UserProfileMapper.fromMap("uid-1", UserProfileMapper.toMap(user)))
    }
}
