package com.koach.studio.projects.model

data class Project(
    val id: String,
    val title: String,
    val clientName: String,
    val serviceType: ServiceType,
    val status: ProjectStatus,
    val dueDate: String,
    val description: String,
    val deliverables: List<Deliverable> = emptyList()
)
