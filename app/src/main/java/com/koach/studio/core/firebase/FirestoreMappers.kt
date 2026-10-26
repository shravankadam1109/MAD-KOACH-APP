package com.koach.studio.core.firebase

import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.ClientStatus
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.Feedback
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Milestone
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.Revision
import com.koach.studio.projects.model.ServiceType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Firestore <-> model conversion. Pure Kotlin so it is unit-tested on the JVM.
 *
 * Dates are stored as ISO-8601 text (`2026-10-26`, `2026-10-26T14:05:00`):
 * they sort correctly in queries and match the app's local-date models.
 * Readers are lenient: a malformed document maps to null and is skipped rather
 * than crashing a list.
 */
object FirestoreMappers {

    // ---- shared helpers --------------------------------------------------

    private fun Map<String, Any?>.str(key: String): String? = this[key] as? String
    private fun Map<String, Any?>.req(key: String): String = str(key) ?: throw IllegalArgumentException("missing $key")
    private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()
    private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.strings(key: String): List<String> = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
    private fun Map<String, Any?>.date(key: String): LocalDate = LocalDate.parse(req(key))
    private fun Map<String, Any?>.dateTime(key: String): LocalDateTime = LocalDateTime.parse(req(key))
    private inline fun <reified E : Enum<E>> Map<String, Any?>.enum(key: String): E = enumValueOf(req(key))

    private inline fun <T> lenient(block: () -> T): T? = try {
        block()
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: java.time.DateTimeException) {
        null
    }

    // ---- projects --------------------------------------------------------

    fun project(id: String, data: Map<String, Any?>, deliverables: List<Deliverable>, revisions: List<Revision>): Project? = lenient {
        @Suppress("UNCHECKED_CAST")
        val milestones = (data["milestones"] as? List<Map<String, Any?>>).orEmpty().mapNotNull { m ->
            lenient { Milestone(m.req("id"), m.req("title"), m.date("dueDate"), m.bool("completed")) }
        }
        Project(
            id = id,
            title = data.req("title"),
            clientName = data.str("clientName").orEmpty(),
            serviceType = data.enum<ServiceType>("serviceType"),
            status = data.enum<ProjectStatus>("status"),
            dueDate = data.date("dueDate"),
            description = data.str("description").orEmpty(),
            deliverables = deliverables,
            clientId = data.req("clientId"),
            milestones = milestones,
            revisions = revisions.sortedByDescending { it.requestedOn },
            teamIds = data.strings("teamIds")
        )
    }

    fun projectFields(p: Project): Map<String, Any?> = mapOf(
        "title" to p.title,
        "clientName" to p.clientName,
        "serviceType" to p.serviceType.name,
        "status" to p.status.name,
        "dueDate" to p.dueDate.toString(),
        "description" to p.description,
        "clientId" to p.clientId,
        "milestones" to milestones(p.milestones),
        "teamIds" to p.teamIds
    )

    fun milestones(list: List<Milestone>): List<Map<String, Any?>> =
        list.map { mapOf("id" to it.id, "title" to it.title, "dueDate" to it.dueDate.toString(), "completed" to it.completed) }

    fun deliverable(id: String, projectId: String, data: Map<String, Any?>): Deliverable? = lenient {
        Deliverable(
            id = id,
            projectId = projectId,
            name = data.req("name"),
            type = data.str("type").orEmpty(),
            size = data.str("size").orEmpty(),
            version = data.str("version") ?: "v1.0",
            notes = data.str("notes").orEmpty(),
            status = data.enum<DeliverableStatus>("status"),
            previousVersions = data.strings("previousVersions")
        )
    }

    fun deliverableFields(d: Deliverable): Map<String, Any?> = mapOf(
        "name" to d.name,
        "type" to d.type,
        "size" to d.size,
        "version" to d.version,
        "notes" to d.notes,
        "status" to d.status.name,
        "previousVersions" to d.previousVersions
    )

