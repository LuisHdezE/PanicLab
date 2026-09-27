package com.example.artifact

class AndroidBugReportImporter(
    private val detector: DiagnosticArtifactDetector = DeterministicDiagnosticArtifactDetector()
) {
    fun importText(sourceFilename: String, text: String): AndroidDiagnosticSession {
        val detection = detector.detect(text)
        if (detection.type != DiagnosticArtifactType.ANDROID_BUGREPORT) {
            throw BugReportImportException(
                BugReportImportFailureReason.UNSUPPORTED_ARTIFACT,
                "Input is not a supported Android bugreport"
            )
        }

        return buildSession(
            sourceFilename = sourceFilename,
            primaryReportPath = sourceFilename,
            primaryText = text,
            artifacts = listOf(
                AndroidArtifactInventoryItem(
                    path = sourceFilename,
                    type = DiagnosticArtifactType.ANDROID_BUGREPORT,
                    sizeBytes = text.encodeToByteArray().size
                )
            )
        )
    }

    fun importArchive(
        sourceFilename: String,
        bytes: ByteArray,
        archiveReader: BugReportArchiveReader,
        limits: BugReportImportLimits = BugReportImportLimits()
    ): AndroidDiagnosticSession {
        if (bytes.size > limits.maxArchiveBytes) {
            throw BugReportImportException(
                BugReportImportFailureReason.ARCHIVE_TOO_LARGE,
                "Archive exceeds ${limits.maxArchiveBytes} bytes"
            )
        }

        val entries = archiveReader.read(bytes, limits)
        val textual = entries.filter { isTextCandidate(it.path) }
        val detected = textual.mapNotNull { entry ->
            val text = runCatching { entry.bytes.decodeToString() }.getOrNull() ?: return@mapNotNull null
            Triple(entry, text, detector.detect(text))
        }

        val primary = detected
            .filter { it.third.type == DiagnosticArtifactType.ANDROID_BUGREPORT }
            .sortedWith(
                compareByDescending<Triple<BugReportArchiveEntry, String, ArtifactDetection>> {
                    preferredBugReportName(it.first.path)
                }.thenByDescending { it.first.bytes.size }
            )
            .firstOrNull()
            ?: throw BugReportImportException(
                BugReportImportFailureReason.PRIMARY_REPORT_NOT_FOUND,
                "No Android bugreport text was found in the archive"
            )

        val inventory = entries.map { entry ->
            val known = detected.firstOrNull { it.first.path == entry.path }?.third?.type ?: DiagnosticArtifactType.UNKNOWN
            AndroidArtifactInventoryItem(
                path = entry.path,
                type = known,
                sizeBytes = entry.bytes.size
            )
        }

        return buildSession(
            sourceFilename = sourceFilename,
            primaryReportPath = primary.first.path,
            primaryText = primary.second,
            artifacts = inventory
        )
    }

    private fun buildSession(
        sourceFilename: String,
        primaryReportPath: String,
        primaryText: String,
        artifacts: List<AndroidArtifactInventoryItem>
    ): AndroidDiagnosticSession {
        val properties = PROPERTY_REGEX.findAll(primaryText).associate { match ->
            match.groupValues[1] to match.groupValues[2]
        }

        fun property(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            properties[key]?.takeIf { it.isNotBlank() }
        }

        val fingerprint = BUILD_FINGERPRINT_REGEX.find(primaryText)?.groupValues?.get(1)?.trim()?.trim('\'')
            ?: property("ro.build.fingerprint")

        return AndroidDiagnosticSession(
            sourceFilename = sourceFilename,
            primaryReportPath = primaryReportPath,
            primaryReportText = primaryText,
            manufacturer = property("ro.product.manufacturer", "ro.product.system.manufacturer"),
            brand = property("ro.product.brand", "ro.product.system.brand"),
            model = property("ro.product.model", "ro.product.system.model"),
            product = property("ro.product.name", "ro.product.system.name"),
            device = property("ro.product.device", "ro.product.system.device"),
            androidRelease = property("ro.build.version.release", "ro.system.build.version.release"),
            sdk = property("ro.build.version.sdk", "ro.system.build.version.sdk"),
            buildFingerprint = fingerprint,
            buildId = property("ro.build.id", "ro.system.build.id"),
            kernel = KERNEL_REGEX.find(primaryText)?.groupValues?.get(1)?.trim(),
            securityPatch = property("ro.build.version.security_patch", "ro.vendor.build.security_patch"),
            reportTimestamp = DUMPSTATE_TIMESTAMP_REGEX.find(primaryText)?.groupValues?.get(1)?.trim(),
            artifacts = artifacts
        )
    }

    private fun preferredBugReportName(path: String): Boolean {
        val filename = path.replace('\\', '/').substringAfterLast('/').lowercase()
        return filename.startsWith("bugreport-") && filename.endsWith(".txt")
    }

    private fun isTextCandidate(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".txt") || lower.endsWith(".log") || '.' !in lower.substringAfterLast('/')
    }

    private companion object {
        val PROPERTY_REGEX = Regex("(?m)^\\[([^\\]]+)]\\s*:\\s*\\[(.*)]\\s*$")
        val BUILD_FINGERPRINT_REGEX = Regex("(?im)^Build fingerprint:\\s*(.+?)\\s*$")
        val DUMPSTATE_TIMESTAMP_REGEX = Regex("(?im)^==\\s*dumpstate:\\s*(.+?)\\s*$")
        val KERNEL_REGEX = Regex("(?im)^(?:Kernel version|Linux version):\\s*(.+?)\\s*$")
    }
}
