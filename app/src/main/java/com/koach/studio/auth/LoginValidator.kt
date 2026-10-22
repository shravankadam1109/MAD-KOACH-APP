package com.koach.studio.auth

object LoginValidator {
    const val PASSWORD_MIN = 6
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    enum class Field { EMAIL, PASSWORD }

    fun validate(email: String, password: String): Map<Field, String> = buildMap {
        when {
            email.isBlank() -> put(Field.EMAIL, "Enter your email")
            !EMAIL.matches(email.trim()) -> put(Field.EMAIL, "That doesn't look like an email address")
        }
        when {
            password.isEmpty() -> put(Field.PASSWORD, "Enter your password")
            password.length < PASSWORD_MIN -> put(Field.PASSWORD, "Passwords are at least $PASSWORD_MIN characters")
        }
    }
}
