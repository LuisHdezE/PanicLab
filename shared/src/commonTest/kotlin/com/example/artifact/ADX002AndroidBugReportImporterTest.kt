package com.example.artifact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ADX002AndroidBugReportImporterTest {
    private val importer = AndroidBugReportImporter()

    @Test
    fun importsPlainTextAndExtractsDeviceMetadata() {
        val session = importer.importText("bugreport-example.txt", Fixtures.primary)

        assertEquals("Google", session.manufacturer)
        assertEquals("Pixel", session.brand)
        assertEquals("Pixel 9", session.model)
        assertEquals("tokay", session.product)
        assertEquals("tokay", session.device)
        assertEquals("16", session.androidRelease)
        assertEquals("36", session.sdk)
        assertEquals("BP2A.260101.001", session.buildId)
        assertEquals("2026-09-05", session.securityPatch)
        assertEquals("2026-09-27 09:30:00", session.reportTimestamp)
        assertEquals("google/tokay/tokay:16/BP2A.260101.001/123456:user/release-keys", session.buildFingerprint)
        assertEquals(DiagnosticArtifactType.ANDROID_BUGREPORT, session.artifacts.single().type)
    }

    @Test
    fun importsArchiveAndPrefersCanonicalBugreportName() {
        val reader = FakeArchiveReader(
            listOf(
                BugReportArchiveEntry("logs/logcat.txt", Fixtures.logcat.encodeToByteArray()),
                BugReportArchiveEntry("bugreport-tokay-BP2A.txt", Fixtures.primary.encodeToByteArray()),
                BugReportArchiveEntry("FS/data/anr/anr_001.txt", Fixtures.anr.encodeToByteArray())
            )
        )

        val session = importer.importArchive("report.zip", byteArrayOf(1, 2, 3), reader)

        assertEquals("bugreport-tokay-BP2A.txt", session.primaryReportPath)
        assertEquals(3, session.artifacts.size)
        assertEquals(DiagnosticArtifactType.ANDROID_LOGCAT, session.artifacts.first { it.path.contains("logcat") }.type)
        assertEquals(DiagnosticArtifactType.ANDROID_ANR, session.artifacts.first { it.path.contains("anr_") }.type)
    }

    @Test
    fun archiveWithoutBugreportFailsClosed() {
        val reader = FakeArchiveReader(
            listOf(BugReportArchiveEntry("logcat.txt", Fixtures.logcat.encodeToByteArray()))
        )

        val error = assertFailsWith<BugReportImportException> {
            importer.importArchive("report.zip", byteArrayOf(1), reader)
        }

        assertEquals(BugReportImportFailureReason.PRIMARY_REPORT_NOT_FOUND, error.reason)
    }

    @Test
    fun plainTextThatIsNotBugreportIsRejected() {
        val error = assertFailsWith<BugReportImportException> {
            importer.importText("notes.txt", "technician notes only")
        }
        assertEquals(BugReportImportFailureReason.UNSUPPORTED_ARTIFACT, error.reason)
    }

    @Test
    fun archiveSizeIsCheckedBeforeReaderIsInvoked() {
        var invoked = false
        val reader = object : BugReportArchiveReader {
            override fun read(bytes: ByteArray, limits: BugReportImportLimits): List<BugReportArchiveEntry> {
                invoked = true
                return emptyList()
            }
        }

        val error = assertFailsWith<BugReportImportException> {
            importer.importArchive(
                "huge.zip",
                ByteArray(5),
                reader,
                BugReportImportLimits(maxArchiveBytes = 4)
            )
        }

        assertEquals(BugReportImportFailureReason.ARCHIVE_TOO_LARGE, error.reason)
        assertTrue(!invoked)
    }

    @Test
    fun pathValidatorRejectsTraversalAbsoluteAndWindowsPaths() {
        listOf("../escape.txt", "/absolute.txt", "C:/escape.txt", "safe/../../escape.txt").forEach { path ->
            val error = assertFailsWith<BugReportImportException> { validateArchiveEntryPath(path) }
            assertEquals(BugReportImportFailureReason.UNSAFE_ENTRY_PATH, error.reason)
        }
        validateArchiveEntryPath("FS/data/anr/anr_001.txt")
    }

    private class FakeArchiveReader(
        private val entries: List<BugReportArchiveEntry>
    ) : BugReportArchiveReader {
        override fun read(bytes: ByteArray, limits: BugReportImportLimits): List<BugReportArchiveEntry> = entries
    }

    private object Fixtures {
        val primary = """
            ========================================================
            == dumpstate: 2026-09-27 09:30:00
            ========================================================
            Build fingerprint: 'google/tokay/tokay:16/BP2A.260101.001/123456:user/release-keys'
            [ro.product.manufacturer]: [Google]
            [ro.product.brand]: [Pixel]
            [ro.product.model]: [Pixel 9]
            [ro.product.name]: [tokay]
            [ro.product.device]: [tokay]
            [ro.build.version.release]: [16]
            [ro.build.version.sdk]: [36]
            [ro.build.id]: [BP2A.260101.001]
            [ro.build.version.security_patch]: [2026-09-05]
            Linux version: 6.1.99-android16-test
            ------ SYSTEM LOG ------
        """.trimIndent()

        val logcat = """
            --------- beginning of main
            09-27 09:30:01.100  1000  1000 E Camera : failure
            09-27 09:30:01.200  1000  1000 I Camera : restart
        """.trimIndent()

        val anr = """
            ----- pid 123 at 2026-09-27 09:31:00 -----
            Cmd line: com.example.camera
            DALVIK THREADS (10):
            "main" prio=5 tid=1 Waiting
            held mutexes=
        """.trimIndent()
    }
}
