package com.koach.studio.core.di

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.ProjectRepository
import java.time.Clock

/**
 * Single place where the app's dependencies are created. ViewModels receive
 * what they need from here through constructor parameters, so tests can pass
 * fakes directly and the in-memory repositories can later be swapped for
 * Firebase-backed ones without touching any screen.
 */
interface AppContainer {
    val clock: Clock
    val projectRepository: ProjectRepository
}

/** Demo wiring: every repository is in-memory and resets when the process dies. */
class DemoAppContainer(
    override val clock: Clock = Clock.systemDefaultZone()
) : AppContainer {
    override val projectRepository: ProjectRepository by lazy { InMemoryProjectRepository() }
}
