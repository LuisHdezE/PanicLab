package com.example.util

object PanicLogAnalysisWindow {

    fun forAnalysis(text: String): String {
        if (text.length <= MAX_ANALYSIS_CHARS) return text

        val structuredCutoff = findAppleStructuredCutoff(text)
        if (structuredCutoff != null) {
            return buildString(structuredCutoff + TRUNCATION_MARKER.length) {
                append(text, 0, structuredCutoff)
                append(TRUNCATION_MARKER)
            }
        }

        return buildString(MAX_ANALYSIS_CHARS + TRUNCATION_MARKER.length) {
            append(text, 0, HEAD_CHARS)
            append(TRUNCATION_MARKER)
            append(text, text.length - TAIL_CHARS, text.length)
        }
    }

    private fun findAppleStructuredCutoff(text: String): Int? {
        val markerIndex = text.indexOf(PROCESS_BY_PID_MARKER)
        return markerIndex.takeIf { it >= MIN_DIAGNOSTIC_PREFIX_CHARS }
    }

    const val MAX_ANALYSIS_CHARS = 160_000
    private const val MIN_DIAGNOSTIC_PREFIX_CHARS = 4_096
    private const val HEAD_CHARS = 128_000
    private const val TAIL_CHARS = 32_000
    private const val PROCESS_BY_PID_MARKER = "\n  \"processByPid\""
    private const val TRUNCATION_MARKER =
        "\n\n[... contenido no diagnóstico omitido para análisis seguro ...]\n\n"
}
