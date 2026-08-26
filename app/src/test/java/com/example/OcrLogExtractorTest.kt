package com.example

import com.example.ocr.OcrLogExtractor
import org.junit.Assert.*
import org.junit.Test

class OcrLogExtractorTest {

    @Test
    fun testProcessScannedText_extractsHexCodesAndDeviceModel() {
        val ocrSample = """
            iPhone 14 Pro
            Build: 21D61
            panic(cpu 0 caller 0xfffffff011223344): SMC PANIC - BSC failure at address 0x400000
            S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0
        """.trimIndent()

        val result = OcrLogExtractor.processScannedText(ocrSample)

        assertTrue("Should detect valid panic signatures", result.hasValidPanicSignatures)
        assertTrue("Should detect 0x400000 hex code", result.detectedPanicCodes.contains("0X400000") || result.detectedPanicCodes.contains("0x400000".uppercase()))
        assertNotNull("Should detect iPhone 14 Pro", result.detectedDeviceModel)
        assertTrue("Should contain SMC BSC Failure keyword", result.detectedKeywords.any { it.contains("SMC BSC Failure") })
        assertTrue("Should contain Build keyword", result.detectedKeywords.any { it.contains("21D61") })
    }

    @Test
    fun testProcessScannedText_repairsOcrOpticalArtifacts() {
        // Simulating common OCR mistakes: capital 'O' instead of 0 in 'Ox4OOOOO', '0xl000' with lowercase 'l'
        val rawArtifactOcr = """
            panic(cpu 1): SMC PANIC - BSC failure at address Ox4OOOOO
            S . sensor array: 0x0 0xl000 0x0
            Product: iPhone13,2
        """.trimIndent()

        val result = OcrLogExtractor.processScannedText(rawArtifactOcr)

        assertTrue(result.hasValidPanicSignatures)
        // Cleaned text should have repaired 0x400000 and 0x1000
        assertTrue(result.cleanedText.contains("0x400000", ignoreCase = true))
        assertTrue(result.cleanedText.contains("0x1000", ignoreCase = true))
        assertTrue(result.cleanedText.contains("S.sensor array", ignoreCase = true))
    }

    @Test
    fun testProcessScannedText_detectsDecimalSensorCodes() {
        val decimalOcr = """
            S.sensor array 0 - 5 is 0, 524288, 0, 0, 0
            SMC PANIC
        """.trimIndent()

        val result = OcrLogExtractor.processScannedText(decimalOcr)

        assertTrue(result.hasValidPanicSignatures)
        // 524288 in decimal is 0x80000
        assertTrue("Should extract 0x80000 from decimal 524288", result.detectedPanicCodes.any { it.equals("0x80000", ignoreCase = true) })
    }

    @Test
    fun testProcessScannedText_emptyTextReturnsGracefulResult() {
        val result = OcrLogExtractor.processScannedText("")
        assertFalse(result.hasValidPanicSignatures)
        assertEquals(0, result.lineCount)
        assertTrue(result.detectedPanicCodes.isEmpty())
    }
}
