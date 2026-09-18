package com.example.shared

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.PanicCode
import com.example.domain.model.RepairFlow
import com.example.domain.model.VerificationStatus
import com.example.parser.LogNormalizer
import com.example.platform.Clock
import com.example.platform.IdGenerator
import com.example.util.HexUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortableCoreTest {
    @Test
    fun panicCodesPreserveDecimalAndHexSemantics() {
        val decimal = PanicCode.parse("3145728")
        val hexadecimal = PanicCode.parse("0x300000")

        assertEquals(3145728L, decimal?.numericValue)
        assertEquals("0x300000", decimal?.hexadecimal)
        assertEquals(decimal?.numericValue, hexadecimal?.numericValue)
        assertEquals("3145728", PanicCode.fromNumeric(3145728L).decimal)
    }

    @Test
    fun hexEquivalenceFixtureValuesRemainStable() {
        val fixtures = listOf(
            "0x1000" to "4096",
            "0x80000" to "524288",
            "0x100000" to "1048576",
            "0x200000" to "2097152",
            "0x300000" to "3145728",
            "0x400000" to "4194304",
            "0x500000" to "5242880"
        )

        fixtures.forEach { (hex, decimal) ->
            assertTrue(HexUtils.areCodesEquivalent(hex, decimal), "$hex must equal $decimal")
        }
    }

    @Test
    fun hexParsingCoversPortableBranches() {
        assertNull(HexUtils.parseCodeToLong("   "))
        assertEquals(255L, HexUtils.parseCodeToLong("0Xff"))
        assertEquals(1000L, HexUtils.parseCodeToLong("1000"))
        assertEquals(255L, HexUtils.parseCodeToLong("FF"))
        assertNull(HexUtils.parseCodeToLong("not-a-code"))
        assertNull(HexUtils.parseCodeToLong("0x"))
        assertNull(HexUtils.parseCodeToLong("999999999999999999999999999"))
        assertEquals("0xFF", HexUtils.toCanonicalHex("ff"))
        assertEquals("bad-code", HexUtils.toCanonicalHex(" bad-code "))
        assertEquals("255", HexUtils.toDecimalString("0xFF"))
        assertEquals("bad-code", HexUtils.toDecimalString(" bad-code "))
        assertTrue(HexUtils.areCodesEquivalent("0xFF", "0Xff"))
        assertFalse(HexUtils.areCodesEquivalent("invalid", "0x1"))
        assertTrue(HexUtils.hasBits(0x300000, 0x100000))
        assertFalse(HexUtils.hasBits(0x100000, 0x200000))
        assertFalse(HexUtils.hasBits(0x100000, 0L))
    }

    @Test
    fun logNormalizerPreservesBaselineCleanup() {
        assertEquals("", LogNormalizer.normalize("  \t  "))
        assertEquals("line1\nline2\t\"quoted\"", LogNormalizer.normalize("line1\\nline2\\t\\\"quoted\\\""))
        assertEquals("line1\nline2\nline3", LogNormalizer.normalize("  line1\r\nline2\rline3  "))
        assertEquals("line1\\n\nline2", LogNormalizer.normalize("line1\\n\nline2"))
    }

    @Test
    fun nondeterminismContractsAcceptFixedProviders() {
        val clock = Clock { 1_711_000_000_000L }
        val ids = listOf("evidence-1", "report-1").iterator()
        val idGenerator = IdGenerator { ids.next() }

        assertEquals(1_711_000_000_000L, clock.nowEpochMillis())
        assertEquals("evidence-1", idGenerator.nextId())
        assertEquals("report-1", idGenerator.nextId())
    }

    @Test
    fun diagnosticReportRequiresExplicitTimeInsteadOfReadingWallClock() {
        val report = DiagnosticReport(
            id = "report-fixed",
            createdAt = 1_711_000_000_000L,
            deviceModel = null,
            productCode = "iPhone14,4",
            osVersion = "17.0",
            build = "21A329",
            panicFamilies = emptyList(),
            panicStringSummary = "fixture",
            evidences = emptyList(),
            primaryCandidate = null,
            alternativeCandidates = emptyList(),
            confidence = ConfidenceLevel.UNKNOWN,
            verificationStatus = VerificationStatus.UNKNOWN,
            repairFlow = RepairFlow(),
            knowledgeBaseVersion = "fixture"
        )

        assertEquals(1_711_000_000_000L, report.createdAt)
        assertEquals("report-fixed", report.id)
    }
}
