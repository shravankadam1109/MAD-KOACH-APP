package com.koach.studio.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.koach.studio.bookings.data.BookingRepository
import com.koach.studio.bookings.data.FirestoreBookingRepository
import com.koach.studio.clients.data.ClientRepository
import com.koach.studio.clients.data.FirestoreClientRepository
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.di.DemoAppContainer
import com.koach.studio.core.settings.InMemoryThemePreferencesStore
import com.koach.studio.core.settings.ThemePreferencesStore
import com.koach.studio.documents.data.DocumentRepository
import com.koach.studio.documents.data.FirestoreDocumentRepository
import com.koach.studio.feedback.FeedbackRepository
import com.koach.studio.feedback.FirestoreFeedbackRepository
import com.koach.studio.messaging.data.FirestoreMessageRepository
import com.koach.studio.messaging.data.MessageRepository
import com.koach.studio.notifications.data.FirestoreNotificationRepository
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.payments.FirestorePaymentRepository
import com.koach.studio.payments.PaymentRepository
import com.koach.studio.projects.data.FirestoreProjectRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.tasks.data.FirestoreTaskRepository
import com.koach.studio.tasks.data.TaskRepository
import java.time.Clock

/**
 * Firebase wiring: authentication, profiles and every record live in
 * Firestore, guarded by the security rules in `firebase/`. The service
 * catalogue and portfolio are still bundled with the app, and file transfers
 * and payments are still simulated, which the app says via [dataNotice].
 */
class FirebaseAppContainer(
    clock: Clock = Clock.systemDefaultZone(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    storage: FirebaseStorage = FirebaseStorage.getInstance(),
    /** Debug only: connect to the local Firebase emulators on this host (10.0.2.2 from an Android emulator). */
    emulatorHost: String? = null,
    themePreferencesStore: ThemePreferencesStore = InMemoryThemePreferencesStore(),
    private val pendingSignUps: PendingSignUpStore = InMemoryPendingSignUpStore()
) : DemoAppContainer(clock = clock, themePreferencesStore = themePreferencesStore) {

    init {
        if (emulatorHost != null) {
            auth.useEmulator(emulatorHost, 9099)
            firestore.useEmulator(emulatorHost, 8080)
            storage.useEmulator(emulatorHost, 9199)
        }
    }

    override val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth, firestore, pendingSignUps) }

    private val signedInUser get() = authRepository.currentUser

    override val projectRepository: ProjectRepository by lazy {
        SecuredProjectRepository(FirestoreProjectRepository(firestore, signedInUser, clock), signedInUser, accessControl)
    }

    override val notificationRepository: NotificationRepository by lazy {
        FirestoreNotificationRepository(firestore, signedInUser)
    }

    override val bookingRepository: BookingRepository by lazy {
        FirestoreBookingRepository(firestore, signedInUser, accessControl, notificationRepository, clock)
    }

    override val clientRepository: ClientRepository by lazy {
        FirestoreClientRepository(firestore, signedInUser, accessControl, clock)
    }

    override val taskRepository: TaskRepository by lazy {
        FirestoreTaskRepository(firestore, signedInUser, accessControl, clock)
    }

    override val documentRepository: DocumentRepository by lazy {
        FirestoreDocumentRepository(firestore, signedInUser, accessControl, clock)
    }

    override val messageRepository: MessageRepository by lazy {
        FirestoreMessageRepository(firestore, signedInUser, accessControl, notificationRepository, clock)
    }

    override val paymentRepository: PaymentRepository by lazy {
        FirestorePaymentRepository(firestore, signedInUser, accessControl)
    }

    override val feedbackRepository: FeedbackRepository by lazy {
        FirestoreFeedbackRepository(firestore, signedInUser, accessControl, notificationRepository, clock)
    }

    override val dataNotice: String =
        "Live Firebase data. Services and portfolio are bundled with the app; file transfers and payments are still simulated."
}
