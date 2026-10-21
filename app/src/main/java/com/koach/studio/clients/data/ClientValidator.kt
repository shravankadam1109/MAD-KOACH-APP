package com.koach.studio.clients.data

object ClientValidator {
    const val NAME_MAX = 80
    const val NOTES_MAX = 500
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    enum class Field { NAME, CONTACT, EMAIL, PHONE, INDUSTRY, CITY, NOTES }

    fun validate(draft: ClientDraft): Map<Field, String> = buildMap {
        val name = draft.name.trim()
        when {
            name.length < 2 -> put(Field.NAME, "Enter the client's name")
            name.length > NAME_MAX -> put(Field.NAME, "Keep the name under $NAME_MAX characters")
        }
        if (draft.contactName.trim().length < 2) put(Field.CONTACT, "Enter a contact person")
        if (!EMAIL.matches(draft.email.trim())) put(Field.EMAIL, "Enter a valid email address")
        phoneError(draft.phone)?.let { put(Field.PHONE, it) }
        if (draft.industry.isBlank()) put(Field.INDUSTRY, "Enter an industry")
        if (draft.city.isBlank()) put(Field.CITY, "Enter a city")
        if (draft.notes.length > NOTES_MAX) put(Field.NOTES, "Keep notes under $NOTES_MAX characters")
    }

    /** Accepts digits with optional leading +, spaces and dashes; 10–15 digits. */
    fun phoneError(phone: String): String? {
        val trimmed = phone.trim()
        if (!trimmed.matches(Regex("^\\+?[0-9 -]+$"))) return "Use digits, spaces or dashes"
        val digits = trimmed.count(Char::isDigit)
        return if (digits in 10..15) null else "Phone numbers need 10–15 digits"
    }
}
