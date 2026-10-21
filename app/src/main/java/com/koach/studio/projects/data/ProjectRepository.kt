package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    fun getProjectById(projectId: String): Flow<Project?>
    suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit>
    suspend fun requestRevision(projectId: String, reason: String): Result<Unit>
    suspend fun startDeliverableDownload(projectId: String, deliverableId: String, simulateFailure: Boolean = false)
}
