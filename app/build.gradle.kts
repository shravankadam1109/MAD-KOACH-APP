plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase is optional. google-services.json is never committed; without it the
// app builds in demo mode, so teammates and CI don't need Firebase access.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}
// Real Firebase backend is opt-in while repositories are being migrated:
//   ./gradlew installDebug -Pkoach.backend=firebase
val useFirebaseBackend = hasFirebaseConfig && (findProperty("koach.backend") as String?) == "firebase"
// Point a debug build at the local Firebase emulators instead of the real project:
//   ./gradlew installDebug -Pkoach.backend=firebase -Pkoach.firebaseEmulator=true
val useFirebaseEmulator = (findProperty("koach.firebaseEmulator") as String?) == "true"

android {
    namespace = "com.koach.studio"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.koach.studio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "FIREBASE_BACKEND", useFirebaseBackend.toString())
        buildConfigField("boolean", "FIREBASE_EMULATOR", "false")
    }

    buildTypes {
        debug {
            // The emulator switch is debug-only; release builds always use the real project.
            buildConfigField("boolean", "FIREBASE_EMULATOR", useFirebaseEmulator.toString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // System animations make Compose UI tests flaky on emulators.
        animationsDisabled = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    // Google sign-in through Android Credential Manager, exchanged for a Firebase credential.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
