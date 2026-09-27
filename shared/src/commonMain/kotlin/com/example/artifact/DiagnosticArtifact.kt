package com.example.artifact

enum class DiagnosticPlatform {
    APPLE,
    ANDROID,
    UNKNOWN
}

enum class DiagnosticArtifactType(val platform: DiagnosticPlatform) {
    APPLE_PANIC_FULL(DiagnosticPlatform.APPLE),
    APPLE_IPS_CRASH(DiagnosticPlatform.APPLE),
    ANDROID_BUGREPORT(DiagnosticPlatform.ANDROID),
    ANDROID_KERNEL_PANIC(DiagnosticPlatform.ANDROID),
    ANDROID_TOMBSTONE(DiagnosticPlatform.ANDROID),
    ANDROID_ANR(DiagnosticPlatform.ANDROID),
    ANDROID_JAVA_CRASH(DiagnosticPlatform.ANDROID),
    ANDROID_NATIVE_CRASH(DiagnosticPlatform.ANDROID),
    ANDROID_LOGCAT(DiagnosticPlatform.ANDROID),
    ANDROID_DUMPSYS(DiagnosticPlatform.ANDROID),
    UNKNOWN(DiagnosticPlatform.UNKNOWN)
}

data class ArtifactDetection(
    val type: DiagnosticArtifactType,
    val confidence: Double,
    val reasons: List<String> = emptyList()
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be between 0.0 and 1.0" }
    }
}

interface DiagnosticArtifactDetector {
    fun detect(text: String): ArtifactDetection
}
