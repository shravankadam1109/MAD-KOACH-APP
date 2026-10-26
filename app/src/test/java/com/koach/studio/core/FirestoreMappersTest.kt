package com.koach.studio.core

import com.koach.studio.core.data.DemoData
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Every record written to Firestore must read back unchanged, and bad documents must not crash a list. */
class FirestoreMappersTest {

    private val demo = DemoData(LocalDate.of(2026, 10, 1))

    @Test
    fun projects_roundTripWithDeliverablesRevisionsAndMilestones() {
        FakeProjectDataSource.createInitialProjects().forEach { p ->
            val deliverables = p.deliverables.map { d -> FirestoreMappers.deliverable(d.id, p.id, FirestoreMappers.deliverableFields(d))!! }
            val revisions = p.revisions.map { r -> FirestoreMappers.revision(r.id, p.id, FirestoreMappers.revisionFields(r))!! }
            val back = FirestoreMappers.project(p.id, FirestoreMappers.projectFields(p), deliverables, revisions)
            assertEquals(p.copy(revisions = p.revisions.sortedByDescending { it.requestedOn }), back)
        }
    }

    @Test
    fun records_roundTrip() {
        demo.clients.forEach { assertEquals(it, FirestoreMappers.client(it.id, FirestoreMappers.clientFields(it))) }
        demo.bookings.forEach { assertEquals(it, FirestoreMappers.booking(it.id, FirestoreMappers.bookingFields(it))) }
        demo.tasks.forEach { assertEquals(it, FirestoreMappers.task(it.id, FirestoreMappers.taskFields(it))) }
        demo.documents.forEach { assertEquals(it, FirestoreMappers.document(it.id, FirestoreMappers.documentFields(it))) }
        demo.threads.forEach { assertEquals(it, FirestoreMappers.thread(it.id, FirestoreMappers.threadFields(it))) }
        demo.messages.forEach { assertEquals(it, FirestoreMappers.message(it.id, it.threadId, FirestoreMappers.messageFields(it))) }
        demo.notifications.forEach { assertEquals(it, FirestoreMappers.notification(it.id, FirestoreMappers.notificationFields(it))) }
        demo.payments.forEach { assertEquals(it, FirestoreMappers.payment(it.id, FirestoreMappers.paymentFields(it))) }
        demo.feedback.forEach { assertEquals(it, FirestoreMappers.feedback(it.id, FirestoreMappers.feedbackFields(it))) }
    }

    @Test
    fun malformedDocuments_mapToNull() {
        val p = FakeProjectDataSource.createInitialProjects().first()
        val fields = FirestoreMappers.projectFields(p)
        assertNull(FirestoreMappers.project(p.id, fields - "title", emptyList(), emptyList()))
        assertNull(FirestoreMappers.project(p.id, fields + ("status" to "LOST"), emptyList(), emptyList()))
        assertNull(FirestoreMappers.project(p.id, fields + ("dueDate" to "next week"), emptyList(), emptyList()))
        assertNull(FirestoreMappers.message("m", "t", mapOf("senderId" to "u")))
        assertNull(FirestoreMappers.booking("b", emptyMap()))
    }

    @Test
    fun badMilestone_isSkipped_notTheWholeProject() {
        val p = FakeProjectDataSource.createInitialProjects().first { it.milestones.isNotEmpty() }
        val fields = FirestoreMappers.projectFields(p) + ("milestones" to FirestoreMappers.milestones(p.milestones) + mapOf("id" to "broken"))
        val back = FirestoreMappers.project(p.id, fields, emptyList(), emptyList())!!
        assertEquals(p.milestones, back.milestones)
        assertEquals(ProjectStatus.entries.first { it == p.status }, back.status)
    }

    @Test
    fun numbersFromFirestore_areReadAsLongs() {
        val payment = demo.payments.first()
        val fields = FirestoreMappers.paymentFields(payment) + ("amountInr" to payment.amountInr.toDouble())
        assertEquals(payment.amountInr, FirestoreMappers.payment(payment.id, fields)!!.amountInr)
    }
}
