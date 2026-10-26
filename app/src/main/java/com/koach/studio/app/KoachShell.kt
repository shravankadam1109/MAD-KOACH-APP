package com.koach.studio.app

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateDpAsState
import com.koach.studio.core.designsystem.components.KoachBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.pressScale
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.koach.studio.app.menu.MenuScreen
import com.koach.studio.app.navigation.Routes
import com.koach.studio.app.navigation.TopLevelDestination
import com.koach.studio.bookings.ui.BookingDetailScreen
import com.koach.studio.bookings.ui.BookingDetailViewModel
import com.koach.studio.bookings.ui.BookingsScreen
import com.koach.studio.bookings.ui.BookingsViewModel
import com.koach.studio.bookings.ui.NewBookingScreen
import com.koach.studio.bookings.ui.NewBookingViewModel
import com.koach.studio.clients.ui.ClientDetailScreen
import com.koach.studio.clients.ui.ClientDetailViewModel
import com.koach.studio.clients.ui.ClientFormScreen
import com.koach.studio.clients.ui.ClientFormViewModel
import com.koach.studio.clients.ui.ClientListScreen
import com.koach.studio.clients.ui.ClientListViewModel
import com.koach.studio.contact.ContactScreen
import com.koach.studio.contact.ContactViewModel
import com.koach.studio.core.designsystem.components.CountBadge
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.User
import com.koach.studio.documents.ui.AddDocumentScreen
import com.koach.studio.documents.ui.AddDocumentViewModel
import com.koach.studio.documents.ui.DocumentsScreen
import com.koach.studio.documents.ui.DocumentsViewModel
import com.koach.studio.feedback.FeedbackListScreen
import com.koach.studio.feedback.FeedbackListViewModel
import com.koach.studio.feedback.NewFeedbackScreen
import com.koach.studio.feedback.NewFeedbackViewModel
import com.koach.studio.home.AgencyHomeActions
import com.koach.studio.home.AgencyHomeScreen
import com.koach.studio.home.AgencyHomeViewModel
import com.koach.studio.home.ClientHomeActions
import com.koach.studio.home.ClientHomeScreen
import com.koach.studio.home.ClientHomeViewModel
import com.koach.studio.messaging.ui.ConversationScreen
import com.koach.studio.messaging.ui.ConversationViewModel
import com.koach.studio.messaging.ui.ThreadListScreen
import com.koach.studio.messaging.ui.ThreadListViewModel
import com.koach.studio.notifications.ui.NotificationsScreen
import com.koach.studio.notifications.ui.NotificationsViewModel
import com.koach.studio.payments.PaymentsScreen
import com.koach.studio.payments.PaymentsViewModel
import com.koach.studio.portfolio.PortfolioDetailScreen
import com.koach.studio.portfolio.PortfolioDetailViewModel
import com.koach.studio.portfolio.PortfolioViewModel
import com.koach.studio.profile.ProfileScreen
import com.koach.studio.projects.navigation.ProjectsDestinations
import com.koach.studio.projects.navigation.projectsGraph
import com.koach.studio.services.ExploreScreen
import com.koach.studio.services.ServiceDetailScreen
import com.koach.studio.services.ServiceDetailViewModel
import com.koach.studio.services.ServicesViewModel
import com.koach.studio.settings.SettingsScreen
import com.koach.studio.settings.SettingsViewModel
import com.koach.studio.tasks.ui.TaskDetailScreen
import com.koach.studio.tasks.ui.TaskDetailViewModel
import com.koach.studio.tasks.ui.TaskFormScreen
import com.koach.studio.tasks.ui.TaskFormViewModel
import com.koach.studio.tasks.ui.TaskListScreen
import com.koach.studio.tasks.ui.TaskListViewModel
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
    val messageRepository = appContainer().messageRepository
    val unreadFlow = remember(messageRepository) { messageRepository.observeUnreadCount() }
    val unreadMessages by unreadFlow.collectAsState(initial = 0)

    KoachBackground(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                KoachBottomBar(
                    tabs = tabs,
                    badges = mapOf(TopLevelDestination.MESSAGES to unreadMessages),
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
}

/**
 * Light docked navigation: content fades into the bar, a hairline marks its
 * edge, and every tab shows an icon and a small label. The active tab is
 * picked out in green, with a short indicator that slides between tabs.
 * Every tab keeps its label for accessibility services.
 */
@Composable
private fun KoachBottomBar(
    tabs: List<TopLevelDestination>,
    badges: Map<TopLevelDestination, Int>,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit
) {
    val c = KoachTheme.colors
    val selectedIndex = tabs.indexOfFirst(isSelected).coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, c.background.copy(alpha = 0.94f), c.background), endY = 90f))
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(c.glassBorder))
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                val tabWidth = maxWidth / tabs.size
                val indicatorX by animateDpAsState(tabWidth * selectedIndex + (tabWidth - 22.dp) / 2, KoachMotion.press(), label = "tab-indicator")
                Box(
                    Modifier
                        .offset(x = indicatorX)
                        .size(width = 22.dp, height = 3.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(c.brand)
                )
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    tabs.forEach { tab ->
                        val selected = isSelected(tab)
                        val count = badges[tab] ?: 0
                        val tint by animateColorAsState(if (selected) c.brandBright else c.textTertiary, KoachMotion.enter(KoachMotion.Medium), label = "tab-tint")
                        val labelTint by animateColorAsState(if (selected) c.textPrimary else c.textTertiary, KoachMotion.enter(KoachMotion.Medium), label = "tab-label")
                        val tabInteraction = remember { MutableInteractionSource() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .pressScale(tabInteraction, 0.92f)
                                .selectable(selected = selected, interactionSource = tabInteraction, indication = null, role = Role.Tab, onClick = { onSelect(tab) })
                                .semantics(mergeDescendants = true) {
                                    contentDescription = if (count > 0) "${tab.label}, $count unread" else tab.label
                                }
                                .testTag("tab_${tab.route}")
                        ) {
                            BadgedBox(badge = { if (count > 0) CountBadge(count) }) {
                                Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp), color = labelTint, maxLines = 1)
                        }
                    }
                }
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
    val container = appContainer()
    val auth = container.authRepository
    val scope = rememberCoroutineScope()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        // One choreography everywhere: the new screen drifts in and settles from
        // slightly smaller while the old one recedes; going back reverses it.
        enterTransition = {
            fadeIn(KoachMotion.enter(KoachMotion.Medium, delayMillis = 40)) +
                scaleIn(KoachMotion.enter(), initialScale = 0.96f) +
                slideInHorizontally(KoachMotion.enter()) { it / 14 }
        },
        exitTransition = { fadeOut(KoachMotion.exit()) + scaleOut(KoachMotion.exit(KoachMotion.Medium), targetScale = 0.98f) },
        popEnterTransition = {
            fadeIn(KoachMotion.enter(KoachMotion.Medium, delayMillis = 40)) + scaleIn(KoachMotion.enter(), initialScale = 0.98f)
        },
        popExitTransition = {
            fadeOut(KoachMotion.exit()) +
                scaleOut(KoachMotion.exit(KoachMotion.Medium), targetScale = 0.96f) +
                slideOutHorizontally(KoachMotion.exit(KoachMotion.Medium)) { it / 14 }
        }
    ) {
        projectsGraph(navController, onOpenDocuments = { id -> navController.navigate(Routes.documents(id)) })

        composable(Routes.HOME) {
            ClientHomeScreen(
                viewModel = koachViewModel {
                    ClientHomeViewModel(user, it.projectRepository, it.notificationRepository, it.clock, it.catalogRepository)
                },
                actions = ClientHomeActions(
                    onOpenProject = { id -> navController.navigate(ProjectsDestinations.projectDetail(id)) },
                    onAllProjects = { navController.navigateToTopLevel(ProjectsDestinations.PROJECT_LIST) },
                    onBook = { navController.navigate(Routes.newBooking()) },
                    onMessages = { navController.navigateToTopLevel(Routes.MESSAGES) },
                    onPayments = { navController.navigate(Routes.PAYMENTS) },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onContact = { navController.navigate(Routes.CONTACT) },
                    onOpenService = { id -> navController.navigate(Routes.serviceDetail(id)) },
                    onOpenPortfolioItem = { id -> navController.navigate(Routes.portfolioDetail(id)) }
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
                onStartSimilar = { serviceId -> navController.navigate(Routes.serviceDetail(serviceId)) },
                onBookSimilar = { serviceId -> navController.navigate(Routes.newBooking(serviceId)) }
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
                onNewBooking = { navController.navigate(Routes.newBooking()) },
                onOpenBooking = { id -> navController.navigate(Routes.bookingDetail(id)) }
            )
        }
        composable(
            Routes.BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { entry ->
            val bookingId = entry.arguments?.getString("bookingId").orEmpty()
            BookingDetailScreen(
                viewModel = koachViewModel(key = "booking-detail-$bookingId") {
                    BookingDetailViewModel(bookingId, user, it.bookingRepository, it.catalogRepository)
                },
                onBack = back
            )
        }

        composable(Routes.AGENCY_HOME) {
            AgencyHomeScreen(
                viewModel = koachViewModel {
                    AgencyHomeViewModel(
                        user = user,
                        projects = it.projectRepository,
                        tasks = it.taskRepository,
                        bookings = it.bookingRepository,
                        notifications = it.notificationRepository,
                        clock = it.clock,
                        people = it.people,
                        isDemo = it.authRepository.isDemo
                    )
                },
                actions = AgencyHomeActions(
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenProject = { id -> navController.navigate(ProjectsDestinations.projectDetail(id)) },
                    onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                    onAllTasks = { navController.navigateToTopLevel(Routes.TASKS) },
                    onNewTask = { navController.navigate(Routes.taskForm()) },
                    onBookings = { navController.navigate(Routes.BOOKINGS) },
                    onMessages = { navController.navigate(Routes.MESSAGES) },
                    onAllProjects = { navController.navigateToTopLevel(ProjectsDestinations.PROJECT_LIST) },
                    onClients = { navController.navigateToTopLevel(Routes.CLIENTS) },
                    onDocuments = { navController.navigate(Routes.documents()) }
                )
            )
        }
        composable(Routes.TASKS) {
            TaskListScreen(
                viewModel = koachViewModel { TaskListViewModel(user, it.taskRepository, it.projectRepository, it.people, it.clock) },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                onNewTask = { navController.navigate(Routes.taskForm()) }
            )
        }
        composable(
            Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            TaskDetailScreen(
                viewModel = koachViewModel(key = "task-detail-$taskId") {
                    TaskDetailViewModel(taskId, user, it.taskRepository, it.projectRepository, it.people, it.clock)
                },
                onBack = back,
                onEdit = { navController.navigate(Routes.taskForm(taskId)) }
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
        composable(Routes.DOCUMENTS) { DocumentsRoute(user, null, navController) }
        composable(
            Routes.PROJECT_DOCUMENTS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            DocumentsRoute(user, entry.arguments?.getString("projectId").orEmpty().ifBlank { null }, navController)
        }
        composable(
            Routes.ADD_DOCUMENT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId").orEmpty().ifBlank { null }
            AddDocumentScreen(
                viewModel = koachViewModel(key = "add-doc-$projectId") {
                    AddDocumentViewModel(user, it.documentRepository, it.projectRepository, projectId)
                },
                onBack = back
            )
        }
        composable(Routes.MESSAGES) {
            ThreadListScreen(
                viewModel = koachViewModel { ThreadListViewModel(it.messageRepository, it.people, it.clock, user) },
                onOpenThread = { id -> navController.navigate(Routes.conversation(id)) },
                onBack = null
            )
        }
        composable(
            Routes.CONVERSATION,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { entry ->
            val threadId = entry.arguments?.getString("threadId").orEmpty()
            ConversationScreen(
                viewModel = koachViewModel(key = "thread-$threadId") {
                    ConversationViewModel(threadId, user, it.messageRepository, it.people)
                },
                onBack = back
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = koachViewModel { NotificationsViewModel(it.notificationRepository, it.clock) },
                onBack = back,
                onOpenTarget = { notification -> navController.openNotificationTarget(notification) }
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
            MenuScreen(
                user = user,
                onNavigate = navController::navigate,
                onSignOut = { scope.launch { auth.signOut() } },
                themePreferencesStore = container.themePreferencesStore
            )
        }
        composable(Routes.MORE) {
            MenuScreen(
                user = user,
                onNavigate = navController::navigate,
                onSignOut = { scope.launch { auth.signOut() } },
                themePreferencesStore = container.themePreferencesStore
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                user = user,
                isDemo = auth.isDemo,
                viewModel = koachViewModel { SettingsViewModel(it.themePreferencesStore) },
                onBack = back,
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenContact = { navController.navigate(Routes.CONTACT) },
                onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                onSignOut = { scope.launch { auth.signOut() } }
            )
        }

        composable(Routes.PAYMENTS) {
            PaymentsScreen(
                viewModel = koachViewModel { PaymentsViewModel(user, it.paymentRepository, it.clientRepository, it.clock) },
                onBack = back
            )
        }
        composable(Routes.FEEDBACK) {
            FeedbackListScreen(
                viewModel = koachViewModel { FeedbackListViewModel(user, it.feedbackRepository, it.people) },
                onBack = back,
                onNewFeedback = { navController.navigate(Routes.NEW_FEEDBACK) }
            )
        }
        composable(Routes.NEW_FEEDBACK) {
            NewFeedbackScreen(
                viewModel = koachViewModel { NewFeedbackViewModel(it.feedbackRepository, it.projectRepository) },
                onBack = back
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                user = user,
                clients = container.clientRepository,
                isDemo = auth.isDemo,
                onBack = back,
                onSignOut = { scope.launch { auth.signOut() } }
            )
        }
        composable(Routes.CONTACT) {
            ContactScreen(
                viewModel = koachViewModel { ContactViewModel() },
                onBack = back,
                onBookService = { navController.navigate(Routes.newBooking()) },
                onSendMessage = { navController.navigateToTopLevel(Routes.MESSAGES) },
                onSendFeedback = { navController.navigate(Routes.NEW_FEEDBACK) }
            )
        }
    }
}

@Composable
private fun DocumentsRoute(user: User, projectId: String?, navController: NavHostController) {
    DocumentsScreen(
        viewModel = koachViewModel(key = "documents-$projectId") {
            DocumentsViewModel(user, it.documentRepository, it.projectRepository, it.people, it.documentStorage, projectId)
        },
        onBack = { navController.popBackStack() },
        onAddDocument = { navController.navigate(Routes.addDocument(projectId)) }
    )
}

/** Opens whatever a notification is about, based on its target id prefix. */
private fun NavHostController.openNotificationTarget(notification: AppNotification) {
    val target = notification.targetId ?: return
    when {
        target.startsWith("proj-") -> navigate(ProjectsDestinations.projectDetail(target))
        target.startsWith("th-") -> navigate(Routes.conversation(target))
        target.startsWith("t-") -> navigate(Routes.taskForm(target))
        target.startsWith("bk-") -> navigate(Routes.bookingDetail(target))
        target.startsWith("fb-") -> navigate(Routes.FEEDBACK)
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
