package com.koach.studio.core.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class TaskStatus(val displayName: String) {
    TODO("To do"),
    IN_PROGRESS("In progress"),
    IN_REVIEW("In review"),
    DONE("Done")
}

enum class TaskPriority(val displayName: String, val rank: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
    URGENT("Urgent", 3)
}

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val projectId: String?,
    val assigneeId: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val dueDate: LocalDate,
    val createdBy: String,
    val createdOn: LocalDate
) {
    fun isOverdue(today: LocalDate): Boolean = status != TaskStatus.DONE && dueDate.isBefore(today)
}

enum class DocumentCategory(val displayName: String) {
    BRIEF("Brief"),
    CONTRACT("Contract"),
    INVOICE("Invoice"),
    SCRIPT("Script"),
    STORYBOARD("Storyboard"),
    ASSET("Asset"),
    REPORT("Report")
}

enum class DocumentVisibility(val displayName: String) {
    /** Visible to the client the document belongs to and to the agency. */
    SHARED("Shared with client"),
    /** Agency team only. */
    INTERNAL("Internal")
}

data class StudioDocument(
    val id: String,
    val name: String,
    val category: DocumentCategory,
    val projectId: String?,
    val clientId: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: LocalDateTime,
    val visibility: DocumentVisibility,
    /**
     * Location in remote storage. Always null in the demo build: files are
     * metadata only and nothing is uploaded anywhere.
     */
    val storagePath: String? = null
)
