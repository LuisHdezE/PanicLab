package com.example.shared

import com.example.ios.NativeOcrFacade
import com.example.ocr.OcrLogExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeOcrFacadeTest {
    @Test
    fun nativeFacadeMapsCommonOcrSemanticsWithoutChangingThem() {
        val raw = "iPhone14,7\nSMC PANIC\nS.sensor array is 0x0, 0x500000, 0x0"

        val common = OcrLogExtractor.processScannedText(raw)
        val native = NativeOcrFacade().process(raw)

        assertEquals(common.rawText, native.rawText)
        assertEquals(common.cleanedText, native.cleanedText)
        assertEquals(common.detectedDeviceModel, native.detectedDeviceModel)
        assertEquals(common.detectedBuild, native.detectedBuild)
        assertEquals(common.detectedPanicCodes.joinToString("\n"), native.panicCodesText)
        assertEquals(common.detectedKeywords.joinToString("\n"), native.keywordsText)
        assertEquals(common.lineCount, native.lineCount)
        assertEquals(common.hasValidPanicSignatures, native.hasValidPanicSignatures)
        assertEquals(common.confidenceHint, native.confidenceHint)
        assertTrue(native.hasValidPanicSignatures)
        assertTrue(native.cleanedText.contains("0x500000"))
    }

    @Test
    fun nativeFacadePreservesSafeNoTextResult() {
        val result = NativeOcrFacade().process("   \n\t")

        assertEquals("", result.rawText)
        assertEquals("", result.cleanedText)
        assertEquals(0, result.lineCount)
        assertFalse(result.hasValidPanicSignatures)
        assertEquals("Sin texto detectado", result.confidenceHint)
    }
}
