package com.probill.utility

object RegexUtils {
    fun matchPhone(phone: String): Boolean
        = phone.matches("(([0-9]){10})".toRegex())
}