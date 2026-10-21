package com.koach.studio.projects.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** [Project.dueDate] is stored as ISO text (yyyy-MM-dd); null if it doesn't parse. */
val Project.dueLocalDate: LocalDate?
    get() = try {
        LocalDate.parse(dueDate)
    } catch (e: DateTimeParseException) {
        null
    }
