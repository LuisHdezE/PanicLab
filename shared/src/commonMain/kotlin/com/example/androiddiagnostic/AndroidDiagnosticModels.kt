package com.example.androiddiagnostic

import com.example.artifact.AndroidEvidence

enum class AndroidDiagnosticFamily {
    KERNEL_WATCHDOG,
    CAMERA,
    MODEM_RADIO,
    THERMAL,
    STORAGE_IO
}

enum class AndroidDiagnosticCategory {
    SOFTWARE_PROBABLE,
    DRIVER_HAL_PROBABLE,
    COMMUNICATION_INTERCONNECT_PROBABLE,
    HARDWARE_PROBABLE,
    INDETERMINATE
}

data class AndroidDiagnosticCandidate(
    val ruleId: String,
    val family: AndroidDiagnosticFamily,
    val category: AndroidDiagnosticCategory,
    val confidence: Double,
    val rationale: String,
    val evidence: List<AndroidEvidence>
) {
    init {
        require(ruleId.isNotBlank()) { "ruleId must not be blank" }
        require(confidence in 0.0..1.0) { "confidence must be between 0.0 and 1.0" }
        require(rationale.isNotBlank()) { "rationale must not be blank" }
        require(evidence.isNotEmpty()) { "diagnostic candidates require evidence" }
    }
}

data class AndroidDiagnosticReport(
    val sourceFilename: String,
    val candidates: List<AndroidDiagnosticCandidate>
) {
    val primary: AndroidDiagnosticCandidate?
        get() = candidates.firstOrNull()

    fun candidatesFor(family: AndroidDiagnosticFamily): List<AndroidDiagnosticCandidate> =
        candidates.filter { it.family == family }
}
