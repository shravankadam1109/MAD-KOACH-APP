package com.koach.studio.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.koach.studio.app.menu.MenuScreen
import com.koach.studio.bookings.ui.BookingsScreen
import com.koach.studio.clients.ui.ClientDetailScreen
import com.koach.studio.clients.ui.ClientDetailViewModel
import com.koach.studio.clients.ui.ClientFormScreen
import com.koach.studio.clients.ui.ClientFormViewModel
import com.koach.studio.clients.ui.ClientListScreen
import com.koach.studio.clients.ui.ClientListViewModel
import com.koach.studio.bookings.ui.BookingsViewModel
import com.koach.studio.bookings.ui.NewBookingScreen
import com.koach.studio.bookings.ui.NewBookingViewModel
import com.koach.studio.app.menu.PendingFeatureScreen
import com.koach.studio.app.menu.menuEntriesFor
import com.koach.studio.app.navigation.Routes
import com.koach.studio.app.navigation.TopLevelDestination
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.core.model.User
import com.koach.studio.home.AgencyHomeActions
import com.koach.studio.home.AgencyHomeScreen
import com.koach.studio.home.AgencyHomeViewModel
import com.koach.studio.home.ClientHomeActions
import com.koach.studio.home.ClientHomeScreen
import com.koach.studio.home.ClientHomeViewModel
import com.koach.studio.projects.navigation.ProjectsDestinations
import com.koach.studio.portfolio.PortfolioDetailScreen
import com.koach.studio.portfolio.PortfolioDetailViewModel
import com.koach.studio.portfolio.PortfolioViewModel
import com.koach.studio.services.ExploreScreen
import com.koach.studio.tasks.ui.TaskFormScreen
import com.koach.studio.tasks.ui.TaskFormViewModel
import com.koach.studio.tasks.ui.TaskListScreen
import com.koach.studio.tasks.ui.TaskListViewModel
import com.koach.studio.services.ServiceDetailScreen
import com.koach.studio.services.ServiceDetailViewModel
import com.koach.studio.services.ServicesViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.koach.studio.projects.navigation.projectsGraph
import kotlinx.coroutines.launch

/**
 * Signed-in shell: a single NavHost for the whole app plus a role-specific
 * bottom bar that is only shown on top-level destinations.
 */
@Composable
fun KoachShell(user: User, navController: NavHostController = rememberNavController()) {
    val tabs = TopLevelDestination.forRole(user.role)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = tabs.any { tab -> currentDestination?.route == tab.route }

    Scaffold(
        containerColor = KoachTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                KoachBottomBar(
                    tabs = tabs,
                    isSelected = { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true },
                    onSelect = { tab -> navController.navigateToTopLevel(tab.route) }
                )
            }
        }
    ) { innerPadding ->
        KoachNavHost(
            user = user,
            navController = navController,
            startDestination = tabs.first().route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

@Composable
private fun KoachBottomBar(
    tabs: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit
) {
    val c = KoachTheme.colors
    Column(Modifier.background(c.background)) {
        KoachDivider()
        NavigationBar(containerColor = c.background, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
            tabs.forEach { tab ->
                val selected = isSelected(tab)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.brand,
                        selectedTextColor = c.textPrimary,
                        indicatorColor = c.brandContainer,
                        unselectedIconColor = c.textTertiary,
                        unselectedTextColor = c.textTertiary
                    ),
                    modifier = Modifier.testTag("tab_${tab.route}")
                )
            }
        }
    }
}

