package com.example.artifact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ADX001ArtifactDetectorTest {

    private val detector = DeterministicDiagnosticArtifactDetector()

    @Test
    fun detectsApplePanicFull() {
        assertDetected(Fixtures.applePanicFull, DiagnosticArtifactType.APPLE_PANIC_FULL)
    }

    @Test
    fun detectsAppleIpsCrash() {
        assertDetected(Fixtures.appleIpsCrash, DiagnosticArtifactType.APPLE_IPS_CRASH)
    }

    @Test
    fun detectsAndroidBugReportBeforeEmbeddedArtifacts() {
        val result = detector.detect(Fixtures.androidBugReportWithEmbeddedLogcatAndKernelPanic)
        assertEquals(DiagnosticArtifactType.ANDROID_BUGREPORT, result.type)
        assertTrue(result.confidence >= 0.95)
    }

    @Test
    fun detectsAndroidTombstone() {
        assertDetected(Fixtures.androidTombstone, DiagnosticArtifactType.ANDROID_TOMBSTONE)
    }

    @Test
    fun detectsAndroidNativeCrashWithoutFullTombstoneBanner() {
        assertDetected(Fixtures.androidNativeCrash, DiagnosticArtifactType.ANDROID_NATIVE_CRASH)
    }

    @Test
    fun detectsAndroidKernelPanic() {
        assertDetected(Fixtures.androidKernelPanic, DiagnosticArtifactType.ANDROID_KERNEL_PANIC)
    }

    @Test
    fun detectsAndroidAnr() {
        assertDetected(Fixtures.androidAnr, DiagnosticArtifactType.ANDROID_ANR)
    }

    @Test
    fun detectsAndroidJavaCrash() {
        assertDetected(Fixtures.androidJavaCrash, DiagnosticArtifactType.ANDROID_JAVA_CRASH)
    }

    @Test
    fun detectsAndroidDumpsys() {
        assertDetected(Fixtures.androidDumpsys, DiagnosticArtifactType.ANDROID_DUMPSYS)
    }

    @Test
    fun detectsAndroidLogcat() {
        assertDetected(Fixtures.androidLogcat, DiagnosticArtifactType.ANDROID_LOGCAT)
    }

    @Test
    fun unknownInputFailsClosed() {
        val result = detector.detect("technician note: device restarted twice")
        assertEquals(DiagnosticArtifactType.UNKNOWN, result.type)
        assertEquals(0.0, result.confidence)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test
    fun blankInputFailsClosed() {
        assertEquals(DiagnosticArtifactType.UNKNOWN, detector.detect("   \n\r ").type)
    }

    @Test
    fun platformIsPartOfTheArtifactContract() {
        assertEquals(DiagnosticPlatform.APPLE, DiagnosticArtifactType.APPLE_PANIC_FULL.platform)
        assertEquals(DiagnosticPlatform.ANDROID, DiagnosticArtifactType.ANDROID_BUGREPORT.platform)
        assertEquals(DiagnosticPlatform.UNKNOWN, DiagnosticArtifactType.UNKNOWN.platform)
    }

    @Test
    fun confidenceCannotEscapeUnitInterval() {
        assertFailsWith<IllegalArgumentException> {
            ArtifactDetection(DiagnosticArtifactType.UNKNOWN, 1.01)
        }
    }

    private fun assertDetected(text: String, expected: DiagnosticArtifactType) {
        val result = detector.detect(text)
        assertEquals(expected, result.type)
        assertTrue(result.confidence > 0.0)
        assertTrue(result.reasons.isNotEmpty())
    }

    private object Fixtures {
        val applePanicFull = """
            {"bug_type":210,"os_version":"iPhone OS 18.0"}
            panicString: panic(cpu 0 caller 0xfffffff): watchdog timeout
            Darwin Kernel Version 24.0.0
        """.trimIndent()

        val appleIpsCrash = """
            Incident Identifier: 00000000-1111-2222-3333-444444444444
            CrashReporter Key: REDACTED
            Process: ExampleApp [123]
            Path: /private/var/containers/Bundle/Application/REDACTED/ExampleApp.app/ExampleApp
        """.trimIndent()

        val androidBugReportWithEmbeddedLogcatAndKernelPanic = """
            ========================================================
            == dumpstate: 2026-09-27 00:00:00
            ========================================================
            Build fingerprint: 'google/example/example:16/TEST/123:user/release-keys'
            ------ SYSTEM LOG ------
            09-27 00:00:01.100  1000  1000 E Example : failure
            09-27 00:00:01.200  1000  1000 E Kernel  : Kernel panic - not syncing: synthetic fixture
        """.trimIndent()

        val androidTombstone = """
            *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
            Build fingerprint: 'vendor/device/device:16/TEST/123:user/release-keys'
            ABI: 'arm64'
            pid: 321, tid: 322, name: cameraserver  >>> /system/bin/cameraserver <<<
            signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0
            backtrace:
              #00 pc 0000000000012345 /system/lib64/libexample.so
        """.trimIndent()

        val androidNativeCrash = """
            debuggerd: handling request: pid 777
            signal 6 (SIGABRT), code -1 (SI_QUEUE)
            backtrace:
              #00 pc 0000000000001234 /vendor/lib64/libcamera_example.so
        """.trimIndent()

        val androidKernelPanic = """
            <0>[  42.100000] Kernel panic - not syncing: Fatal exception
            <0>[  42.100100] CPU: 0 PID: 1 Comm: init
            <0>[  42.100200] Call trace:
        """.trimIndent()

        val androidAnr = """
            ----- pid 4242 at 2026-09-27 00:01:02 -----
            Cmd line: com.example.camera
            DALVIK THREADS (14):
            "main" prio=5 tid=1 Waiting
              | held mutexes=
              - waiting to lock <0x1234>
        """.trimIndent()

        val androidJavaCrash = """
            09-27 00:00:01.000  1234  1234 E AndroidRuntime: FATAL EXCEPTION: main
            09-27 00:00:01.001  1234  1234 E AndroidRuntime: Process: com.example.app, PID: 1234
            09-27 00:00:01.002  1234  1234 E AndroidRuntime: java.lang.IllegalStateException: synthetic fixture
        """.trimIndent()

        val androidDumpsys = """
            DUMP OF SERVICE thermalservice:
            Thermal Status: 2
            Current temperatures from HAL:
            Temperature{mValue=42.0, mType=SKIN}
        """.trimIndent()

        val androidLogcat = """
            --------- beginning of main
            09-27 00:00:01.100  1000  1000 I ActivityManager: Start proc 1234
            09-27 00:00:01.200  1234  1234 W CameraService: provider timeout
            09-27 00:00:01.300  1234  1234 E CameraService: service disconnected
        """.trimIndent()
    }
}
