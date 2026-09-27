package com.example.artifact

class AndroidEvidenceExtractor(
    private val maxPerKind: Int = 20,
    private val contextRadius: Int = 1
) {
    init {
        require(maxPerKind > 0)
        require(contextRadius >= 0)
    }

    fun extract(session: AndroidDiagnosticSession): AndroidEvidenceReport = extractText(
        sourceFilename = session.sourceFilename,
        sourcePath = session.primaryReportPath,
        text = session.primaryReportText
    )

    fun extractText(sourceFilename: String, sourcePath: String, text: String): AndroidEvidenceReport {
        if (text.isBlank()) {
            return AndroidEvidenceReport(sourceFilename, sourcePath, emptyList())
        }

        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val evidence = mutableListOf<AndroidEvidence>()
        val counts = mutableMapOf<AndroidEvidenceKind, Int>()
        val seen = mutableSetOf<String>()
        var currentSection: String? = null

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            sectionName(line)?.let { currentSection = it }

            SPECS.forEach { spec ->
                if ((counts[spec.kind] ?: 0) >= maxPerKind) return@forEach
                if (!spec.regex.containsMatchIn(line)) return@forEach

                val normalized = line.trim().lowercase().replace(Regex("\\s+"), " ")
                val dedupeKey = "${spec.kind}:$normalized"
                if (!seen.add(dedupeKey)) return@forEach

                evidence += AndroidEvidence(
                    code = "ANDROID.EVIDENCE.${spec.kind.name}",
                    kind = spec.kind,
                    subsystem = spec.subsystem,
                    severity = spec.severity,
                    source = inferSource(currentSection, spec.kind),
                    sourcePath = sourcePath,
                    sourceSection = currentSection,
                    lineNumber = index + 1,
                    timestamp = extractTimestamp(line),
                    excerpt = buildExcerpt(lines, index)
                )
                counts[spec.kind] = (counts[spec.kind] ?: 0) + 1
            }
        }

        return AndroidEvidenceReport(
            sourceFilename = sourceFilename,
            sourcePath = sourcePath,
            evidence = evidence.sortedBy { it.lineNumber }
        )
    }

    private fun buildExcerpt(lines: List<String>, center: Int): String {
        val start = (center - contextRadius).coerceAtLeast(0)
        val end = (center + contextRadius).coerceAtMost(lines.lastIndex)
        return lines.subList(start, end + 1)
            .joinToString("\n") { it.trimEnd() }
            .trim()
            .take(MAX_EXCERPT_CHARS)
    }

    private fun inferSource(section: String?, kind: AndroidEvidenceKind): AndroidEvidenceSource {
        val lower = section?.lowercase().orEmpty()
        return when {
            "tombstone" in lower -> AndroidEvidenceSource.TOMBSTONE
            "anr" in lower || "trace" in lower && kind == AndroidEvidenceKind.ANR -> AndroidEvidenceSource.ANR_TRACE
            "kernel" in lower || kind == AndroidEvidenceKind.KERNEL_PANIC || kind == AndroidEvidenceKind.WATCHDOG -> AndroidEvidenceSource.KERNEL
            "dumpsys" in lower || "dump of service" in lower -> AndroidEvidenceSource.DUMPSYS
            "system log" in lower || "main log" in lower || "event log" in lower || "radio log" in lower || "logcat" in lower -> AndroidEvidenceSource.LOGCAT
            section != null -> AndroidEvidenceSource.BUGREPORT
            else -> AndroidEvidenceSource.UNKNOWN
        }
    }

    private fun sectionName(line: String): String? = SECTION_REGEX.matchEntire(line.trim())
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun extractTimestamp(line: String): String? = FULL_TIMESTAMP_REGEX.find(line)?.value
        ?: LOGCAT_TIMESTAMP_REGEX.find(line)?.value

    private data class Spec(
        val kind: AndroidEvidenceKind,
        val subsystem: AndroidSubsystem,
        val severity: AndroidEvidenceSeverity,
        val regex: Regex
    )

    private companion object {
        const val MAX_EXCERPT_CHARS = 1200

        val SECTION_REGEX = Regex("^-{6,}\\s*(.+?)\\s*-{6,}$")
        val FULL_TIMESTAMP_REGEX = Regex("\\b\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:[+-]\\d{4})?\\b")
        val LOGCAT_TIMESTAMP_REGEX = Regex("\\b\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\b")

        val SPECS = listOf(
            Spec(
                AndroidEvidenceKind.KERNEL_PANIC,
                AndroidSubsystem.KERNEL,
                AndroidEvidenceSeverity.CRITICAL,
                Regex("kernel panic - not syncing|panic_on_oops", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.WATCHDOG,
                AndroidSubsystem.KERNEL,
                AndroidEvidenceSeverity.CRITICAL,
                Regex("watchdog (?:bite|timeout|detected|hard lockup)|soft lockup.*cpu", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.TOMBSTONE,
                AndroidSubsystem.NATIVE,
                AndroidEvidenceSeverity.ERROR,
                Regex("\\*{3}\\s+\\*{3}\\s+\\*{3}")
            ),
            Spec(
                AndroidEvidenceKind.NATIVE_CRASH,
                AndroidSubsystem.NATIVE,
                AndroidEvidenceSeverity.ERROR,
                Regex("fatal signal\\s+\\d+|signal\\s+\\d+\\s+\\(SIG[A-Z]+\\)|^\\s*backtrace:\\s*$", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.ANR,
                AndroidSubsystem.APPLICATION,
                AndroidEvidenceSeverity.WARNING,
                Regex("\\bANR in\\b|\\bam_anr\\b|Application Not Responding", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.JAVA_CRASH,
                AndroidSubsystem.APPLICATION,
                AndroidEvidenceSeverity.ERROR,
                Regex("FATAL EXCEPTION:|AndroidRuntime.*(?:FATAL|Exception)", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.THERMAL,
                AndroidSubsystem.THERMAL,
                AndroidEvidenceSeverity.WARNING,
                Regex("(?:thermal|temperature).*(?:throttl|critical|shutdown|overheat|trip)|(?:throttl|overheat).*(?:thermal|temperature)", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.STORAGE_IO,
                AndroidSubsystem.STORAGE,
                AndroidEvidenceSeverity.ERROR,
                Regex("I/O error|EXT4-fs error|F2FS.*(?:error|corrupt)|ufs.*(?:error|timeout|fatal)|mmc.*(?:error|timeout)|blk_update_request.*error", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.MODEM_RADIO,
                AndroidSubsystem.RADIO,
                AndroidEvidenceSeverity.ERROR,
                Regex("(?:modem|radio|RIL|baseband).*(?:crash|fatal|reset|timeout|failed|failure|not responding)", RegexOption.IGNORE_CASE)
            ),
            Spec(
                AndroidEvidenceKind.CAMERA_HAL,
                AndroidSubsystem.CAMERA,
                AndroidEvidenceSeverity.ERROR,
                Regex("(?:camera|cameraserver|camera provider|camera hal).*(?:error|fatal|crash|dead|reset|failed|failure|timeout|not responding)", RegexOption.IGNORE_CASE)
            )
        )
    }
}
