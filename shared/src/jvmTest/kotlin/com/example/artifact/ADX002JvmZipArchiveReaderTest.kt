package com.example.artifact

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ADX002JvmZipArchiveReaderTest {
    private val reader = JvmZipArchiveReader()

    @Test
    fun readsRealBugreportZipAndFeedsImporter() {
        val zip = zipOf(
            "bugreport-device-build.txt" to bugreportText,
            "FS/data/anr/anr_001.txt" to anrText
        )

        val session = AndroidBugReportImporter().importArchive("bugreport.zip", zip, reader)

        assertEquals("bugreport-device-build.txt", session.primaryReportPath)
        assertEquals("Example Phone", session.model)
        assertEquals(2, session.artifacts.size)
        assertEquals(
            DiagnosticArtifactType.ANDROID_ANR,
            session.artifacts.first { it.path.contains("anr_001") }.type
        )
    }

    @Test
    fun rejectsZipSlipPath() {
        val error = assertFailsWith<BugReportImportException> {
            reader.read(zipOf("../escape.txt" to "unsafe"))
        }
        assertEquals(BugReportImportFailureReason.UNSAFE_ENTRY_PATH, error.reason)
    }

    @Test
    fun rejectsEntryAndExpandedSizeLimits() {
        val zip = zipOf("large.txt" to "x".repeat(2048))
        val entryError = assertFailsWith<BugReportImportException> {
            reader.read(zip, BugReportImportLimits(maxEntryBytes = 1024))
        }
        assertEquals(BugReportImportFailureReason.ENTRY_TOO_LARGE, entryError.reason)

        val expandedError = assertFailsWith<BugReportImportException> {
            reader.read(zip, BugReportImportLimits(maxExpandedBytes = 1024))
        }
        assertEquals(BugReportImportFailureReason.EXPANDED_SIZE_LIMIT, expandedError.reason)
    }

    @Test
    fun rejectsEntryCountLimit() {
        val zip = zipOf("a.txt" to "a", "b.txt" to "b")
        val error = assertFailsWith<BugReportImportException> {
            reader.read(zip, BugReportImportLimits(maxEntries = 1))
        }
        assertEquals(BugReportImportFailureReason.TOO_MANY_ENTRIES, error.reason)
    }

    @Test
    fun rejectsSuspiciousCompressionRatio() {
        val zip = zipOf("repetitive.txt" to "A".repeat(64 * 1024))
        val error = assertFailsWith<BugReportImportException> {
            reader.read(zip, BugReportImportLimits(maxCompressionRatio = 2.0))
        }
        assertEquals(BugReportImportFailureReason.SUSPICIOUS_COMPRESSION_RATIO, error.reason)
    }

    @Test
    fun rejectsNonZipEnvelope() {
        val error = assertFailsWith<BugReportImportException> {
            reader.read("not a zip".encodeToByteArray())
        }
        assertEquals(BugReportImportFailureReason.MALFORMED_ARCHIVE, error.reason)
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.encodeToByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private val bugreportText = """
        == dumpstate: 2026-09-27 10:00:00
        Build fingerprint: 'example/device/device:16/BUILD/123:user/release-keys'
        [ro.product.model]: [Example Phone]
        [ro.build.version.release]: [16]
        ------ SYSTEM LOG ------
    """.trimIndent()

    private val anrText = """
        ----- pid 123 at 2026-09-27 10:01:00 -----
        Cmd line: com.example.app
        DALVIK THREADS (4):
        held mutexes=
    """.trimIndent()
}
