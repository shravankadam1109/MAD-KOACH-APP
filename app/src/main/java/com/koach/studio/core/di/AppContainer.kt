package com.koach.studio.core.di

import com.koach.studio.bookings.data.BookingRepository
import com.koach.studio.bookings.data.InMemoryBookingRepository
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.clients.data.ClientRepository
import com.koach.studio.clients.data.InMemoryClientRepository
import com.koach.studio.catalog.data.InMemoryCatalogRepository
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.InMemorySessionStore
import com.koach.studio.core.auth.SessionStore
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.User
import com.koach.studio.documents.data.DemoDocumentStorage
import com.koach.studio.documents.data.DocumentRepository
import com.koach.studio.documents.data.DocumentStorage
import com.koach.studio.documents.data.InMemoryDocumentRepository
import com.koach.studio.feedback.FeedbackRepository
import com.koach.studio.feedback.InMemoryFeedbackRepository
import com.koach.studio.messaging.data.InMemoryMessageRepository
import com.koach.studio.messaging.data.MessageRepository
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.payments.InMemoryPaymentRepository
import com.koach.studio.payments.PaymentRepository
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.tasks.data.InMemoryTaskRepository
import com.koach.studio.tasks.data.TaskRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import kotlinx.coroutines.flow.first
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
    /** People directory (staff and client contacts) used to show names and avatars. */
    val people: List<User>
    val projectRepository: ProjectRepository
    val notificationRepository: NotificationRepository
    val catalogRepository: CatalogRepository
    val bookingRepository: BookingRepository
    val clientRepository: ClientRepository
    val taskRepository: TaskRepository
    val documentRepository: DocumentRepository
    val documentStorage: DocumentStorage
    val messageRepository: MessageRepository
    val paymentRepository: PaymentRepository
    val feedbackRepository: FeedbackRepository
}

/** Demo wiring: every repository is in-memory and resets when the process dies. */
class DemoAppContainer(
    override val clock: Clock = Clock.systemDefaultZone(),
    /** In-memory by default (tests); the app passes a persistent store. */
    private val sessionStore: SessionStore = InMemorySessionStore()
) : AppContainer {

    private val demoData by lazy { DemoData(LocalDate.now(clock)) }

    override val authRepository: AuthRepository by lazy { DemoAuthRepository(demoData.users, sessionStore) }

    override val people: List<User> by lazy { demoData.users }

    override val accessControl: AccessControl by lazy { AccessControl { authRepository.currentUser.value } }

    /** Unfiltered project store; everything outside this container goes through [projectRepository]. */
    private val projectStore by lazy { InMemoryProjectRepository(clock = clock) }

    override val projectRepository: ProjectRepository by lazy {
        SecuredProjectRepository(projectStore, authRepository.currentUser, accessControl)
    }

    override val notificationRepository: NotificationRepository by lazy {
        InMemoryNotificationRepository(demoData.notifications, authRepository.currentUser, accessControl)
    }

    override val catalogRepository: CatalogRepository by lazy {
        InMemoryCatalogRepository(demoData.services, demoData.portfolio)
    }

    override val bookingRepository: BookingRepository by lazy {
        InMemoryBookingRepository(
            demoData.bookings, demoData.users, authRepository.currentUser, accessControl, notificationRepository, clock
        )
    }

    override val clientRepository: ClientRepository by lazy {
        InMemoryClientRepository(demoData.clients, authRepository.currentUser, accessControl, clock) { clientId ->
            projectStore.getProjects().first().count { it.clientId == clientId }
        }
    }

    override val taskRepository: TaskRepository by lazy {
        InMemoryTaskRepository(demoData.tasks, authRepository.currentUser, accessControl, clock)
    }

    override val documentRepository: DocumentRepository by lazy {
        InMemoryDocumentRepository(demoData.documents, authRepository.currentUser, accessControl, clock)
    }

    override val documentStorage: DocumentStorage = DemoDocumentStorage

    override val messageRepository: MessageRepository by lazy {
        InMemoryMessageRepository(
            demoData.threads, demoData.messages, authRepository.currentUser, accessControl, notificationRepository, clock
        )
    }

    override val paymentRepository: PaymentRepository by lazy {
        InMemoryPaymentRepository(demoData.payments, demoData.users, authRepository.currentUser, accessControl, notificationRepository, clock)
    }

    override val feedbackRepository: FeedbackRepository by lazy {
        InMemoryFeedbackRepository(demoData.feedback, demoData.users, authRepository.currentUser, accessControl, notificationRepository, clock)
    }
}
