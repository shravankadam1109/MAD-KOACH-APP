package com.koach.studio.core.di

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.data.DemoData
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Single place where the app's dependencies are created. ViewModels receive
 * what they need from here through constructor parameters, so tests can pass
 * fakes directly and the in-memory repositories can later be swapped for
 * Firebase-backed ones without touching any screen.
 */
interface AppContainer {
    val clock: Clock
    val authRepository: AuthRepository
    val accessControl: AccessControl
    val projectRepository: ProjectRepository
    val notificationRepository: NotificationRepository
}

/** Demo wiring: every repository is in-memory and resets when the process dies. */
class DemoAppContainer(
    override val clock: Clock = Clock.systemDefaultZone()
) : AppContainer {

    private val demoData by lazy { DemoData(LocalDate.now(clock)) }

    override val authRepository: AuthRepository by lazy { DemoAuthRepository(demoData.users) }

    override val accessControl: AccessControl by lazy { AccessControl { authRepository.currentUser.value } }

    override val projectRepository: ProjectRepository by lazy {
        SecuredProjectRepository(InMemoryProjectRepository(), authRepository.currentUser, accessControl)
    }

    override val notificationRepository: NotificationRepository by lazy {
        InMemoryNotificationRepository(demoData.notifications, authRepository.currentUser, accessControl)
    }
}
