package com.example.shared

import com.example.parser.MetadataExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetadataExtractorStackSafetyTest {

    @Test
    fun largeConcatenatedApplePanicJsonDoesNotOverflowRegexStack() {
        val escapedMailbox = buildString {
            repeat(6_000) { index ->
                append("[RX] user")
                append(index)
                append(" 0x0000000110b28abb 0x000000000008b000 0x0010230000000020\\n")
            }
        }

        val rawLog = buildString {
            append("{\"bug_type\":\"210\",\"timestamp\":\"2026-08-26 10:30:36.00 -0300\"}\n")
            append("{\"product\":\"iPhone14,7\",\"panicString\":\"")
            append("SMC PANIC - ASSERT: target/d27/target.cpp:321: 0, SMC BSC failure, TAOJ ----\\n")
            append(escapedMailbox)
            append("S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0\"}")
        }

        assertTrue(rawLog.length > 400_000)

        val metadata = MetadataExtractor.extract(rawLog)
        val panicString = assertNotNull(metadata.panicString)

        assertEquals("iPhone14,7", metadata.product)
        assertTrue(panicString.startsWith("SMC PANIC - ASSERT"))
        assertTrue(panicString.contains("SMC BSC failure"))
        assertTrue(panicString.contains("S.sensor array 0 - 5 is 0, 4194304"))
    }
}
