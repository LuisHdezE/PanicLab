package com.example.parser

object LogNormalizer {
    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var processed = rawText

        if (processed.contains("\\n") && !processed.contains("\n")) {
            processed = processed
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        processed = processed.replace("\r\n", "\n").replace("\r", "\n")
        return processed.trim()
    }
}
