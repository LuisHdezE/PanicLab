package com.example.artifact

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ADX002AndroidZipArchiveReaderHostTest {
    private val reader = AndroidZipArchiveReader()

    @Test
    fun androidReaderParsesBoundedArchive() {
        val archive = zipOf(
            "bugreport-device-build.txt" to """
                == dumpstate: 2026-09-27 10:00:00
                Build fingerprint: 'example/device/device:16/BUILD/123:user/release-keys'
                ------ SYSTEM LOG ------
            """.trimIndent()
        )

        val entries = reader.read(archive)

        assertEquals(1, entries.size)
        assertEquals("bugreport-device-build.txt", entries.single().path)
    }

    @Test
    fun androidReaderRejectsTraversal() {
        val error = assertFailsWith<BugReportImportException> {
            reader.read(zipOf("safe/../../escape.txt" to "unsafe"))
        }
        assertEquals(BugReportImportFailureReason.UNSAFE_ENTRY_PATH, error.reason)
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
}
