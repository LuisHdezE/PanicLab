package com.example.androiddiagnostic

import com.example.artifact.BugReportArchiveEntry
import com.example.artifact.BugReportArchiveReader
import com.example.artifact.BugReportImportLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ADX005AndroidBugReportAnalyzerTest {
    private val analyzer = AndroidBugReportAnalyzer()

    @Test
    fun analyzesTextThroughTheCompleteDeterministicPipeline() {
        val text = bugReport(
            "------ RADIO LOG ------",
            "09-27 11:01:02.003 E RIL: modem reset after timeout",
            "09-27 11:01:03.003 E RIL: modem failed after reset"
        )

        val result = analyzer.analyzeText("bugreport-pixel.txt", text)

        assertEquals("bugreport-pixel.txt", result.session.sourceFilename)
        assertEquals("Google", result.session.manufacturer)
        assertEquals("Pixel 8", result.session.model)
        assertTrue(result.evidenceReport.evidence.isNotEmpty())
        assertNotNull(result.diagnosticReport.primary)
        assertEquals(AndroidDiagnosticFamily.MODEM_RADIO, result.diagnosticReport.primary?.family)
    }

    @Test
    fun analyzesArchiveUsingTheProvidedSafeArchiveReader() {
        val text = bugReport(
            "------ SYSTEM LOG ------",
            "09-27 11:01:02.003 E CameraProvider: camera provider failed timeout",
            "09-27 11:01:03.003 E CameraProvider: camera hal failed timeout"
        )
        val reader = object : BugReportArchiveReader {
            override fun read(bytes: ByteArray, limits: BugReportImportLimits): List<BugReportArchiveEntry> =
                listOf(BugReportArchiveEntry("bugreport-device.txt", text.encodeToByteArray()))
        }

        val result = analyzer.analyzeArchive(
            sourceFilename = "bugreport-device.zip",
            bytes = byteArrayOf(1, 2, 3),
            archiveReader = reader
        )

        assertEquals("bugreport-device.txt", result.session.primaryReportPath)
        assertTrue(result.evidenceReport.evidence.isNotEmpty())
        assertEquals(AndroidDiagnosticFamily.CAMERA, result.diagnosticReport.primary?.family)
    }

    @Test
    fun preservesAnIndeterminateCompletedAnalysisWhenNoDiagnosticEvidenceExists() {
        val result = analyzer.analyzeText(
            "bugreport-clean.txt",
            bugReport("------ SYSTEM LOG ------", "09-27 11:01:02.003 I ActivityManager: system ready")
        )

        assertTrue(result.evidenceReport.evidence.isEmpty())
        assertTrue(result.diagnosticReport.candidates.isEmpty())
        assertEquals(null, result.diagnosticReport.primary)
    }

    private fun bugReport(vararg lines: String): String = buildString {
        appendLine("========================================================")
        appendLine("== dumpstate: 2026-09-27 11:01:00")
        appendLine("========================================================")
        appendLine("Build fingerprint: 'google/shiba/shiba:16/BP2A.260927.001/123456:user/release-keys'")
        appendLine("[ro.product.manufacturer]: [Google]")
        appendLine("[ro.product.model]: [Pixel 8]")
        appendLine("[ro.build.version.release]: [16]")
        appendLine("[ro.build.version.sdk]: [36]")
        appendLine("[ro.build.id]: [BP2A.260927.001]")
        appendLine("[ro.build.version.security_patch]: [2026-09-01]")
        lines.forEach(::appendLine)
    }
}
