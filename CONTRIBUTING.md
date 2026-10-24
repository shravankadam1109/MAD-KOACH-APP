# Contributing Guidelines

## Team and module ownership

Each module has an owner who reviews changes to it and leads its future work. Anyone can contribute anywhere, but ask the owner to review.

| Member | Owns | Packages |
|---|---|---|
| **Shravan Kadam** (integration lead) | Architecture, design system, navigation shell, shared models and repository interfaces, dependency container, Git workflow, final QA | `app/`, `core/`, `auth/`, `home/`, `catalog/`, `services/`, `portfolio/`, `bookings/`, `payments/`, `profile/` |
| **Aditya Menon** | Client management; projects and deliverables (original PR #4) | `clients/`, `projects/` |
| **Kushagra Khare** | Tasks and documents | `tasks/`, `documents/` |
| **Suhani Jain** | Client-facing UI/UX, contact workflows, messaging, notifications, feedback | `contact/`, `messaging/`, `notifications/`, `feedback/` |

Changes to shared code (`core/`, `app/`, repository interfaces, `AppContainer`) affect every module, so Shravan reviews them and the affected owners are tagged.

## Branch naming
- `feature/<name>` for new features (e.g. `feature/home-screen`)
- `fix/<name>` for bug fixes
- `docs/<name>` for documentation changes
- `chore/<name>` for tooling and build changes
- `test/<name>` for test-only changes

Never commit directly to `main`.

## Commit messages
Use [Conventional Commits](https://www.conventionalcommits.org/) in the present tense:
- Good: `feat: add service card component`, `fix: hide drafts from clients`
- Bad: "stuff", "update", "fix"

Keep each commit to one change that builds and passes tests. Never force-push shared branches or rewrite published history.

## Workflow
1. Pick an open Issue and assign yourself.
2. Create a branch from the latest `main`. If your work depends on an open PR, branch from that PR's branch and say so in your PR.
3. Make small, focused commits.
4. Run the checks below.
5. Push your branch and open a Pull Request.
6. Write `Closes #<issue number>` in the PR description.
7. Get at least one teammate to review (the module owner, where there is one), then merge.
8. Pull the latest `main` before starting your next task.

## Code conventions
- **Design system only.** Use `KoachTheme.colors`, `MaterialTheme.typography` and the components in `core/designsystem/components`. Don't hard-code colours or font sizes in screens.
- **MVVM.** Screens read a `StateFlow<UiState>` from a ViewModel and call ViewModel functions. No repository calls from composables.
- **Repositories behind interfaces.** Expose reads as `Flow` and writes as `suspend fun …: Result<…>`. Put the in-memory version next to the interface and register it in `AppContainer`.
- **Enforce permissions in the repository** with `AccessControl` and the `Permission` table, not only by hiding UI.
- **Create ViewModels with `koachViewModel { }`.** Pass dependencies through the constructor so tests can build them directly.
- **Be honest about demo behaviour.** Anything simulated (sign-in, downloads, uploads, payments) must say so in the UI.
- **Never commit secrets**: no API keys, passwords, signing keys or `google-services.json`. Don't commit `local.properties` or machine-specific files.

## Checks before opening a PR

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew connectedDebugAndroidTest
```

```bash
./gradlew lintDebug
```

Instrumented tests need an emulator or device. Use the `FreshDemoData` rule chained around `createAndroidComposeRule` so each test starts with clean data.

## Pull request checklist
- The app builds and runs without errors
- Unit tests pass, and new logic has tests
- I tested my changes on a phone or emulator
- I included a screenshot (for UI changes)
- New screens use the design system and work for the roles that can reach them
