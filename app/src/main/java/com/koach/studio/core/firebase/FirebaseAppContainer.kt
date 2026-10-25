package com.koach.studio.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.di.DemoAppContainer
import java.time.Clock

/**
 * Firebase wiring. Authentication and user profiles are real; the remaining
 * repositories are still the in-memory demo ones until they are migrated to
 * Firestore, and the app says so on screen via [dataNotice].
 */
class FirebaseAppContainer(
    clock: Clock = Clock.systemDefaultZone(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    storage: FirebaseStorage = FirebaseStorage.getInstance(),
    /** Debug only: connect to the local Firebase emulators on this host (10.0.2.2 from an Android emulator). */
    emulatorHost: String? = null
) : DemoAppContainer(clock) {

    init {
        if (emulatorHost != null) {
            auth.useEmulator(emulatorHost, 9099)
            firestore.useEmulator(emulatorHost, 8080)
            storage.useEmulator(emulatorHost, 9199)
        }
    }

    override val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth, firestore) }

    override val dataNotice: String =
        "Signed in with Firebase. Projects, bookings and other records are still sample data until they move to Firestore."
}