@Composable
private fun KoachNavHost(
    user: User,
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val auth = appContainer().authRepository
    val scope = rememberCoroutineScope()
    val back: () -> Unit = { navController.popBackStack() }
    val pendingTitles = menuEntriesFor(user).associate { it.route to it.title }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 10 } }
    ) {
        projectsGraph(navController)

        composable(Routes.HOME) {
            ClientHomeScreen(
                viewModel = koachViewModel {
                    ClientHomeViewModel(user, it.projectRepository, it.notificationRepository, it.clock)
                },
                actions = ClientHomeActions(
                    onOpenProject = { id -> navController.navigate(ProjectsDestinations.projectDetail(id)) },
                    onAllProjects = { navController.navigateToTopLevel(ProjectsDestinations.PROJECT_LIST) },
                    onBook = { navController.navigate(Routes.newBooking()) },
                    onMessages = { navController.navigateToTopLevel(Routes.MESSAGES) },
                    onPayments = { navController.navigate(Routes.PAYMENTS) },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
                )
            )
        }

        composable(Routes.EXPLORE) {
            ExploreScreen(
                servicesViewModel = koachViewModel { ServicesViewModel(it.catalogRepository) },
                portfolioViewModel = koachViewModel { PortfolioViewModel(it.catalogRepository) },
                onOpenService = { id -> navController.navigate(Routes.serviceDetail(id)) },
                onOpenPortfolioItem = { id -> navController.navigate(Routes.portfolioDetail(id)) }
            )
        }
        composable(
            Routes.PORTFOLIO_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { entry ->
            val itemId = entry.arguments?.getString("itemId").orEmpty()
            PortfolioDetailScreen(
                viewModel = koachViewModel(key = "portfolio-$itemId") { PortfolioDetailViewModel(itemId, it.catalogRepository) },
                onBack = back,
                onStartSimilar = { serviceId -> navController.navigate(Routes.serviceDetail(serviceId)) }
            )
        }
        composable(
            Routes.SERVICE_DETAIL,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { entry ->
            val serviceId = entry.arguments?.getString("serviceId").orEmpty()
            ServiceDetailScreen(
                viewModel = koachViewModel(key = "service-$serviceId") { ServiceDetailViewModel(serviceId, it.catalogRepository) },
                onBack = back,
                onEnquire = { sid, pid -> navController.navigate(Routes.newBooking(sid, pid)) }
            )
        }
        composable(
            Routes.NEW_BOOKING,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType; defaultValue = "" },
                navArgument("packageId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val serviceId = entry.arguments?.getString("serviceId").orEmpty()
            val packageId = entry.arguments?.getString("packageId").orEmpty()
            NewBookingScreen(
                viewModel = koachViewModel(key = "booking-$serviceId-$packageId") {
                    NewBookingViewModel(user, serviceId, packageId, it.catalogRepository, it.bookingRepository, it.clock)
                },
                onBack = back,
                onViewBookings = {
                    navController.navigate(Routes.BOOKINGS) { popUpTo(Routes.NEW_BOOKING) { inclusive = true } }
                }
            )
        }
        composable(Routes.BOOKINGS) {
            BookingsScreen(
                viewModel = koachViewModel { BookingsViewModel(user, it.bookingRepository, it.catalogRepository) },
                onBack = back,
                onNewBooking = { navController.navigate(Routes.newBooking()) }
            )
        }

        composable(Routes.AGENCY_HOME) {
            AgencyHomeScreen(
                viewModel = koachViewModel {
                    AgencyHomeViewModel(user, it.projectRepository, it.taskRepository, it.bookingRepository, it.notificationRepository, it.clock)
                },
                actions = AgencyHomeActions(
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenProject = { id -> navController.navigate(ProjectsDestinations.projectDetail(id)) },
                    onOpenTask = { id -> navController.navigate(Routes.taskForm(id)) },
                    onAllTasks = { navController.navigateToTopLevel(Routes.TASKS) },
                    onNewTask = { navController.navigate(Routes.taskForm()) },
                    onBookings = { navController.navigate(Routes.BOOKINGS) },
                    onMessages = { navController.navigate(Routes.MESSAGES) }
                )
            )
        }
        composable(Routes.TASKS) {
            TaskListScreen(
                viewModel = koachViewModel { TaskListViewModel(user, it.taskRepository, it.projectRepository, it.people, it.clock) },
                onOpenTask = { id -> navController.navigate(Routes.taskForm(id)) },
                onNewTask = { navController.navigate(Routes.taskForm()) }
            )
        }
        composable(
            Routes.TASK_FORM,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
                navArgument("projectId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty().ifBlank { null }
            val projectId = entry.arguments?.getString("projectId").orEmpty().ifBlank { null }
            TaskFormScreen(
                viewModel = koachViewModel(key = "task-form-$taskId-$projectId") {
                    TaskFormViewModel(taskId, user, it.taskRepository, it.projectRepository, it.people, it.clock, projectId)
                },
                onBack = back
            )
        }
        composable(Routes.CLIENTS) {
            ClientListScreen(
                viewModel = koachViewModel { ClientListViewModel(user, it.clientRepository, it.projectRepository) },
                onOpenClient = { id -> navController.navigate(Routes.clientDetail(id)) },
                onAddClient = { navController.navigate(Routes.clientForm()) }
            )
        }
        composable(
            Routes.CLIENT_DETAIL,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { entry ->
            val clientId = entry.arguments?.getString("clientId").orEmpty()
            ClientDetailScreen(
                viewModel = koachViewModel(key = "client-$clientId") {
                    ClientDetailViewModel(clientId, user, it.clientRepository, it.projectRepository)
                },
                onBack = back,
                onEdit = { id -> navController.navigate(Routes.clientForm(id)) },
                onOpenProject = { id -> navController.navigate(ProjectsDestinations.projectDetail(id)) }
            )
        }
        composable(
            Routes.CLIENT_FORM,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val clientId = entry.arguments?.getString("clientId").orEmpty().ifBlank { null }
            ClientFormScreen(
                viewModel = koachViewModel(key = "client-form-$clientId") { ClientFormViewModel(clientId, it.clientRepository) },
                onBack = back,
                onSaved = { savedId ->
                    navController.popBackStack()
                    if (clientId == null) navController.navigate(Routes.clientDetail(savedId))
                }
            )
        }

        composable(Routes.ACCOUNT) {
            MenuScreen(user, onNavigate = navController::navigate, onSignOut = { scope.launch { auth.signOut() } })
        }
        composable(Routes.MORE) {
            MenuScreen(user, onNavigate = navController::navigate, onSignOut = { scope.launch { auth.signOut() } })
        }

        // Destinations below are filled in by their feature modules.
        listOf(
            Routes.MESSAGES to "Messages",
            Routes.NOTIFICATIONS to "Notifications",
            Routes.DOCUMENTS to "Documents",
            Routes.PAYMENTS to "Payments",
            Routes.FEEDBACK to "Feedback",
            Routes.PROFILE to "Profile"
        ).forEach { (route, title) ->
            composable(route) { PendingFeatureScreen(pendingTitles[route] ?: title, onBack = back) }
        }
    }
}

/** Switch tabs without growing the back stack, restoring each tab's state. */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