    fun revision(id: String, projectId: String, data: Map<String, Any?>): Revision? = lenient {
        Revision(id, projectId, data.str("deliverableId"), data.req("reason"), data.req("requestedBy"), data.date("requestedOn"), data.bool("resolved"))
    }

    fun revisionFields(r: Revision): Map<String, Any?> = mapOf(
        "deliverableId" to r.deliverableId,
        "reason" to r.reason,
        "requestedBy" to r.requestedBy,
        "requestedOn" to r.requestedOn.toString(),
        "resolved" to r.resolved
    )

    // ---- clients, bookings, tasks, documents -----------------------------

    fun client(id: String, data: Map<String, Any?>): Client? = lenient {
        Client(
            id, data.req("name"), data.str("contactName").orEmpty(), data.str("email").orEmpty(), data.str("phone").orEmpty(),
            data.str("industry").orEmpty(), data.str("city").orEmpty(), data.enum<ClientStatus>("status"), data.date("since"),
            data.str("notes").orEmpty()
        )
    }

    fun clientFields(c: Client): Map<String, Any?> = mapOf(
        "name" to c.name, "contactName" to c.contactName, "email" to c.email, "phone" to c.phone, "industry" to c.industry,
        "city" to c.city, "status" to c.status.name, "since" to c.since.toString(), "notes" to c.notes
    )

    fun booking(id: String, data: Map<String, Any?>): Booking? = lenient {
        Booking(
            id, data.str("clientId"), data.req("requestedBy"), data.str("contactName").orEmpty(), data.str("contactEmail").orEmpty(),
            data.req("serviceId"), data.str("packageId"), data.date("preferredDate"), data.long("budgetInr"), data.str("details").orEmpty(),
            data.enum<BookingStatus>("status"), data.date("createdOn"), data.str("statusNote")
        )
    }

    fun bookingFields(b: Booking): Map<String, Any?> = mapOf(
        "clientId" to b.clientId, "requestedBy" to b.requestedBy, "contactName" to b.contactName, "contactEmail" to b.contactEmail,
        "serviceId" to b.serviceId, "packageId" to b.packageId, "preferredDate" to b.preferredDate.toString(), "budgetInr" to b.budgetInr,
        "details" to b.details, "status" to b.status.name, "createdOn" to b.createdOn.toString(), "statusNote" to b.statusNote
    )

    fun task(id: String, data: Map<String, Any?>): TaskItem? = lenient {
        TaskItem(
            id, data.req("title"), data.str("description").orEmpty(), data.str("projectId"), data.str("assigneeId"),
            data.enum<TaskPriority>("priority"), data.enum<TaskStatus>("status"), data.date("dueDate"), data.str("createdBy").orEmpty(),
            data.date("createdOn")
        )
    }

    fun taskFields(t: TaskItem): Map<String, Any?> = mapOf(
        "title" to t.title, "description" to t.description, "projectId" to t.projectId, "assigneeId" to t.assigneeId,
        "priority" to t.priority.name, "status" to t.status.name, "dueDate" to t.dueDate.toString(), "createdBy" to t.createdBy,
        "createdOn" to t.createdOn.toString()
    )

    fun document(id: String, data: Map<String, Any?>): StudioDocument? = lenient {
        StudioDocument(
            id, data.req("name"), data.enum<DocumentCategory>("category"), data.str("projectId"), data.str("clientId"),
            data.str("mimeType") ?: "application/octet-stream", data.long("sizeBytes") ?: 0, data.req("uploadedBy"),
            data.dateTime("uploadedAt"), data.enum<DocumentVisibility>("visibility"), data.str("storagePath")
        )
    }

    fun documentFields(d: StudioDocument): Map<String, Any?> = mapOf(
        "name" to d.name, "category" to d.category.name, "projectId" to d.projectId, "clientId" to d.clientId, "mimeType" to d.mimeType,
        "sizeBytes" to d.sizeBytes, "uploadedBy" to d.uploadedBy, "uploadedAt" to d.uploadedAt.toString(), "visibility" to d.visibility.name,
        "storagePath" to d.storagePath
    )

