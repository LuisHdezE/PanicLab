package com.example.util

import com.example.diagnostic.SensorExtractor
import com.example.domain.model.PanicFamily
import com.example.parser.LogNormalizer
import com.example.parser.MetadataExtractor
import com.example.parser.PanicClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanicLogTextDecoderTest {

    @Test
    fun decodesUtf16LittleEndianWhenBomIsPresent() {
        val text = """{"product":"iPhone14,7","panicString":"SMC PANIC - SMC BSC failure"}"""
        val payload = text.toByteArray(Charsets.UTF_16LE)
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + payload

        val decoded = PanicLogTextDecoder.decode(bytes)

        assertEquals(text, decoded)
    }

    @Test
    fun decodesUtf16BigEndianWhenBomIsPresent() {
        val text = """{"product":"iPhone14,7","panicString":"SMC PANIC"}"""
        val payload = text.toByteArray(Charsets.UTF_16BE)
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + payload

        val decoded = PanicLogTextDecoder.decode(bytes)

        assertEquals(text, decoded)
    }

    @Test
    fun removesUtf8Bom() {
        val text = """{"product":"iPhone12,8"}"""
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            text.toByteArray(Charsets.UTF_8)

        assertEquals(text, PanicLogTextDecoder.decode(bytes))
    }

    @Test
    fun normalizesLegacyCharacterSpacedPanicLog() {
        val spaced =
            """{ " b u g _ t y p e " : " 2 1 0 " , " p r o d u c t " : " i P h o n e 1 2 , 8 " , " p a n i c S t r i n g " : " M i s s i n g   s e n s o r ( s ) :   m i c 1 " }"""

        val decoded = PanicLogTextDecoder.decode(spaced.toByteArray(Charsets.UTF_8))

        assertTrue(decoded.contains("\"bug_type\":\"210\""))
        assertTrue(decoded.contains("\"product\":\"iPhone12,8\""))
        assertTrue(decoded.contains("\"panicString\":\"Missing sensor(s): mic1\""))
    }

    @Test
    fun leavesNormalUtf8LogUntouched() {
        val text =
            """{"product":"iPhone12,8","panicString":"userspace watchdog timeout: Missing sensor(s): mic1"}"""

        assertEquals(text, PanicLogTextDecoder.decode(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun largePanicLogIsBoundedWithoutLosingHeaderOrTail() {
        val header =
            """{"product":"iPhone14,7","panicString":"SMC PANIC - SMC BSC failure - S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0"}"""
        val tail = "Debugger message: panic"
        val oversized = header + "\n" + "x".repeat(400_000) + "\n" + tail

        val analysisText = PanicLogAnalysisWindow.forAnalysis(oversized)

        assertTrue(analysisText.length < oversized.length)
        assertTrue(analysisText.contains("iPhone14,7"))
        assertTrue(analysisText.contains("4194304"))
        assertTrue(analysisText.contains(tail))
    }

    @Test
    fun largeUtf16PanicLogCanBeDecodedAndBoundedForAnalysis() {
        val header =
            """{"product":"iPhone14,7","panicString":"SMC PANIC - SMC BSC failure - S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0"}"""
        val text = header + "\n" + "kernel_task ".repeat(45_000) + "\nDebugger message: panic"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)

        val decoded = PanicLogTextDecoder.decode(bytes)
        val analysisText = PanicLogAnalysisWindow.forAnalysis(decoded)

        assertTrue(analysisText.length <= PanicLogAnalysisWindow.MAX_ANALYSIS_CHARS + 100)
        assertTrue(analysisText.contains("iPhone14,7"))
        assertTrue(analysisText.contains("4194304"))
        assertTrue(analysisText.contains("Debugger message: panic"))
    }

    @Test
    fun largeUtf16AppleStyleLogSurvivesDecodeWindowAndDeterministicParsing() {
        val header =
            """{"bug_type":"210","timestamp":"2026-08-26 10:30:36.00 -0300","os_version":"iPhone OS 26.5 (23F77)"}"""
        val panicObjectPrefix =
            "{\"product\":\"iPhone14,7\",\"panicString\":\"SMC PANIC - SMC BSC failure\\nS.sensor array 0 - 5 is 0, 4194304, 0, 0, 0\",\"payload\":\""
        val text = header + "\n" + panicObjectPrefix + "x".repeat(450_000) + "\"}"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)

        val decoded = PanicLogTextDecoder.decode(bytes)
        val analysisText = PanicLogAnalysisWindow.forAnalysis(decoded)
        val normalized = LogNormalizer.normalize(analysisText)
        val metadata = MetadataExtractor.extract(normalized)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized, metadata.panicString)

        assertEquals("iPhone14,7", metadata.product)
        assertTrue(PanicFamily.SMC_BSC_FAILURE in families)
        assertTrue(sensors.sensorCodes.any { it.numericValue == 4_194_304L })
    }

    @Test
    fun smallPanicLogIsNotChangedByAnalysisWindow() {
        val text = """{"product":"iPhone14,7","panicString":"SMC PANIC"}"""

        assertEquals(text, PanicLogAnalysisWindow.forAnalysis(text))
    }
}
