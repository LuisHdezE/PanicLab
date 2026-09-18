package com.example.util

import com.example.domain.model.PanicCode
import com.example.domain.model.SensorCode

object HexUtils {
    fun parseCodeToLong(code: String): Long? {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return null

        return try {
            when {
                trimmed.startsWith("0x", ignoreCase = true) -> trimmed.substring(2).toLong(16)
                trimmed.matches(Regex("^[0-9]+$")) -> trimmed.toLong(10)
                trimmed.matches(Regex("^[0-9a-fA-F]+$")) -> trimmed.toLong(16)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parse(code: String): Long? = parseCodeToLong(code)

    fun parseSensorCode(code: String): SensorCode? = SensorCode.parse(code)

    fun parsePanicCode(code: String): PanicCode? = PanicCode.parse(code)

    fun toCanonicalHex(code: String): String {
        val value = parseCodeToLong(code) ?: return code.trim()
        return "0x" + value.toString(16).uppercase()
    }

    fun toDecimalString(code: String): String {
        val value = parseCodeToLong(code) ?: return code.trim()
        return value.toString(10)
    }

    fun areCodesEquivalent(code1: String, code2: String): Boolean {
        if (code1.equals(code2, ignoreCase = true)) return true
        val value1 = parseCodeToLong(code1) ?: return false
        val value2 = parseCodeToLong(code2) ?: return false
        return value1 == value2
    }

    fun hasBits(fullMask: Long, subMask: Long): Boolean {
        if (subMask == 0L) return false
        return (fullMask and subMask) == subMask
    }
}
