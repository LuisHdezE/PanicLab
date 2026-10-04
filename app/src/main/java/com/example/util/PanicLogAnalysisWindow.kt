package com.example.util

object PanicLogAnalysisWindow {

    fun forAnalysis(text: String): String {
        if (text.length <= MAX_ANALYSIS_CHARS) return text

        return buildString(MAX_ANALYSIS_CHARS + TRUNCATION_MARKER.length) {
            append(text, 0, HEAD_CHARS)
            append(TRUNCATION_MARKER)
            append(text, text.length - TAIL_CHARS, text.length)
        }
    }

    const val MAX_ANALYSIS_CHARS = 160_000
    private const val HEAD_CHARS = 128_000
    private const val TAIL_CHARS = 32_000
    private const val TRUNCATION_MARKER =
        "\n\n[... contenido no diagnóstico omitido para análisis seguro ...]\n\n"
}
