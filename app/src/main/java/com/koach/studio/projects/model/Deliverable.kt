package com.koach.studio.projects.model

data class Deliverable(
    val id: String,
    val projectId: String,
    val name: String,
    val type: String,
    val size: String,
    val version: String,
    val notes: String = "",
    val status: DeliverableStatus = DeliverableStatus.DRAFT,
    val downloadState: DownloadState = DownloadState.IDLE,
    val downloadProgress: Float = 0f
)
