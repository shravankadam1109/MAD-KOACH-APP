package com.koach.studio.projects.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.koach.studio.projects.ui.detail.ProjectDetailScreen
import com.koach.studio.projects.ui.edit.EditDeliverableScreen
import com.koach.studio.projects.ui.list.ProjectListScreen
import com.koach.studio.projects.ui.revision.RevisionRequestScreen

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
        projectsGraph(navController)
    }
}

/** Project destinations, added to whichever NavHost hosts them (the app shell or [ProjectsNavHost]). */
fun NavGraphBuilder.projectsGraph(navController: NavHostController) {
    composable(ProjectsDestinations.PROJECT_LIST) {
        ProjectListScreen(
            onProjectClick = { projectId ->
                navController.navigate(ProjectsDestinations.projectDetail(projectId))
            }
        )
    }

    composable(
        route = ProjectsDestinations.PROJECT_DETAIL,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
        ProjectDetailScreen(
            projectId = projectId,
            onNavigateBack = { navController.popBackStack() },
            onDeliverableClick = { deliverableId ->
                navController.navigate(ProjectsDestinations.editDeliverable(projectId, deliverableId))
            },
            onRequestRevisionClick = {
                navController.navigate(ProjectsDestinations.revisionRequest(projectId))
            }
        )
    }

    composable(
        route = ProjectsDestinations.EDIT_DELIVERABLE,
        arguments = listOf(
            navArgument("projectId") { type = NavType.StringType },
            navArgument("deliverableId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
        val deliverableId = backStackEntry.arguments?.getString("deliverableId").orEmpty()
        EditDeliverableScreen(
            projectId = projectId,
            deliverableId = deliverableId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = ProjectsDestinations.REVISION_REQUEST,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
        RevisionRequestScreen(
            projectId = projectId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
