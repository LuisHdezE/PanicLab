package com.example.util

import java.util.Locale

object HexUtils {

    /**
     * Parses a string code (which might be "0x1000", "0X1000", "1000", "3145728", or "4096")
     * into a standard Long value.
     */
    fun parseCodeToLong(code: String): Long? {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return null

        return try {
            if (trimmed.startsWith("0x", ignoreCase = true)) {
                val hexPart = trimmed.substring(2)
                hexPart.toLong(16)
            } else if (trimmed.matches(Regex("^[0-9]+$"))) {
                // Could be decimal number like 3145728 or 4194304 or 0
                trimmed.toLong(10)
            } else if (trimmed.matches(Regex("^[0-9a-fA-F]+$"))) {
                trimmed.toLong(16)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Alias for parseCodeToLong for concise numeric evaluation
     */
    fun parse(code: String): Long? = parseCodeToLong(code)

    /**
     * Parses a string code into a complete PanicCode/SensorCode representation
     */
    fun parseSensorCode(code: String): com.example.domain.model.SensorCode? {
        return com.example.domain.model.SensorCode.parse(code)
    }

    fun parsePanicCode(code: String): com.example.domain.model.PanicCode? {
        return com.example.domain.model.PanicCode.parse(code)
    }

    /**
     * Converts any code (hex or decimal) to canonical uppercase hex format (e.g. 3145728 -> "0x300000", "0x800" -> "0x800")
     */
    fun toCanonicalHex(code: String): String {
        val value = parseCodeToLong(code) ?: return code.trim()
        return "0x" + value.toString(16).uppercase(Locale.ROOT)
    }

    fun toDecimalString(code: String): String {
        val value = parseCodeToLong(code) ?: return code.trim()
        return value.toString(10)
    }

    /**
     * Checks if two code representations match (e.g., "3145728" matches "0x300000")
     */
    fun areCodesEquivalent(code1: String, code2: String): Boolean {
        if (code1.equals(code2, ignoreCase = true)) return true
        val val1 = parseCodeToLong(code1) ?: return false
        val val2 = parseCodeToLong(code2) ?: return false
        return val1 == val2
    }

    /**
     * Bitmask check: checks if mask has all bits of candidate
     */
    fun hasBits(fullMask: Long, subMask: Long): Boolean {
        if (subMask == 0L) return false
        return (fullMask and subMask) == subMask
    }
}
