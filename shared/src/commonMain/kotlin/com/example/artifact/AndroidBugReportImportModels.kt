package com.example.artifact

data class BugReportImportLimits(
    val maxArchiveBytes: Int = 64 * 1024 * 1024,
    val maxEntries: Int = 2048,
    val maxEntryBytes: Int = 64 * 1024 * 1024,
    val maxExpandedBytes: Long = 256L * 1024 * 1024,
    val maxCompressionRatio: Double = 250.0
) {
    init {
        require(maxArchiveBytes > 0)
        require(maxEntries > 0)
        require(maxEntryBytes > 0)
        require(maxExpandedBytes > 0)
        require(maxCompressionRatio >= 1.0)
    }
}

data class BugReportArchiveEntry(
    val path: String,
    val bytes: ByteArray,
    val compressedSize: Long? = null
)

interface BugReportArchiveReader {
    fun read(bytes: ByteArray, limits: BugReportImportLimits = BugReportImportLimits()): List<BugReportArchiveEntry>
}

enum class BugReportImportFailureReason {
    ARCHIVE_TOO_LARGE,
    TOO_MANY_ENTRIES,
    ENTRY_TOO_LARGE,
    EXPANDED_SIZE_LIMIT,
    SUSPICIOUS_COMPRESSION_RATIO,
    UNSAFE_ENTRY_PATH,
    MALFORMED_ARCHIVE,
    PRIMARY_REPORT_NOT_FOUND,
    UNSUPPORTED_ARTIFACT
}

class BugReportImportException(
    val reason: BugReportImportFailureReason,
    message: String
) : IllegalArgumentException(message)

data class AndroidArtifactInventoryItem(
    val path: String,
    val type: DiagnosticArtifactType,
    val sizeBytes: Int
)

data class AndroidDiagnosticSession(
    val sourceFilename: String,
    val primaryReportPath: String,
    val primaryReportText: String,
    val manufacturer: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val product: String? = null,
    val device: String? = null,
    val androidRelease: String? = null,
    val sdk: String? = null,
    val buildFingerprint: String? = null,
    val buildId: String? = null,
    val kernel: String? = null,
    val securityPatch: String? = null,
    val reportTimestamp: String? = null,
    val artifacts: List<AndroidArtifactInventoryItem> = emptyList()
)

internal fun validateArchiveEntryPath(path: String) {
    val normalized = path.replace('\\', '/')
    val segments = normalized.split('/')
    val unsafe = normalized.startsWith('/') ||
        Regex("^[A-Za-z]:/").containsMatchIn(normalized) ||
        segments.any { it == ".." } ||
        normalized.contains('\u0000')
    if (unsafe) {
        throw BugReportImportException(
            BugReportImportFailureReason.UNSAFE_ENTRY_PATH,
            "Unsafe ZIP entry path: $path"
        )
    }
}
