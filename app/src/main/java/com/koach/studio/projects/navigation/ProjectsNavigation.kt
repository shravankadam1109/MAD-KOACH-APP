package com.koach.studio.projects.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.koach.studio.projects.ui.list.ProjectListScreen

object ProjectsDestinations {
    const val PROJECT_LIST = "projects"
    const val PROJECT_DETAIL = "projects/{projectId}"
    const val EDIT_DELIVERABLE = "projects/{projectId}/deliverables/{deliverableId}/edit"
    const val REVISION_REQUEST = "projects/{projectId}/revision-request"

    fun projectDetail(projectId: String): String = "projects/$projectId"
    fun editDeliverable(projectId: String, deliverableId: String): String =
        "projects/$projectId/deliverables/$deliverableId/edit"
    fun revisionRequest(projectId: String): String = "projects/$projectId/revision-request"
}

@Composable
fun ProjectsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ProjectsDestinations.PROJECT_LIST
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ProjectsDestinations.PROJECT_LIST) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(ProjectsDestinations.projectDetail(projectId))
                }
            )
        }
    }
}
