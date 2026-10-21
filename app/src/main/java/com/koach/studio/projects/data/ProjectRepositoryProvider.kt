package com.koach.studio.projects.data

object ProjectRepositoryProvider {
    var repository: ProjectRepository = InMemoryProjectRepository()

    fun reset() {
        repository = InMemoryProjectRepository()
    }
}
