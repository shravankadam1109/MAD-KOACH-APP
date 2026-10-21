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
import com.koach.studio.app.menu.PendingFeatureScreen
import com.koach.studio.app.menu.menuEntriesFor
import com.koach.studio.app.navigation.Routes
import com.koach.studio.app.navigation.TopLevelDestination
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.model.User
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

        composable(Routes.ACCOUNT) {
            MenuScreen(user, onNavigate = navController::navigate, onSignOut = { scope.launch { auth.signOut() } })
        }
        composable(Routes.MORE) {
            MenuScreen(user, onNavigate = navController::navigate, onSignOut = { scope.launch { auth.signOut() } })
        }

        // Destinations below are filled in by their feature modules.
        listOf(
            Routes.HOME to "Home",
            Routes.AGENCY_HOME to "Studio",
            Routes.EXPLORE to "Explore",
            Routes.CLIENTS to "Clients",
            Routes.TASKS to "Tasks",
            Routes.MESSAGES to "Messages",
            Routes.NOTIFICATIONS to "Notifications",
            Routes.BOOKINGS to "Bookings",
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
