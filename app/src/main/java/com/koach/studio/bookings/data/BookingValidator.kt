package com.koach.studio.bookings.data

import java.time.LocalDate

/** Field rules for booking requests, shared by the form and the repository. */
object BookingValidator {
    const val MIN_LEAD_DAYS = 2L
    const val MAX_AHEAD_DAYS = 365L
    const val DETAILS_MIN = 20
    const val DETAILS_MAX = 600
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    enum class Field { SERVICE, DATE, BUDGET, DETAILS, NAME, EMAIL, PHONE }

    fun validate(request: BookingRequest, today: LocalDate): Map<Field, String> = buildMap {
        if (request.serviceId.isBlank()) put(Field.SERVICE, "Choose a service")
        dateError(request.preferredDate, today)?.let { put(Field.DATE, it) }
        if (request.budgetInr != null && request.budgetInr <= 0) put(Field.BUDGET, "Budget must be more than zero")
        detailsError(request.details)?.let { put(Field.DETAILS, it) }
        nameError(request.contactName)?.let { put(Field.NAME, it) }
        emailError(request.contactEmail)?.let { put(Field.EMAIL, it) }
        phoneError(request.contactPhone)?.let { put(Field.PHONE, it) }
    }

    fun nameError(name: String): String? =
        if (name.trim().length < 2) "Enter your name" else null

    fun dateError(date: LocalDate?, today: LocalDate): String? = when {
        date == null -> "Pick a preferred date"
        date.isBefore(today.plusDays(MIN_LEAD_DAYS)) -> "Please allow at least $MIN_LEAD_DAYS days' notice"
        date.isAfter(today.plusDays(MAX_AHEAD_DAYS)) -> "Choose a date within the next year"
        else -> null
    }

    fun detailsError(details: String): String? {
        val length = details.trim().length
        return when {
            length < DETAILS_MIN -> "Tell us a bit more (at least $DETAILS_MIN characters)"
            length > DETAILS_MAX -> "Keep it under $DETAILS_MAX characters"
            else -> null
        }
    }

    fun emailError(email: String): String? = if (EMAIL.matches(email.trim())) null else "Enter a valid email address"

    fun phoneError(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return null
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.length < 10 -> "Enter a valid phone number (at least 10 digits)"
            !trimmed.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' } -> "Enter a valid phone number"
            else -> null
        }
    }
}
