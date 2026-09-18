package com.example

import com.example.diagnostic.DiagnosticReportBuilder
import com.example.diagnostic.ExtractedSensors
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.ParsedMetadata
import com.example.parser.EvidenceExtractor
import com.example.platform.Clock
import com.example.platform.IdGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

class NondeterminismSeamsTest {

    @Test
    fun reportBuilderUsesInjectedClockAndId() {
        val report = DiagnosticReportBuilder.build(
            sourceFilename = "fixture.ips",
            rawLog = "panic fixture",
            metadata = ParsedMetadata(product = "iPhone14,4"),
            deviceModel = null,
            panicFamilies = emptyList(),
            evidences = emptyList(),
            primaryCandidate = null,
            alternativeCandidates = emptyList(),
            kbVersion = "fixture-kb",
            saveRawLogsPreference = false,
            clock = Clock { 1_711_000_000_000L },
            idGenerator = IdGenerator { "report-fixed-id" }
        )

        assertEquals("report-fixed-id", report.id)
        assertEquals(1_711_000_000_000L, report.createdAt)
        assertEquals(ConfidenceLevel.UNKNOWN, report.confidence)
    }

    @Test
    fun evidenceExtractorUsesInjectedIdSequence() {
        val ids = listOf("evidence-missing", "evidence-smc").iterator()
        val evidences = EvidenceExtractor.extractEvidences(
            logText = "Missing sensor(s): PRS0\nS.sensor array 0 - 0 is 4096",
            metadata = ParsedMetadata(),
            panicFamilies = emptyList(),
            extractedSensors = ExtractedSensors(
                missingSensorTokens = listOf("PRS0"),
                smcSensorCodes = listOf("4096")
            ),
            idGenerator = IdGenerator { ids.next() }
        )

        assertEquals(listOf("evidence-missing", "evidence-smc"), evidences.map { it.id })
    }
}
