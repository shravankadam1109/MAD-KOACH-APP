package com.koach.studio.core

import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.ServiceCategory
import com.koach.studio.core.ui.formatInr
import com.koach.studio.projects.data.FakeProjectDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Guards the demo dataset against dangling references and duplicate ids. */
class DemoDataTest {

    private val data = DemoData(LocalDate.of(2026, 10, 22))
    private val projects = FakeProjectDataSource.createInitialProjects()
    private val userIds = data.users.map { it.id }.toSet()
    private val clientIds = data.clients.map { it.id }.toSet()
    private val projectIds = projects.map { it.id }.toSet()

    @Test
    fun ids_areUniqueWithinEachCollection() {
        listOf(
            data.users.map { it.id }, data.clients.map { it.id }, data.services.map { it.id },
            data.portfolio.map { it.id }, data.bookings.map { it.id }, data.tasks.map { it.id },
            data.documents.map { it.id }, data.threads.map { it.id }, data.messages.map { it.id },
            data.notifications.map { it.id }, data.payments.map { it.id }, data.feedback.map { it.id }
        ).forEach { ids -> assertEquals(ids.size, ids.toSet().size) }
    }

    @Test
    fun everyReference_pointsToAnExistingRecord() {
        data.users.mapNotNull { it.clientId }.forEach { assertTrue(it in clientIds) }
        projects.forEach { p ->
            assertTrue("project ${p.id} client", p.clientId in clientIds)
            p.teamIds.forEach { assertTrue(it in userIds) }
            p.revisions.forEach { assertTrue(it.requestedBy in userIds) }
        }
        val packageIds = data.services.flatMap { s -> s.packages.map { it.id } }.toSet()
        data.bookings.forEach { b ->
            assertTrue(b.serviceId in data.services.map { it.id })
            b.packageId?.let { assertTrue(it in packageIds) }
            b.clientId?.let { assertTrue(it in clientIds) }
        }
        data.tasks.forEach { t ->
            t.projectId?.let { assertTrue(it in projectIds) }
            t.assigneeId?.let { assertTrue(it in userIds) }
        }
        data.documents.forEach { d ->
            d.projectId?.let { assertTrue(it in projectIds) }
            d.clientId?.let { assertTrue(it in clientIds) }
            assertTrue(d.uploadedBy in userIds)
        }
        data.threads.forEach { th ->
            assertTrue(th.projectId in projectIds)
            th.participantIds.forEach { assertTrue(it in userIds) }
        }
        val threadIds = data.threads.map { it.id }.toSet()
        data.messages.forEach { m ->
            assertTrue(m.threadId in threadIds)
            assertTrue(m.senderId in userIds)
        }
        data.notifications.forEach { assertTrue(it.recipientId in userIds) }
        data.payments.forEach { assertTrue(it.clientId in clientIds) }
        data.feedback.forEach { assertTrue(it.clientId in clientIds) }
    }

    @Test
    fun catalogue_coversEveryServiceCategory_andDemoRolesExist() {
        assertEquals(ServiceCategory.entries.toSet(), data.services.map { it.category }.toSet())
        assertTrue(data.portfolio.none { it.isSample })
        assertTrue(data.portfolio.all { it.cover != null })
        Role.entries.forEach { role -> assertTrue(data.users.any { it.role == role }) }
    }

    @Test
    fun formatInr_usesIndianGrouping() {
        assertEquals("₹1,20,000", formatInr(120_000))
        assertEquals("₹999", formatInr(999))
        assertEquals("₹12,34,56,789", formatInr(123_456_789))
    }
}
