package com.koach.studio.core.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class ThreadType(val displayName: String) {
    DM("Direct Messages"),
    PROJECT("Team / Project")
}

data class MessageThread(
    val id: String,
    val projectId: String,
    val title: String,
    val participantIds: List<String>,
    val type: ThreadType = if (projectId.isBlank() || projectId.startsWith("dm-")) ThreadType.DM else ThreadType.PROJECT
) {
    val isDirectMessage: Boolean get() = type == ThreadType.DM
}

data class Message(
    val id: String,
    val threadId: String,
    val senderId: String,
    val body: String,
    val sentAt: LocalDateTime,
    val readBy: Set<String>
)

enum class NotificationType(val displayName: String) {
    PROJECT("Project"),
    DELIVERABLE("Deliverable"),
    MESSAGE("Message"),
    BOOKING("Booking"),
    PAYMENT("Payment"),
    TASK("Task"),
    FEEDBACK("Feedback")
}

data class AppNotification(
    val id: String,
    val recipientId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime,
    val isRead: Boolean,
    /** Optional entity the notification is about, e.g. a project id. */
    val targetId: String? = null
)

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    DUE("Due"),
    OVERDUE("Overdue")
}

/** An invoice record. Status is simulated; no payment processor is connected. */
data class Payment(
    val id: String,
    val invoiceNumber: String,
    val clientId: String,
    val projectId: String?,
    val description: String,
    val amountInr: Long,
    val issuedOn: LocalDate,
    val dueOn: LocalDate,
    val status: PaymentStatus,
    val paidOn: LocalDate? = null
)

enum class FeedbackType(val displayName: String) {
    FEEDBACK("Feedback"),
    COMPLAINT("Complaint")
}

enum class FeedbackStatus(val displayName: String) {
    OPEN("Open"),
    IN_PROGRESS("In progress"),
    RESOLVED("Resolved")
}

data class Feedback(
    val id: String,
    val clientId: String,
    val authorId: String,
    val projectId: String?,
    val type: FeedbackType,
    /** 1..5 for feedback; null for complaints. */
    val rating: Int?,
    val subject: String,
    val message: String,
    val status: FeedbackStatus,
    val createdOn: LocalDate,
    val response: String? = null
)