    // ---- messaging, notifications, payments, feedback --------------------

    fun thread(id: String, data: Map<String, Any?>): MessageThread? = lenient {
        val projId = data.str("projectId").orEmpty()
        val typeStr = data.str("type")
        val threadType = if (typeStr != null) {
            runCatching { com.koach.studio.core.model.ThreadType.valueOf(typeStr) }.getOrDefault(
                if (projId.isBlank() || projId.startsWith("dm-")) com.koach.studio.core.model.ThreadType.DM else com.koach.studio.core.model.ThreadType.PROJECT
            )
        } else {
            if (projId.isBlank() || projId.startsWith("dm-")) com.koach.studio.core.model.ThreadType.DM else com.koach.studio.core.model.ThreadType.PROJECT
        }
        MessageThread(id, projId, data.req("title"), data.strings("participantIds"), threadType)
    }

    fun threadFields(t: MessageThread, lastMessageAt: LocalDateTime? = null): Map<String, Any?> = mapOf(
        "projectId" to t.projectId, "title" to t.title, "participantIds" to t.participantIds, "type" to t.type.name, "lastMessageAt" to lastMessageAt?.toString()
    )

    fun message(id: String, threadId: String, data: Map<String, Any?>): Message? = lenient {
        Message(id, threadId, data.req("senderId"), data.req("body"), data.dateTime("sentAt"), data.strings("readBy").toSet())
    }

    fun messageFields(m: Message): Map<String, Any?> = mapOf(
        "senderId" to m.senderId, "body" to m.body, "sentAt" to m.sentAt.toString(), "readBy" to m.readBy.toList()
    )

    fun notification(id: String, data: Map<String, Any?>): AppNotification? = lenient {
        AppNotification(
            id, data.req("recipientId"), data.enum<NotificationType>("type"), data.req("title"), data.str("body").orEmpty(),
            data.dateTime("createdAt"), data.bool("isRead"), data.str("targetId")
        )
    }

    fun notificationFields(n: AppNotification): Map<String, Any?> = mapOf(
        "recipientId" to n.recipientId, "type" to n.type.name, "title" to n.title, "body" to n.body,
        "createdAt" to n.createdAt.toString(), "isRead" to n.isRead, "targetId" to n.targetId
    )

    fun payment(id: String, data: Map<String, Any?>): Payment? = lenient {
        Payment(
            id, data.req("invoiceNumber"), data.req("clientId"), data.str("projectId"), data.str("description").orEmpty(),
            data.long("amountInr") ?: 0, data.date("issuedOn"), data.date("dueOn"), data.enum<PaymentStatus>("status"),
            data.str("paidOn")?.let(LocalDate::parse)
        )
    }

    fun paymentFields(p: Payment): Map<String, Any?> = mapOf(
        "invoiceNumber" to p.invoiceNumber, "clientId" to p.clientId, "projectId" to p.projectId, "description" to p.description,
        "amountInr" to p.amountInr, "issuedOn" to p.issuedOn.toString(), "dueOn" to p.dueOn.toString(), "status" to p.status.name,
        "paidOn" to p.paidOn?.toString()
    )

    fun feedback(id: String, data: Map<String, Any?>): Feedback? = lenient {
        Feedback(
            id, data.req("clientId"), data.req("authorId"), data.str("projectId"), data.enum<FeedbackType>("type"),
            data.long("rating")?.toInt(), data.req("subject"), data.str("message").orEmpty(), data.enum<FeedbackStatus>("status"),
            data.date("createdOn"), data.str("response")
        )
    }

    fun feedbackFields(f: Feedback): Map<String, Any?> = mapOf(
        "clientId" to f.clientId, "authorId" to f.authorId, "projectId" to f.projectId, "type" to f.type.name,
        "rating" to f.rating, "subject" to f.subject, "message" to f.message, "status" to f.status.name,
        "createdOn" to f.createdOn.toString(), "response" to f.response
    )
}
