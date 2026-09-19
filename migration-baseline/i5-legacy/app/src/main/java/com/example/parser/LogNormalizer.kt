package com.example.parser

object LogNormalizer {

    /**
     * Normalizes raw log text:
     * - Unescapes literal "\n", "\r", "\t", "\"" if copied as an escaped string or JSON dump
     * - Normalizes Windows CRLF (\r\n) or old Mac CR (\r) into standard LF (\n)
     * - Trims surrounding whitespace
     */
    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var processed = rawText

        // If the entire text is wrapped in quotes or escaped
        if (processed.contains("\\n") && !processed.contains("\n")) {
            processed = processed
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        // Standardize line endings to \n
        processed = processed.replace("\r\n", "\n").replace("\r", "\n")

        return processed.trim()
    }
}
