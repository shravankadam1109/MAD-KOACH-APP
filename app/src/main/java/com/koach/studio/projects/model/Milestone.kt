package com.koach.studio.projects.model

import java.time.LocalDate

data class Milestone(
    val id: String,
    val title: String,
    val dueDate: LocalDate,
    val completed: Boolean = false
)
