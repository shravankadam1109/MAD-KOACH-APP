package com.koach.studio.projects.model

import java.time.LocalDate

/** A client's request for changes. Kept as history; never overwritten. */
data class Revision(
    val id: String,
    val projectId: String,
    val deliverableId: String?,
    val reason: String,
    val requestedBy: String,
    val requestedOn: LocalDate,
    val resolved: Boolean = false
)
