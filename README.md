# KOACH Studio

A premium Android app for **KOACH Studio**, a creative agency offering filming, video editing, content creation, social media, branding, motion graphics and VFX. Clients book services, follow their projects, review deliverables and talk to their crew. The agency team runs clients, projects, tasks, documents and bookings from the same app.

Built with Kotlin, Jetpack Compose and Material 3. Dark-first UI with KOACH signature lime-green (`#41BA00`), plus user-selectable **Dark**, **Light**, and **System default** themes persisted across app restarts.

> **Dual-mode architecture.** The app supports both **Firebase** (`FirebaseAppContainer` with Firebase Authentication and Cloud Firestore repositories when `google-services.json` is configured) and an instant **Demo workspace** (`DemoAppContainer` with seeded in-memory repositories). See [Backend status](#backend-status-and-limitations).

---

## Features

### For clients

| Area | What you can do |
|---|---|
| Home | Greeting, featured project hero card with progress and next milestone, quick actions, deliverables awaiting review, upcoming milestones |
| Projects | Your projects only, with compact search + filter sheet (service, status, sort order), active filter chips, progress and due dates |
| Project detail | Milestones, crew, description, deliverables, revision history, project documents |
| Deliverables | Simulated download, **Approve**, or **Request changes** (a reason is required and kept in history) |
| Explore | Services catalogue (7 categories, packages in INR) and KOACH Studio's curated social media & creative portfolio artwork |
| Booking | Service and package picker, date picker (2+ days' notice), budget, brief, validation, confirmation, booking history |
| Messages | One conversation per project, unread badges, live updates |
| Contact | Studio address (Bandra West), operating hours, direct phone dialer, urgent production hotline, direct email, map location, and quick booking |
| Account & Settings | Appearance (Dark / Light / System default), notification & privacy preferences, bookings, documents, invoices (simulated payments), feedback, profile, about & version info, sign-out |

### For the agency team

| Area | What you can do |
|---|---|
| Studio dashboard | Active project count & average completion, status breakdown (In Progress, In Review, Revisions, Delivered), booking requests, open & overdue tasks, workflow shortcuts, projects needing attention, my tasks, upcoming 14-day deadlines, and team workload distribution |
| Clients | Compact search, status filters, active/lead summary counts, project counts, create/edit with validation; admins can delete clients that have no projects |
| Projects | Every project with compact filter & sort bottom sheet; change status, tick off milestones, edit deliverables, share new versions for review |
| Tasks | Create/edit/delete, assignee, priority, status, due date, overdue flags, "assigned to me", sort by due date, priority or newest, responsive card layout |
| Documents | Categories, project links, shared vs internal visibility, file metadata via the system picker |
| Bookings inbox | Confirm or decline requests; the client is notified |
| Feedback inbox | Reply to feedback and complaints and set their status |
| Payments | Studio admins see all invoices |
| More & Settings | Central studio tools hub + Appearance (Dark / Light / System default), notifications, privacy, account/session controls, and app version details |

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
4. On the sign-in screen, choose a client or agency demo account (or sign in with Firebase when `google-services.json` is configured).

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

Unit tests cover every repository's permission rules, Firestore mappers, validators, theme preferences, and ViewModels. UI tests cover the client project journey (download → revision), role-based tabs and sign-out, adding a client, sending a message, and the simulated payment flow.

Instrumented tests disable system animations (`testOptions.animationsDisabled`) and reset demo data before each test with the `FreshDemoData` rule.

---

## Architecture

Single-module MVVM app with feature packages:

```
com.koach.studio
├── app/            Shell: root, role-aware bottom bar, NavHost, More & Account menu
├── auth/           Welcome, Login, SignUp, and Demo sign-in screens
├── core/
│   ├── auth/       AuthRepository, roles → permissions, AccessControl
│   ├── data/       DemoData seed & real KOACH portfolio items
│   ├── designsystem/  KoachTheme (Dark/Light/System), colours, type, shapes, components
│   ├── di/         AppContainer, DemoAppContainer, koachViewModel {}
│   ├── firebase/   FirebaseAppContainer, FirebaseAuthRepository, Firestore repositories
│   ├── model/      Shared domain models
│   ├── settings/   ThemePreferencesStore (SharedPreferences-backed & in-memory)
│   └── ui/         Formatting (₹, dates, sizes)
├── projects/       Projects, compact filter sheet, milestones, deliverables, revisions
├── clients/        Client management
├── tasks/          Task management
├── documents/      Document management
├── messaging/      Project conversations
├── notifications/  Notification repository and centre
├── feedback/       Feedback and complaints
├── contact/        Studio location, hotlines, direct email, map & actions
├── bookings/       Booking form, history, inbox
├── catalog/ services/ portfolio/   Explore tab & real KOACH portfolio artwork
├── home/           Client home and Studio dashboard (workload, deadlines, breakdown)
├── payments/       Simulated invoices
├── profile/        Account profile
└── settings/       Central Settings screen (Appearance, notifications, privacy, about)
```

**Data flow:** Screen → ViewModel (`StateFlow<UiState>`) → repository interface (`Flow` reads, `suspend … Result` writes) → Firestore or in-memory implementation.

- `AppContainer` creates every dependency. `KoachApplication` selects `FirebaseAppContainer` when Firebase is configured, or falls back to `DemoAppContainer`.
- `ThemePreferencesStore` persists user theme (`DARK`, `LIGHT`, `SYSTEM`) and notification/privacy toggles via `SharedPreferences`.
- Repositories read the signed-in user through `AccessControl` and enforce permissions on every read filter and write, mirrored in `firestore.rules`.
- All colours, type and spacing come from `KoachTheme`; screens adapt seamlessly between Dark and Light modes.

---

## Backend status and limitations

| Feature | Status in this build |
|---|---|
| Sign-in | **Firebase Auth + Demo fallback.** Supports email/password when Firebase is configured, plus instant demo account switching. |
| Data | **Firestore + In-memory fallback.** Full Firestore repositories (`projects`, `clients`, `tasks`, `documents`, `bookings`, `messages`, `notifications`, `payments`, `feedback`, `catalog`) with offline demo fallback. |
| Deliverable downloads | **Simulated** progress; no file is downloaded. |
| Documents | **Metadata only.** The picker reads name/type/size; binary upload to Firebase Storage is not yet wired (`DemoDocumentStorage.isAvailable = false`). |
| Payments | **Simulated.** "Pay now" only flips the invoice status; no payment provider, no money moves. |
| Notifications | In-app only; no push notifications (FCM). |
| Portfolio | Curated real KOACH Studio social media & creative artwork (`res/drawable-nodpi/portfolio_*.jpg`). |

---

## Team

| Member | Responsibility |
|---|---|
| **Shravan Kadam** | Integration lead: architecture, design system, Firebase integration, navigation shell, shared models and repositories, Git workflow, QA |
| **Aditya Menon** | Client management, plus the original projects and deliverables flow (PR #4) |
| **Kushagra Khare** | Tasks, documents, design-system refinement, Studio dashboard workload, compact project filters, and persistent Settings & theming |
| **Suhani Jain** | Client-facing UI & UX quality, studio contact workflows, service & portfolio booking integration, accessibility, and documentation |

See [CONTRIBUTING.md](CONTRIBUTING.md) for module ownership, the branch workflow and review rules.
