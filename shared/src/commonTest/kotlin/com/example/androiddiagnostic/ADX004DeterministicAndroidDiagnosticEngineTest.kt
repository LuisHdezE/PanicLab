package com.example.androiddiagnostic

import com.example.artifact.AndroidEvidence
import com.example.artifact.AndroidEvidenceKind
import com.example.artifact.AndroidEvidenceReport
import com.example.artifact.AndroidEvidenceSeverity
import com.example.artifact.AndroidEvidenceSource
import com.example.artifact.AndroidSubsystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ADX004DeterministicAndroidDiagnosticEngineTest {
    private val engine = DeterministicAndroidDiagnosticEngine()

    @Test
    fun emptyEvidenceProducesNoCandidates() {
        val result = engine.diagnose(report())
        assertTrue(result.candidates.isEmpty())
        assertNull(result.primary)
    }

    @Test
    fun correlatedKernelPanicAndWatchdogPreferLowLevelHypothesis() {
        val result = engine.diagnose(report(
            evidence(AndroidEvidenceKind.KERNEL_PANIC, 100),
            evidence(AndroidEvidenceKind.WATCHDOG, 125)
        ))
        assertEquals("ADX4.KERNEL.001", result.primary?.ruleId)
        assertEquals(AndroidDiagnosticCategory.DRIVER_HAL_PROBABLE, result.primary?.category)
        assertEquals(0.84, result.primary?.confidence)
    }

    @Test
    fun uncorrelatedKernelSignalsRemainIndeterminate() {
        val result = engine.diagnose(report(
            evidence(AndroidEvidenceKind.KERNEL_PANIC, 10),
            evidence(AndroidEvidenceKind.WATCHDOG, 500)
        ))
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, result.primary?.category)
        assertEquals(0.72, result.primary?.confidence)
    }

    @Test
    fun isolatedKernelPanicAndWatchdogUseSeparateRules() {
        val panic = engine.diagnose(report(evidence(AndroidEvidenceKind.KERNEL_PANIC, 10)))
        assertEquals("ADX4.KERNEL.002", panic.primary?.ruleId)
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, panic.primary?.category)

        val watchdog = engine.diagnose(report(evidence(AndroidEvidenceKind.WATCHDOG, 10)))
        assertEquals("ADX4.KERNEL.003", watchdog.primary?.ruleId)
        assertEquals(AndroidDiagnosticCategory.SOFTWARE_PROBABLE, watchdog.primary?.category)
    }

    @Test
    fun cameraRulesRequireCorroborationBeforeRaisingConfidence() {
        val strong = engine.diagnose(report(
            evidence(AndroidEvidenceKind.CAMERA_HAL, 100),
            evidence(AndroidEvidenceKind.CAMERA_HAL, 110),
            evidence(AndroidEvidenceKind.NATIVE_CRASH, 120)
        )).candidatesFor(AndroidDiagnosticFamily.CAMERA).single()
        assertEquals("ADX4.CAMERA.001", strong.ruleId)
        assertEquals(0.84, strong.confidence)

        val repeated = engine.diagnose(report(
            evidence(AndroidEvidenceKind.CAMERA_HAL, 100),
            evidence(AndroidEvidenceKind.CAMERA_HAL, 300)
        )).candidatesFor(AndroidDiagnosticFamily.CAMERA).single()
        assertEquals("ADX4.CAMERA.002", repeated.ruleId)

        val correlatedSingle = engine.diagnose(report(
            evidence(AndroidEvidenceKind.CAMERA_HAL, 100),
            evidence(AndroidEvidenceKind.NATIVE_CRASH, 130)
        )).candidatesFor(AndroidDiagnosticFamily.CAMERA).single()
        assertEquals("ADX4.CAMERA.003", correlatedSingle.ruleId)

        val isolated = engine.diagnose(report(evidence(AndroidEvidenceKind.CAMERA_HAL, 100)))
            .candidatesFor(AndroidDiagnosticFamily.CAMERA).single()
        assertEquals("ADX4.CAMERA.004", isolated.ruleId)
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, isolated.category)
    }

    @Test
    fun modemRadioRulesPreserveUncertaintyAndCorrelation() {
        val strong = engine.diagnose(report(
            evidence(AndroidEvidenceKind.MODEM_RADIO, 100),
            evidence(AndroidEvidenceKind.MODEM_RADIO, 110),
            evidence(AndroidEvidenceKind.NATIVE_CRASH, 130)
        )).candidatesFor(AndroidDiagnosticFamily.MODEM_RADIO).single()
        assertEquals("ADX4.RADIO.001", strong.ruleId)
        assertEquals(AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE, strong.category)

        val repeated = engine.diagnose(report(
            evidence(AndroidEvidenceKind.MODEM_RADIO, 100),
            evidence(AndroidEvidenceKind.MODEM_RADIO, 300)
        )).candidatesFor(AndroidDiagnosticFamily.MODEM_RADIO).single()
        assertEquals("ADX4.RADIO.002", repeated.ruleId)

        val correlatedSingle = engine.diagnose(report(
            evidence(AndroidEvidenceKind.MODEM_RADIO, 100),
            evidence(AndroidEvidenceKind.NATIVE_CRASH, 130)
        )).candidatesFor(AndroidDiagnosticFamily.MODEM_RADIO).single()
        assertEquals("ADX4.RADIO.003", correlatedSingle.ruleId)

        val isolated = engine.diagnose(report(evidence(AndroidEvidenceKind.MODEM_RADIO, 100)))
            .candidatesFor(AndroidDiagnosticFamily.MODEM_RADIO).single()
        assertEquals("ADX4.RADIO.004", isolated.ruleId)
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, isolated.category)
    }

    @Test
    fun thermalNeedsRepeatedAndCriticalCorrelationForHardwareProbable() {
        val strong = engine.diagnose(report(
            evidence(AndroidEvidenceKind.THERMAL, 100),
            evidence(AndroidEvidenceKind.THERMAL, 110),
            evidence(AndroidEvidenceKind.WATCHDOG, 130)
        )).candidatesFor(AndroidDiagnosticFamily.THERMAL).single()
        assertEquals("ADX4.THERMAL.001", strong.ruleId)
        assertEquals(AndroidDiagnosticCategory.HARDWARE_PROBABLE, strong.category)

        val repeated = engine.diagnose(report(
            evidence(AndroidEvidenceKind.THERMAL, 100),
            evidence(AndroidEvidenceKind.THERMAL, 300)
        )).candidatesFor(AndroidDiagnosticFamily.THERMAL).single()
        assertEquals("ADX4.THERMAL.002", repeated.ruleId)
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, repeated.category)

        val isolated = engine.diagnose(report(evidence(AndroidEvidenceKind.THERMAL, 100)))
            .candidatesFor(AndroidDiagnosticFamily.THERMAL).single()
        assertEquals("ADX4.THERMAL.003", isolated.ruleId)
    }

    @Test
    fun storageRequiresRepeatedCorrelatedLowLevelErrorsForStrongHardwareCandidate() {
        val strong = engine.diagnose(report(
            evidence(AndroidEvidenceKind.STORAGE_IO, 100, "ufs timeout"),
            evidence(AndroidEvidenceKind.STORAGE_IO, 110, "ufs fatal error"),
            evidence(AndroidEvidenceKind.KERNEL_PANIC, 125)
        )).candidatesFor(AndroidDiagnosticFamily.STORAGE_IO).single()
        assertEquals("ADX4.STORAGE.001", strong.ruleId)
        assertEquals(AndroidDiagnosticCategory.HARDWARE_PROBABLE, strong.category)
        assertEquals(0.81, strong.confidence)

        val correlatedFilesystem = engine.diagnose(report(
            evidence(AndroidEvidenceKind.STORAGE_IO, 100, "EXT4-fs error"),
            evidence(AndroidEvidenceKind.STORAGE_IO, 110, "I/O error"),
            evidence(AndroidEvidenceKind.WATCHDOG, 120)
        )).candidatesFor(AndroidDiagnosticFamily.STORAGE_IO).single()
        assertEquals("ADX4.STORAGE.002", correlatedFilesystem.ruleId)
        assertEquals(AndroidDiagnosticCategory.COMMUNICATION_INTERCONNECT_PROBABLE, correlatedFilesystem.category)

        val repeated = engine.diagnose(report(
            evidence(AndroidEvidenceKind.STORAGE_IO, 100),
            evidence(AndroidEvidenceKind.STORAGE_IO, 300)
        )).candidatesFor(AndroidDiagnosticFamily.STORAGE_IO).single()
        assertEquals("ADX4.STORAGE.003", repeated.ruleId)

        val isolated = engine.diagnose(report(evidence(AndroidEvidenceKind.STORAGE_IO, 100)))
            .candidatesFor(AndroidDiagnosticFamily.STORAGE_IO).single()
        assertEquals("ADX4.STORAGE.004", isolated.ruleId)
    }

    @Test
    fun rankingIsDeterministicAndHighestConfidenceComesFirst() {
        val result = engine.diagnose(report(
            evidence(AndroidEvidenceKind.CAMERA_HAL, 10),
            evidence(AndroidEvidenceKind.THERMAL, 100),
            evidence(AndroidEvidenceKind.THERMAL, 110),
            evidence(AndroidEvidenceKind.WATCHDOG, 120),
            evidence(AndroidEvidenceKind.STORAGE_IO, 300)
        ))
        assertEquals(AndroidDiagnosticFamily.THERMAL, result.primary?.family)
        assertTrue(result.candidates.zipWithNext().all { (a, b) -> a.confidence >= b.confidence })
    }

    @Test
    fun correlationRequiresSameSourcePathAndConfiguredWindow() {
        val strict = DeterministicAndroidDiagnosticEngine(correlationWindowLines = 5)
        val result = strict.diagnose(AndroidEvidenceReport(
            sourceFilename = "bugreport.zip",
            sourcePath = "bugreport.txt",
            evidence = listOf(
                evidence(AndroidEvidenceKind.KERNEL_PANIC, 10, path = "a.txt"),
                evidence(AndroidEvidenceKind.WATCHDOG, 12, path = "b.txt")
            )
        ))
        assertEquals(AndroidDiagnosticCategory.INDETERMINATE, result.primary?.category)
        assertFailsWith<IllegalArgumentException> { DeterministicAndroidDiagnosticEngine(-1) }
    }

    @Test
    fun candidateContractsRejectInvalidValues() {
        val ev = evidence(AndroidEvidenceKind.CAMERA_HAL, 1)
        assertFailsWith<IllegalArgumentException> {
            AndroidDiagnosticCandidate("", AndroidDiagnosticFamily.CAMERA, AndroidDiagnosticCategory.INDETERMINATE, 0.5, "reason", listOf(ev))
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidDiagnosticCandidate("R", AndroidDiagnosticFamily.CAMERA, AndroidDiagnosticCategory.INDETERMINATE, 1.1, "reason", listOf(ev))
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidDiagnosticCandidate("R", AndroidDiagnosticFamily.CAMERA, AndroidDiagnosticCategory.INDETERMINATE, 0.5, "", listOf(ev))
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidDiagnosticCandidate("R", AndroidDiagnosticFamily.CAMERA, AndroidDiagnosticCategory.INDETERMINATE, 0.5, "reason", emptyList())
        }
    }

    private fun report(vararg evidence: AndroidEvidence): AndroidEvidenceReport = AndroidEvidenceReport(
        sourceFilename = "bugreport.zip",
        sourcePath = "bugreport.txt",
        evidence = evidence.toList()
    )

    private fun evidence(
        kind: AndroidEvidenceKind,
        line: Int,
        excerpt: String = kind.name,
        path: String = "bugreport.txt"
    ): AndroidEvidence = AndroidEvidence(
        code = "ANDROID.EVIDENCE.${kind.name}",
        kind = kind,
        subsystem = when (kind) {
            AndroidEvidenceKind.KERNEL_PANIC, AndroidEvidenceKind.WATCHDOG -> AndroidSubsystem.KERNEL
            AndroidEvidenceKind.CAMERA_HAL -> AndroidSubsystem.CAMERA
            AndroidEvidenceKind.MODEM_RADIO -> AndroidSubsystem.RADIO
            AndroidEvidenceKind.THERMAL -> AndroidSubsystem.THERMAL
            AndroidEvidenceKind.STORAGE_IO -> AndroidSubsystem.STORAGE
            AndroidEvidenceKind.TOMBSTONE, AndroidEvidenceKind.NATIVE_CRASH -> AndroidSubsystem.NATIVE
            AndroidEvidenceKind.ANR, AndroidEvidenceKind.JAVA_CRASH -> AndroidSubsystem.APPLICATION
        },
        severity = when (kind) {
            AndroidEvidenceKind.KERNEL_PANIC, AndroidEvidenceKind.WATCHDOG -> AndroidEvidenceSeverity.CRITICAL
            AndroidEvidenceKind.THERMAL, AndroidEvidenceKind.ANR -> AndroidEvidenceSeverity.WARNING
            else -> AndroidEvidenceSeverity.ERROR
        },
        source = AndroidEvidenceSource.BUGREPORT,
        sourcePath = path,
        lineNumber = line,
        excerpt = excerpt
    )
}
