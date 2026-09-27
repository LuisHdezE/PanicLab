package com.example.androiddiagnostic

import com.example.artifact.AndroidEvidence
import com.example.artifact.AndroidEvidenceKind
import com.example.artifact.AndroidEvidenceReport
import com.example.artifact.AndroidEvidenceSeverity
import kotlin.math.abs

class DeterministicAndroidDiagnosticEngine(
    private val correlationWindowLines: Int = 60
) {
    init {
        require(correlationWindowLines >= 0) { "correlationWindowLines must be non-negative" }
    }

    fun diagnose(report: AndroidEvidenceReport): AndroidDiagnosticReport {
        val candidates = buildList {
            evaluateKernelWatchdog(report)?.let(::add)
            evaluateCamera(report)?.let(::add)
            evaluateModemRadio(report)?.let(::add)
            evaluateThermal(report)?.let(::add)
            evaluateStorage(report)?.let(::add)
        }.sortedWith(
            compareByDescending<AndroidDiagnosticCandidate> { it.confidence }
                .thenBy { it.ruleId }
        )

        return AndroidDiagnosticReport(
            sourceFilename = report.sourceFilename,
            candidates = candidates
        )
    }

    private fun evaluateKernelWatchdog(report: AndroidEvidenceReport): AndroidDiagnosticCandidate? {
        val panic = report.of(AndroidEvidenceKind.KERNEL_PANIC)
        val watchdog = report.of(AndroidEvidenceKind.WATCHDOG)
        if (panic.isEmpty() && watchdog.isEmpty()) return null

        if (panic.isNotEmpty() && watchdog.isNotEmpty()) {
            val correlated = correlated(panic, watchdog)
            return candidate(
                ruleId = "ADX4.KERNEL.001",
                family = AndroidDiagnosticFamily.KERNEL_WATCHDOG,
                category = if (correlated) AndroidDiagnosticCategory.DRIVER_HAL_PROBABLE else AndroidDiagnosticCategory.INDETERMINATE,
                confidence = if (correlated) 0.84 else 0.72,
                rationale = if (correlated) {
                    "Kernel panic and watchdog evidence occur close together; a low-level driver/HAL or kernel interaction is more plausible than an isolated application failure."
                } else {
                    "Kernel panic and watchdog evidence are both present, but they are not closely correlated in the report."
                },
                evidence = pickEvidence(panic, watchdog)
            )
        }

        return if (panic.isNotEmpty()) {
            candidate(
                "ADX4.KERNEL.002",
                AndroidDiagnosticFamily.KERNEL_WATCHDOG,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.62,
                "A kernel panic is present without independent watchdog corroboration; root cause remains indeterminate.",
                panic.take(3)
            )
        } else {
            candidate(
                "ADX4.KERNEL.003",
                AndroidDiagnosticFamily.KERNEL_WATCHDOG,
                AndroidDiagnosticCategory.SOFTWARE_PROBABLE,
                0.56,
                "Watchdog evidence is present without a kernel panic; software, scheduler, or service stalls remain plausible.",
                watchdog.take(3)
            )
        }
    }

    private fun evaluateCamera(report: AndroidEvidenceReport): AndroidDiagnosticCandidate? {
        val camera = report.of(AndroidEvidenceKind.CAMERA_HAL)
        if (camera.isEmpty()) return null
        val native = report.of(AndroidEvidenceKind.NATIVE_CRASH)
        val correlatedNative = correlated(camera, native)

        return when {
            camera.size >= 2 && correlatedNative -> candidate(
                "ADX4.CAMERA.001",
                AndroidDiagnosticFamily.CAMERA,
                AndroidDiagnosticCategory.DRIVER_HAL_PROBABLE,
                0.84,
                "Repeated camera/HAL failures are correlated with a native crash, strengthening a driver/HAL hypothesis.",
                pickEvidence(camera, native)
            )
            camera.size >= 2 -> candidate(
                "ADX4.CAMERA.002",
                AndroidDiagnosticFamily.CAMERA,
                AndroidDiagnosticCategory.DRIVER_HAL_PROBABLE,
                0.70,
                "Repeated camera/HAL failures are present without a correlated native crash.",
                camera.take(4)
            )
            correlatedNative -> candidate(
                "ADX4.CAMERA.003",
                AndroidDiagnosticFamily.CAMERA,
                AndroidDiagnosticCategory.DRIVER_HAL_PROBABLE,
                0.68,
                "A camera/HAL failure is closely correlated with a native crash.",
                pickEvidence(camera, native)
            )
            else -> candidate(
                "ADX4.CAMERA.004",
                AndroidDiagnosticFamily.CAMERA,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.50,
                "A single camera/HAL failure is insufficient to distinguish transient software, HAL, interconnect, or hardware causes.",
                camera.take(2)
            )
        }
    }

    private fun evaluateModemRadio(report: AndroidEvidenceReport): AndroidDiagnosticCandidate? {
        val radio = report.of(AndroidEvidenceKind.MODEM_RADIO)
        if (radio.isEmpty()) return null
        val native = report.of(AndroidEvidenceKind.NATIVE_CRASH)
        val correlatedNative = correlated(radio, native)

        return when {
            radio.size >= 2 && correlatedNative -> candidate(
                "ADX4.RADIO.001",
                AndroidDiagnosticFamily.MODEM_RADIO,
                AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE,
                0.82,
                "Repeated modem/radio failures correlate with a native crash, increasing the likelihood of a modem stack or communication/interconnect fault.",
                pickEvidence(radio, native)
            )
            radio.size >= 2 -> candidate(
                "ADX4.RADIO.002",
                AndroidDiagnosticFamily.MODEM_RADIO,
                AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE,
                0.69,
                "Repeated modem/radio failures are present without independent native-crash corroboration.",
                radio.take(4)
            )
            correlatedNative -> candidate(
                "ADX4.RADIO.003",
                AndroidDiagnosticFamily.MODEM_RADIO,
                AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE,
                0.66,
                "A modem/radio failure is closely correlated with a native crash.",
                pickEvidence(radio, native)
            )
            else -> candidate(
                "ADX4.RADIO.004",
                AndroidDiagnosticFamily.MODEM_RADIO,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.49,
                "A single modem/radio failure does not establish whether the cause is network, firmware, radio stack, interconnect, or hardware.",
                radio.take(2)
            )
        }
    }

    private fun evaluateThermal(report: AndroidEvidenceReport): AndroidDiagnosticCandidate? {
        val thermal = report.of(AndroidEvidenceKind.THERMAL)
        if (thermal.isEmpty()) return null
        val critical = report.of(AndroidEvidenceKind.WATCHDOG) + report.of(AndroidEvidenceKind.KERNEL_PANIC)
        val correlatedCritical = correlated(thermal, critical)

        return when {
            thermal.size >= 2 && correlatedCritical -> candidate(
                "ADX4.THERMAL.001",
                AndroidDiagnosticFamily.THERMAL,
                AndroidDiagnosticCategory.HARDWARE_PROBABLE,
                0.71,
                "Repeated thermal events correlate with a critical watchdog or kernel event; cooling, sensor, power, or board-level causes become more plausible, but are not proven.",
                pickEvidence(thermal, critical)
            )
            thermal.size >= 2 -> candidate(
                "ADX4.THERMAL.002",
                AndroidDiagnosticFamily.THERMAL,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.61,
                "Repeated thermal events are present without a closely correlated critical restart signal.",
                thermal.take(4)
            )
            else -> candidate(
                "ADX4.THERMAL.003",
                AndroidDiagnosticFamily.THERMAL,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.48,
                "A single thermal event can be workload- or environment-related and is not sufficient for a hardware conclusion.",
                thermal.take(2)
            )
        }
    }

    private fun evaluateStorage(report: AndroidEvidenceReport): AndroidDiagnosticCandidate? {
        val storage = report.of(AndroidEvidenceKind.STORAGE_IO)
        if (storage.isEmpty()) return null
        val critical = report.of(AndroidEvidenceKind.KERNEL_PANIC) + report.of(AndroidEvidenceKind.WATCHDOG)
        val correlatedCritical = correlated(storage, critical)
        val lowLevelStorage = storage.any { evidence ->
            val text = evidence.excerpt.lowercase()
            "ufs" in text || "mmc" in text || "blk_update_request" in text
        }

        return when {
            storage.size >= 2 && correlatedCritical && lowLevelStorage -> candidate(
                "ADX4.STORAGE.001",
                AndroidDiagnosticFamily.STORAGE_IO,
                AndroidDiagnosticCategory.HARDWARE_PROBABLE,
                0.81,
                "Repeated low-level storage I/O failures correlate with a critical kernel/watchdog event; physical storage or storage interconnect failure is probable but not proven.",
                pickEvidence(storage, critical)
            )
            storage.size >= 2 && correlatedCritical -> candidate(
                "ADX4.STORAGE.002",
                AndroidDiagnosticFamily.STORAGE_IO,
                AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE,
                0.73,
                "Repeated storage I/O failures correlate with a critical kernel/watchdog event, but the evidence does not isolate physical media from filesystem or interconnect causes.",
                pickEvidence(storage, critical)
            )
            storage.size >= 2 -> candidate(
                "ADX4.STORAGE.003",
                AndroidDiagnosticFamily.STORAGE_IO,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.63,
                "Repeated storage I/O failures are present without a correlated critical kernel/watchdog event.",
                storage.take(4)
            )
            else -> candidate(
                "ADX4.STORAGE.004",
                AndroidDiagnosticFamily.STORAGE_IO,
                AndroidDiagnosticCategory.INDETERMINATE,
                0.51,
                "A single storage I/O error can result from filesystem, transient driver, media, or interconnect causes.",
                storage.take(2)
            )
        }
    }

    private fun AndroidEvidenceReport.of(kind: AndroidEvidenceKind): List<AndroidEvidence> =
        evidence.filter { it.kind == kind }

    private fun correlated(left: List<AndroidEvidence>, right: List<AndroidEvidence>): Boolean {
        if (left.isEmpty() || right.isEmpty()) return false
        return left.any { a ->
            right.any { b ->
                a.sourcePath == b.sourcePath && abs(a.lineNumber - b.lineNumber) <= correlationWindowLines
            }
        }
    }

    private fun pickEvidence(first: List<AndroidEvidence>, second: List<AndroidEvidence>): List<AndroidEvidence> =
        (first.take(3) + second.take(3))
            .distinctBy { "${it.code}:${it.sourcePath}:${it.lineNumber}" }
            .sortedBy { it.lineNumber }
            .take(6)

    private fun candidate(
        ruleId: String,
        family: AndroidDiagnosticFamily,
        category: AndroidDiagnosticCategory,
        confidence: Double,
        rationale: String,
        evidence: List<AndroidEvidence>
    ): AndroidDiagnosticCandidate = AndroidDiagnosticCandidate(
        ruleId = ruleId,
        family = family,
        category = category,
        confidence = confidence,
        rationale = rationale,
        evidence = evidence
    )
}
