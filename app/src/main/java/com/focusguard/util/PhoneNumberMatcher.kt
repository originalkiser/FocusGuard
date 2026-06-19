package com.focusguard.util

object PhoneNumberMatcher {

    /** Strips all non-digit characters and removes a leading US country code (+1 / 1). */
    fun normalize(number: String): String {
        val digits = number.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("1")) digits.drop(1) else digits
    }

    /** True if the number's first 3 digits match the given area code. */
    fun matchesAreaCode(number: String, areaCode: String): Boolean {
        val stripped = normalize(number)
        val code = areaCode.filter { it.isDigit() }
        return stripped.startsWith(code)
    }

    /**
     * Matches a number against a glob-style pattern where '*' means "zero or more digits".
     * Example: "800*" matches any 800-xxx-xxxx number.
     */
    fun matchesPattern(number: String, pattern: String): Boolean {
        val digits = normalize(number)
        val regexStr = pattern
            .filter { it.isDigit() || it == '*' }
            .replace("*", "\\d*")
        return Regex("^$regexStr$").matches(digits)
    }
}
