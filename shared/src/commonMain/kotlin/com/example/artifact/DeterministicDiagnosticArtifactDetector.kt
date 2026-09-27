package com.example.artifact

class DeterministicDiagnosticArtifactDetector : DiagnosticArtifactDetector {

    override fun detect(text: String): ArtifactDetection {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) {
            return unknown("input is empty")
        }

        val lower = normalized.lowercase()

        detectAndroidBugReport(lower)?.let { return it }
        detectApplePanicFull(lower)?.let { return it }
        detectAppleIpsCrash(lower)?.let { return it }
        detectAndroidTombstone(lower)?.let { return it }
        detectAndroidKernelPanic(lower)?.let { return it }
        detectAndroidAnr(lower)?.let { return it }
        detectAndroidJavaCrash(lower)?.let { return it }
        detectAndroidDumpsys(lower)?.let { return it }
        detectAndroidLogcat(normalized, lower)?.let { return it }

        return unknown("no supported artifact signature matched")
    }

    private fun detectAndroidBugReport(lower: String): ArtifactDetection? {
        val hasFingerprint = "build fingerprint:" in lower
        val hasDumpstate = "dumpstate" in lower || "bugreport" in lower
        val hasSectionBoundary = "------" in lower
        if (hasFingerprint && hasDumpstate && hasSectionBoundary) {
            return detection(
                DiagnosticArtifactType.ANDROID_BUGREPORT,
                0.99,
                "Android build fingerprint present",
                "dumpstate/bugreport marker present",
                "bugreport section boundary present"
            )
        }
        return null
    }

    private fun detectApplePanicFull(lower: String): ArtifactDetection? {
        val hasPanicString = "panicstring" in lower || "panic_string" in lower || "panic(cpu " in lower
        val hasAppleKernel = "darwin kernel version" in lower || "bug_type\" : 210" in lower || "bug_type\":210" in lower
        if (hasPanicString && hasAppleKernel) {
            return detection(
                DiagnosticArtifactType.APPLE_PANIC_FULL,
                0.99,
                "Apple panic signature present",
                "Apple kernel or panic bug-type marker present"
            )
        }
        return null
    }

    private fun detectAppleIpsCrash(lower: String): ArtifactDetection? {
        val classicIps = "incident identifier:" in lower && "crashreporter key:" in lower
        val jsonIps = ("\"incident\"" in lower || "\"incident_id\"" in lower) &&
            ("\"procname\"" in lower || "\"process\"" in lower) &&
            "panicstring" !in lower
        if (classicIps || jsonIps) {
            return detection(
                DiagnosticArtifactType.APPLE_IPS_CRASH,
                0.95,
                if (classicIps) "classic Apple crash-report headers present" else "Apple IPS JSON crash fields present"
            )
        }
        return null
    }

    private fun detectAndroidTombstone(lower: String): ArtifactDetection? {
        val hasBanner = "*** *** *** *** *** *** *** ***" in lower
        val hasAbi = "abi:" in lower
        val hasCrashIdentity = "pid:" in lower && ("signal " in lower || "fault addr" in lower)
        if (hasBanner && hasAbi && hasCrashIdentity) {
            return detection(
                DiagnosticArtifactType.ANDROID_TOMBSTONE,
                0.99,
                "Android tombstone banner present",
                "ABI marker present",
                "native crash pid/signal markers present"
            )
        }

        val debuggerdNative = "debuggerd" in lower && "signal " in lower && "backtrace:" in lower
        if (debuggerdNative) {
            return detection(
                DiagnosticArtifactType.ANDROID_NATIVE_CRASH,
                0.94,
                "debuggerd native crash markers present",
                "native backtrace present"
            )
        }
        return null
    }

    private fun detectAndroidKernelPanic(lower: String): ArtifactDetection? {
        val explicit = "kernel panic - not syncing" in lower
        val callTrace = "call trace:" in lower && ("panic" in lower || "watchdog" in lower)
        if (explicit || callTrace) {
            return detection(
                DiagnosticArtifactType.ANDROID_KERNEL_PANIC,
                if (explicit) 0.99 else 0.91,
                if (explicit) "kernel panic not-syncing signature present" else "kernel panic/watchdog call trace present"
            )
        }
        return null
    }

    private fun detectAndroidAnr(lower: String): ArtifactDetection? {
        val explicit = "anr in " in lower
        val trace = "----- pid " in lower && "cmd line:" in lower &&
            ("dalvik threads" in lower || "waiting to lock" in lower || "held mutexes=" in lower)
        if (explicit || trace) {
            return detection(
                DiagnosticArtifactType.ANDROID_ANR,
                if (trace) 0.97 else 0.90,
                if (trace) "Android ANR trace markers present" else "ANR event marker present"
            )
        }
        return null
    }

    private fun detectAndroidJavaCrash(lower: String): ArtifactDetection? {
        val fatalException = "fatal exception:" in lower
        val runtimeContext = "androidruntime" in lower || "process:" in lower
        if (fatalException && runtimeContext) {
            return detection(
                DiagnosticArtifactType.ANDROID_JAVA_CRASH,
                0.96,
                "FATAL EXCEPTION marker present",
                "Android runtime/process context present"
            )
        }
        return null
    }

    private fun detectAndroidDumpsys(lower: String): ArtifactDetection? {
        val explicit = "dump of service" in lower || "dumpsys " in lower
        val commonHeader = "service dump" in lower && ("activity" in lower || "battery" in lower || "thermal" in lower)
        if (explicit || commonHeader) {
            return detection(
                DiagnosticArtifactType.ANDROID_DUMPSYS,
                0.91,
                "Android dumpsys/service-dump marker present"
            )
        }
        return null
    }

    private fun detectAndroidLogcat(normalized: String, lower: String): ArtifactDetection? {
        val logcatHeader = "beginning of main" in lower || "beginning of system" in lower || "beginning of crash" in lower
        val logLineRegex = Regex("(?m)^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\d+\\s+\\d+\\s+[VDIWEAF]\\s+")
        val matchingLines = logLineRegex.findAll(normalized).take(3).count()
        if (logcatHeader || matchingLines >= 2) {
            return detection(
                DiagnosticArtifactType.ANDROID_LOGCAT,
                if (logcatHeader && matchingLines >= 2) 0.97 else 0.90,
                if (logcatHeader) "logcat buffer header present" else "multiple Android logcat-formatted lines present"
            )
        }
        return null
    }

    private fun detection(
        type: DiagnosticArtifactType,
        confidence: Double,
        vararg reasons: String
    ): ArtifactDetection = ArtifactDetection(type, confidence, reasons.toList())

    private fun unknown(reason: String): ArtifactDetection = ArtifactDetection(
        type = DiagnosticArtifactType.UNKNOWN,
        confidence = 0.0,
        reasons = listOf(reason)
    )
}
