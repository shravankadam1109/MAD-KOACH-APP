package com.koach.studio.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.koach.studio.core.model.Role
import com.koach.studio.projects.navigation.ProjectsDestinations

/** Every route string in the app shell. Project routes live in [ProjectsDestinations]. */
object Routes {
    const val HOME = "home"
    const val AGENCY_HOME = "agency_home"
    const val EXPLORE = "explore"
    const val MESSAGES = "messages"
    const val ACCOUNT = "account"
    const val MORE = "more"
    const val CLIENTS = "clients"
    const val TASKS = "tasks"
    const val DOCUMENTS = "documents"
    const val BOOKINGS = "bookings"
    const val PAYMENTS = "payments"
    const val FEEDBACK = "feedback"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTACT = "contact"
    const val SERVICE_DETAIL = "services/{serviceId}"
    const val NEW_BOOKING = "bookings/new?serviceId={serviceId}&packageId={packageId}"

    const val PORTFOLIO_DETAIL = "portfolio/{itemId}"

    const val CLIENT_DETAIL = "clients/{clientId}"
    const val CLIENT_FORM = "client-form?clientId={clientId}"

    const val NEW_FEEDBACK = "feedback/new"
    const val BOOKING_DETAIL = "booking/{bookingId}"

    fun bookingDetail(bookingId: String) = "booking/$bookingId"

    const val CONVERSATION = "messages/{threadId}"

    fun conversation(threadId: String) = "messages/$threadId"

    const val PROJECT_DOCUMENTS = "documents?projectId={projectId}"
    const val ADD_DOCUMENT = "documents/new?projectId={projectId}"

    fun documents(projectId: String? = null) = "documents?projectId=${projectId.orEmpty()}"
    fun addDocument(projectId: String? = null) = "documents/new?projectId=${projectId.orEmpty()}"

    const val TASK_FORM = "task-form?taskId={taskId}&projectId={projectId}"

    fun taskForm(taskId: String? = null, projectId: String? = null) =
        "task-form?taskId=${taskId.orEmpty()}&projectId=${projectId.orEmpty()}"
    fun clientDetail(clientId: String) = "clients/$clientId"
    fun clientForm(clientId: String? = null) = "client-form?clientId=${clientId.orEmpty()}"
    fun serviceDetail(serviceId: String) = "services/$serviceId"
    fun portfolioDetail(itemId: String) = "portfolio/$itemId"
    fun newBooking(serviceId: String? = null, packageId: String? = null) =
        "bookings/new?serviceId=${serviceId.orEmpty()}&packageId=${packageId.orEmpty()}"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    AGENCY_HOME(Routes.AGENCY_HOME, "Studio", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    PROJECTS(ProjectsDestinations.PROJECT_LIST, "Projects", Icons.Filled.Movie, Icons.Outlined.Movie),
    EXPLORE(Routes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    MESSAGES(Routes.MESSAGES, "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    ACCOUNT(Routes.ACCOUNT, "Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
    CLIENTS(Routes.CLIENTS, "Clients", Icons.Filled.Groups, Icons.Outlined.Groups),
    TASKS(Routes.TASKS, "Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    MORE(Routes.MORE, "More", Icons.Filled.Widgets, Icons.Outlined.Widgets);

    companion object {
        /** Bottom-bar tabs for a role. Clients get a service-led app; the agency a work-led one. */
        fun forRole(role: Role): List<TopLevelDestination> = when (role) {
            Role.CLIENT -> listOf(HOME, PROJECTS, EXPLORE, MESSAGES, ACCOUNT)
            Role.TEAM, Role.ADMIN -> listOf(AGENCY_HOME, PROJECTS, CLIENTS, TASKS, MORE)
        }
    }
}
