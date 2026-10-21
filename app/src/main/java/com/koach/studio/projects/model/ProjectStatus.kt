package com.koach.studio.projects.model

enum class ProjectStatus(val displayName: String) {
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    REVISION_REQUESTED("Revision Requested"),
    COMPLETED("Completed")
}
