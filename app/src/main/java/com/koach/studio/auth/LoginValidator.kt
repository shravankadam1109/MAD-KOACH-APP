package com.koach.studio.auth

object LoginValidator {
    const val PASSWORD_MIN = 6
    const val NAME_MIN = 2
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^\\+?[0-9]{10,14}$")

    enum class Field { NAME, EMAIL, PHONE, PASSWORD, CONFIRM_PASSWORD, OTP_CODE }

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

    fun validateSignUp(name: String, email: String, phone: String, password: String): Map<Field, String> = buildMap {
        if (name.trim().length < NAME_MIN) put(Field.NAME, "Enter your full name")
        when {
            email.isBlank() -> put(Field.EMAIL, "Enter your email")
            !EMAIL.matches(email.trim()) -> put(Field.EMAIL, "That doesn't look like an email address")
        }
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.isNotEmpty() && !PHONE.matches(cleanPhone)) {
            put(Field.PHONE, "Enter a valid phone number (10-14 digits)")
        }
        when {
            password.isEmpty() -> put(Field.PASSWORD, "Choose a password")
            password.length < PASSWORD_MIN -> put(Field.PASSWORD, "Passwords must be at least $PASSWORD_MIN characters")
        }
    }

    fun validatePhone(phone: String): String? {
        val clean = phone.trim().replace(" ", "").replace("-", "")
        return when {
            clean.isBlank() -> "Enter your phone number"
            !PHONE.matches(clean) -> "Enter a valid phone number with country code (e.g. +91 98765 43210)"
            else -> null
        }
    }

    fun validateOtp(code: String): String? {
        return when {
            code.isBlank() -> "Enter the 6-digit verification code"
            code.trim().length != 6 || !code.trim().all { it.isDigit() } -> "Enter a valid 6-digit code"
            else -> null
        }
    }
}
