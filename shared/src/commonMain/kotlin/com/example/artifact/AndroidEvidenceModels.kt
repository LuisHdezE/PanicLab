package com.example.artifact

enum class AndroidEvidenceKind {
    KERNEL_PANIC,
    WATCHDOG,
    TOMBSTONE,
    NATIVE_CRASH,
    ANR,
    JAVA_CRASH,
    THERMAL,
    STORAGE_IO,
    MODEM_RADIO,
    CAMERA_HAL
}

enum class AndroidEvidenceSource {
    BUGREPORT,
    LOGCAT,
    DUMPSYS,
    KERNEL,
    TOMBSTONE,
    ANR_TRACE,
    UNKNOWN
}

enum class AndroidSubsystem {
    KERNEL,
    SYSTEM,
    APPLICATION,
    NATIVE,
    THERMAL,
    STORAGE,
    RADIO,
    CAMERA,
    UNKNOWN
}

enum class AndroidEvidenceSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class AndroidEvidence(
    val code: String,
    val kind: AndroidEvidenceKind,
    val subsystem: AndroidSubsystem,
    val severity: AndroidEvidenceSeverity,
    val source: AndroidEvidenceSource,
    val sourcePath: String,
    val sourceSection: String? = null,
    val lineNumber: Int,
    val timestamp: String? = null,
    val excerpt: String
) {
    init {
        require(code.isNotBlank())
        require(sourcePath.isNotBlank())
        require(lineNumber > 0)
        require(excerpt.isNotBlank())
    }
}

data class AndroidEvidenceReport(
    val sourceFilename: String,
    val sourcePath: String,
    val evidence: List<AndroidEvidence>
) {
    fun count(kind: AndroidEvidenceKind): Int = evidence.count { it.kind == kind }
    fun has(kind: AndroidEvidenceKind): Boolean = evidence.any { it.kind == kind }
}
