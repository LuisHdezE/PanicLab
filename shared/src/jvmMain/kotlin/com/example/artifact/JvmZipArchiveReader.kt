package com.example.artifact

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class JvmZipArchiveReader : BugReportArchiveReader {
    override fun read(bytes: ByteArray, limits: BugReportImportLimits): List<BugReportArchiveEntry> {
        validateArchiveEnvelope(bytes, limits)
        val result = mutableListOf<BugReportArchiveEntry>()
        var expandedTotal = 0L

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }
                    if (result.size >= limits.maxEntries) {
                        fail(BugReportImportFailureReason.TOO_MANY_ENTRIES, "Archive exceeds ${limits.maxEntries} entries")
                    }

                    validateArchiveEntryPath(entry.name)
                    if (entry.size > limits.maxEntryBytes) {
                        fail(BugReportImportFailureReason.ENTRY_TOO_LARGE, "ZIP entry exceeds ${limits.maxEntryBytes} bytes: ${entry.name}")
                    }

                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var entryExpanded = 0L
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        entryExpanded += read
                        expandedTotal += read
                        if (entryExpanded > limits.maxEntryBytes) {
                            fail(BugReportImportFailureReason.ENTRY_TOO_LARGE, "ZIP entry exceeds ${limits.maxEntryBytes} bytes: ${entry.name}")
                        }
                        if (expandedTotal > limits.maxExpandedBytes) {
                            fail(BugReportImportFailureReason.EXPANDED_SIZE_LIMIT, "Archive expands beyond ${limits.maxExpandedBytes} bytes")
                        }
                        output.write(buffer, 0, read)
                    }

                    val compressedSize = entry.compressedSize.takeIf { it > 0 }
                    if (compressedSize != null && entryExpanded > 0) {
                        val ratio = entryExpanded.toDouble() / compressedSize.toDouble()
                        if (ratio > limits.maxCompressionRatio) {
                            fail(
                                BugReportImportFailureReason.SUSPICIOUS_COMPRESSION_RATIO,
                                "Suspicious compression ratio for ${entry.name}: $ratio"
                            )
                        }
                    }

                    result += BugReportArchiveEntry(entry.name, output.toByteArray(), compressedSize)
                    zip.closeEntry()
                }
            }
        } catch (error: BugReportImportException) {
            throw error
        } catch (error: IOException) {
            fail(BugReportImportFailureReason.MALFORMED_ARCHIVE, "Malformed ZIP archive: ${error.message ?: "I/O error"}")
        }

        return result
    }

    private fun validateArchiveEnvelope(bytes: ByteArray, limits: BugReportImportLimits) {
        if (bytes.size > limits.maxArchiveBytes) {
            fail(BugReportImportFailureReason.ARCHIVE_TOO_LARGE, "Archive exceeds ${limits.maxArchiveBytes} bytes")
        }
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            fail(BugReportImportFailureReason.MALFORMED_ARCHIVE, "Input does not have a ZIP signature")
        }
    }

    private fun fail(reason: BugReportImportFailureReason, message: String): Nothing =
        throw BugReportImportException(reason, message)
}
