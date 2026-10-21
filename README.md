# KOACH Studio

A premium Android app for **KOACH Studio**, a creative agency offering filming, video editing, content creation, social media, branding, motion graphics and VFX. Clients book services, follow their projects, review deliverables and talk to their crew. The agency team runs clients, projects, tasks, documents and bookings from the same app.

Built with Kotlin, Jetpack Compose and Material 3. Dark UI with KOACH neon green (`#41BA00`).

> **Demo build.** Sign-in, data, downloads, uploads and payments are simulated in memory. Nothing is sent to a server and all data resets when the app closes. See [Backend status](#backend-status-and-limitations).

---

## Features

### For clients

| Area | What you can do |
|---|---|
| Home | Greeting, featured project with progress and next milestone, quick actions, deliverables awaiting review, upcoming milestones |
| Projects | Your projects only, with search, service filters, progress and due dates |
| Project detail | Milestones, crew, description, deliverables, revision history, project documents |
| Deliverables | Simulated download, **Approve**, or **Request changes** (a reason is required and kept in history) |
| Explore | Services catalogue (7 categories, packages in INR) and a portfolio of clearly labelled **sample** work |
| Booking | Service and package picker, date picker (2+ days' notice), budget, brief, validation, confirmation, booking history |
| Messages | One conversation per project, unread badges, live updates |
| Account | Notifications, bookings, documents, invoices (simulated payments), feedback and complaints, profile, sign-out |

### For the agency team

| Area | What you can do |
|---|---|
| Studio dashboard | Active projects, booking requests, open and overdue tasks, my tasks, projects needing attention |
| Clients | Search, status filters, project counts, create/edit with validation; admins can delete clients that have no projects |
| Projects | Every project; change status, tick off milestones, edit deliverables, share new versions for review |
| Tasks | Create/edit/delete, assignee, priority, status, due date, overdue flags, "assigned to me", sort by due date, priority or newest |
| Documents | Categories, project links, shared vs internal visibility, file metadata via the system picker |
| Bookings inbox | Confirm or decline requests; the client is notified |
| Feedback inbox | Reply to feedback and complaints and set their status |
| Payments | Studio admins see all invoices |

### Roles and permissions

Permissions live in one table (`core/auth/Permissions.kt`) and are **enforced in the repositories**, not just hidden in the UI.

| Permission | Client | Agency Team | Studio Admin |
|---|:-:|:-:|:-:|
| See only own organisation's projects, documents, invoices | ✓ | | |
| Approve deliverables / request revisions | ✓ | | |
| Create bookings, send feedback | ✓ | | |
| Manage clients, projects, tasks, bookings | | ✓ | ✓ |
| Edit deliverables, share new versions | | ✓ | ✓ |
| See internal documents | | ✓ | ✓ |
| Delete clients, see all payments | | | ✓ |

The agency can never set a deliverable to *Approved* or *Revision Requested*. Those are client decisions, and the repository rejects them.

### Demo accounts

Pick one on the sign-in screen. All people and companies are fictional.

| Account | Role |
|---|---|
| Maya Rao, Aetheria Sounds | Client |
| Dev Malhotra, Apex Athletics | Client |
| Arjun Mehta | Studio Admin |
| Zoya Khan, Rohan Das, Isha Patel | Agency Team |

---

## Run it

### Android Studio

1. Clone the repo and open the folder in Android Studio (a recent stable release with Android Gradle Plugin 9.3 support).
2. Let Gradle sync. Android Studio's bundled JDK is fine.
3. Pick an emulator or device on **Android 8.0 (API 26) or newer** and press **Run ▶**.
4. On the sign-in screen, choose a client or agency demo account.

### Command line

The Gradle wrapper needs a JDK. If none is installed system-wide, point `JAVA_HOME` at Android Studio's bundled one:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew assembleDebug
```

```bash
./gradlew installDebug
```

> On `main` before PR #5 merges, `gradlew` is not executable; use `sh ./gradlew …`.

---

## Testing

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew connectedDebugAndroidTest
```

```bash
./gradlew lintDebug
```

Last verified on 22 Oct 2026 on an API 37 emulator:

| Check | Result |
|---|---|
| Unit tests | 92 tests, 0 failures |
| Instrumented UI tests | 6 tests, 0 failures in each of 5 consecutive from-scratch runs |
| Lint | 0 errors; 13 warnings, all dependency/SDK version advisories |
| `assembleDebug`, `assembleRelease` | Successful |

Unit tests cover every repository's permission rules, validators and ViewModels. UI tests cover the client project journey (download → revision), role-based tabs and sign-out, adding a client, sending a message, and the simulated payment flow.

Instrumented tests disable system animations (`testOptions.animationsDisabled`) and reset demo data before each test with the `FreshDemoData` rule. On the API 37 emulator, the first tap after launch was occasionally lost (about 1 in 25 test executions before the fix), so the `signInAs` helper retries the demo sign-in tap until the app shell appears. If a UI test fails with "No compose hierarchies found", re-run it and report it if it persists.

---

## Architecture

Single-module MVVM app with feature packages:

```
com.koach.studio
├── app/            Shell: root, role-aware bottom bar, NavHost, menu
├── auth/           Demo sign-in screen
├── core/
│   ├── auth/       AuthRepository, roles → permissions, AccessControl
│   ├── data/       DemoData seed
│   ├── designsystem/  KoachTheme, colours, type, shapes, components
│   ├── di/         AppContainer, koachViewModel {}
│   ├── model/      Shared domain models
│   └── ui/         Formatting (₹, dates, sizes)
├── projects/       Projects, milestones, deliverables, revisions (from PR #4)
├── clients/        Client management
├── tasks/          Task management
├── documents/      Document management
├── messaging/      Project conversations
├── notifications/  Notification repository and centre
├── feedback/       Feedback and complaints
├── bookings/       Booking form, history, inbox
├── catalog/ services/ portfolio/   Explore tab
├── home/           Client home and Studio dashboard
├── payments/       Simulated invoices
└── profile/
```

**Data flow:** Screen → ViewModel (`StateFlow<UiState>`) → repository interface (`Flow` reads, `suspend … Result` writes) → in-memory implementation.

- `AppContainer` creates every dependency. `KoachApplication` owns it and `koachViewModel { }` builds ViewModels from it. Tests construct ViewModels directly with in-memory repositories.
- Repositories read the signed-in user through `AccessControl` and enforce permissions on every read filter and write, as Firestore security rules would.
- `SecuredProjectRepository` wraps PR #4's `InMemoryProjectRepository`, so the original store stays rule-free and replaceable.
- All colours, type and spacing come from `KoachTheme`; screens contain no hard-coded colours.

---

## Backend status and limitations

| Feature | Status in this build |
|---|---|
| Sign-in | **Simulated.** Demo account picker. `signInWithEmail` returns an error explaining Firebase isn't connected. |
| Data | **In memory.** Seeded from `DemoData` / `FakeProjectDataSource`, lost when the app closes. |
| Deliverable downloads | **Simulated** progress; no file is downloaded. |
| Documents | **Metadata only.** The picker reads name/type/size; nothing is uploaded (`DemoDocumentStorage.isAvailable = false`). |
| Payments | **Simulated.** "Pay now" only flips the invoice status; no payment provider, no money moves. |
| Notifications | In-app only; no push notifications. |
| Portfolio | Sample projects, labelled as such; not real client work. |

No Firebase configuration, `google-services.json`, API keys or credentials are included.

### Moving to Firebase

Each repository is an interface, so the path is:

1. Add Firebase to the project (requires the team's own Firebase project and `google-services.json`, kept out of Git).
2. Implement `AuthRepository` with Firebase Authentication, mapping users to a Firestore profile with a `role` and `clientId`.
3. Implement the repositories with Firestore snapshot listeners; the `Flow` APIs map directly.
4. Implement `DocumentStorage` with Firebase Storage and fill `StudioDocument.storagePath`.
5. Mirror the `AccessPolicy` rules in Firestore and Storage security rules.
6. Provide a `FirebaseAppContainer` instead of `DemoAppContainer` in `KoachApplication`.

---

## Team

| Member | Responsibility |
|---|---|
| **Shravan Kadam** | Integration lead: architecture, design system, navigation shell, shared models and repositories, Git workflow, QA |
| **Aditya Menon** | Client management, plus the original projects and deliverables flow (PR #4) |
| **Kushagra Khare** | Tasks and documents |
| **Suhani** | Messaging, notifications, feedback and complaints |

See [CONTRIBUTING.md](CONTRIBUTING.md) for module ownership, the branch workflow and review rules.

---

## Known gaps and future work

- Real backend (Firebase Auth, Firestore, Storage) and push notifications.
- Profile editing and email sign-in once real accounts exist.
- Dependency upgrades flagged by lint (AGP 9.4, Compose BOM 2026.09, Kotlin 2.4, compileSdk 37) and a targetSdk bump, best done in a dedicated PR with a full regression run.
- Project creation from a confirmed booking (projects are seeded today).
- Real portfolio media in place of generated covers.
- Light theme is intentionally not provided; the brand is dark-only.
