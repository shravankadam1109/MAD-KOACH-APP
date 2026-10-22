package com.koach.studio.projects.model

import java.time.LocalDate

data class Project(
    val id: String,
    val title: String,
    val clientName: String,
    val serviceType: ServiceType,
    val status: ProjectStatus,
    val dueDate: LocalDate,
    val description: String,
    val deliverables: List<Deliverable> = emptyList(),
    /** Owning client record; empty for projects created before clients existed. */
    val clientId: String = "",
    val milestones: List<Milestone> = emptyList(),
    val revisions: List<Revision> = emptyList(),
    /** Agency users working on the project. */
    val teamIds: List<String> = emptyList()
) {
    /** Share of milestones completed, 0..1. Completed projects are always 1. */
    val progress: Float
        get() = when {
            status == ProjectStatus.COMPLETED -> 1f
            milestones.isEmpty() -> 0f
            else -> milestones.count { it.completed }.toFloat() / milestones.size
        }
}
