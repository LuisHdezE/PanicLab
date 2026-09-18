package com.aistudio.paniclab.shared

/**
 * Non-product probe used only to validate the I1 shared KMP scaffold and QA plumbing.
 * Product diagnostic behavior is intentionally not moved in I1.
 */
object ScaffoldProbe {
  fun normalizeLabel(value: String): String =
    if (value.isBlank()) "paniclab" else value.trim()
}
