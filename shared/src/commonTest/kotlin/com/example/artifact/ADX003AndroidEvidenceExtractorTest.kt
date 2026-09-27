package com.example.artifact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ADX003AndroidEvidenceExtractorTest {
    private val extractor = AndroidEvidenceExtractor()

    @Test
    fun extractsDeterministicEvidenceWithoutDiagnosis() {
        val report = extractor.extractText(
            sourceFilename = "bugreport-test.zip",
            sourcePath = "bugreport-device.txt",
            text = FIXTURE
        )

        assertTrue(report.has(AndroidEvidenceKind.KERNEL_PANIC))
        assertTrue(report.has(AndroidEvidenceKind.WATCHDOG))
        assertTrue(report.has(AndroidEvidenceKind.TOMBSTONE))
        assertTrue(report.has(AndroidEvidenceKind.NATIVE_CRASH))
        assertTrue(report.has(AndroidEvidenceKind.ANR))
        assertTrue(report.has(AndroidEvidenceKind.JAVA_CRASH))
        assertTrue(report.has(AndroidEvidenceKind.THERMAL))
        assertTrue(report.has(AndroidEvidenceKind.STORAGE_IO))
        assertTrue(report.has(AndroidEvidenceKind.MODEM_RADIO))
        assertTrue(report.has(AndroidEvidenceKind.CAMERA_HAL))

        assertFalse(report.evidence.any { it.excerpt.contains("hardware probable", ignoreCase = true) })
        assertTrue(report.evidence.zipWithNext().all { (a, b) -> a.lineNumber <= b.lineNumber })
    }

    @Test
    fun assignsSourceSectionsAndTimestamps() {
        val report = extractor.extractText("bugreport.txt", "bugreport.txt", FIXTURE)

        val javaCrash = report.evidence.first { it.kind == AndroidEvidenceKind.JAVA_CRASH }
        assertEquals(AndroidEvidenceSource.LOGCAT, javaCrash.source)
        assertEquals("09-27 10:00:01.123", javaCrash.timestamp)
        assertEquals("SYSTEM LOG", javaCrash.sourceSection)

        val kernel = report.evidence.first { it.kind == AndroidEvidenceKind.KERNEL_PANIC }
        assertEquals(AndroidEvidenceSource.KERNEL, kernel.source)
        assertNotNull(kernel.excerpt)
    }

    @Test
    fun deduplicatesIdenticalLinesAndHonorsPerKindLimit() {
        val limited = AndroidEvidenceExtractor(maxPerKind = 2, contextRadius = 0)
        val text = buildString {
            appendLine("------ SYSTEM LOG ------")
            repeat(5) { appendLine("09-27 10:00:00.000 E AndroidRuntime: FATAL EXCEPTION: main") }
            appendLine("09-27 10:00:01.000 E AndroidRuntime: FATAL EXCEPTION: worker")
            appendLine("09-27 10:00:02.000 E AndroidRuntime: FATAL EXCEPTION: sync")
        }

        val report = limited.extractText("bugreport.txt", "bugreport.txt", text)

        assertEquals(2, report.count(AndroidEvidenceKind.JAVA_CRASH))
    }

    @Test
    fun emptyInputProducesEmptyEvidenceReport() {
        val report = extractor.extractText("bugreport.txt", "bugreport.txt", "   \n")
        assertTrue(report.evidence.isEmpty())
    }

    @Test
    fun extractsFromImportedDiagnosticSession() {
        val session = AndroidDiagnosticSession(
            sourceFilename = "bugreport.zip",
            primaryReportPath = "bugreport-device.txt",
            primaryReportText = "------ RADIO LOG ------\n09-27 11:01:02.003 E RIL: modem reset after timeout"
        )

        val report = extractor.extract(session)

        assertEquals(1, report.count(AndroidEvidenceKind.MODEM_RADIO))
        assertEquals("bugreport-device.txt", report.sourcePath)
    }

    @Test
    fun evidenceModelRejectsInvalidLineNumber() {
        val failed = runCatching {
            AndroidEvidence(
                code = "ANDROID.EVIDENCE.TEST",
                kind = AndroidEvidenceKind.ANR,
                subsystem = AndroidSubsystem.APPLICATION,
                severity = AndroidEvidenceSeverity.WARNING,
                source = AndroidEvidenceSource.BUGREPORT,
                sourcePath = "bugreport.txt",
                lineNumber = 0,
                excerpt = "ANR in example"
            )
        }.isFailure

        assertTrue(failed)
    }

    private companion object {
        val FIXTURE = """
            == dumpstate: 2026-09-27 10:00:00
            ------ KERNEL LOG ------
            2026-09-27 10:00:00.010 kernel panic - not syncing: Fatal exception
            watchdog detected hard lockup on CPU 3
            ------ TOMBSTONES ------
            *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
            signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0
            backtrace:
            ------ ANR TRACE ------
            ANR in com.example.camera
            ------ SYSTEM LOG ------
            09-27 10:00:01.123 E AndroidRuntime: FATAL EXCEPTION: main
            09-27 10:00:02.124 E ThermalEngine: thermal critical throttling engaged
            09-27 10:00:03.125 E kernel: EXT4-fs error (device dm-8): ext4_find_entry
            ------ RADIO LOG ------
            09-27 10:00:04.126 E RIL: modem reset after timeout
            ------ DUMPSYS CAMERA ------
            Camera HAL error: provider not responding
        """.trimIndent()
    }
}
