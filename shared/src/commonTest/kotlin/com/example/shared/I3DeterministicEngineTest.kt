package com.example.shared

import com.example.diagnostic.CandidateRanker
import com.example.diagnostic.DiagnosticReportBuilder
import com.example.diagnostic.DiagnosticRulesEngine
import com.example.diagnostic.ExtractedSensors
import com.example.diagnostic.SensorExtractor
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceModel
import com.example.domain.model.DeviceScope
import com.example.domain.model.DiagnosisDefinition
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.domain.model.RepairFlow
import com.example.domain.model.SensorCode
import com.example.domain.model.SuspectedComponent
import com.example.domain.model.VerificationStatus
import com.example.ocr.OcrLogExtractor
import com.example.parser.DeviceResolver
import com.example.parser.EvidenceExtractor
import com.example.parser.LogNormalizer
import com.example.parser.MetadataExtractor
import com.example.parser.PanicClassifier
import com.example.platform.Clock
import com.example.platform.IdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class I3DeterministicEngineTest {

    private class SequenceIds : IdGenerator {
        private var value = 0
        override fun nextId(): String = "id-${++value}"
    }

    private val fixedClock = Clock { 1_700_000_000_123L }

    @Test
    fun metadataParsesCompleteJsonAndAliases() {
        val metadata = MetadataExtractor.extract(
            """{"model":"iPhone14,7","osVersion":"17.3","build":"21D50","bug_type":"210","incident":"ABC","crashReporterKey":"KEY","panicInitiator":"init","panicString":"SMC PANIC\nBSC failure","kernel":"Darwin","socId":"T8110","timestamp":"2026-01-01","repairStatus":"none","roots_installed":"0"}"""
        )

        assertEquals("iPhone14,7", metadata.product)
        assertEquals("17.3", metadata.osVersion)
        assertEquals("21D50", metadata.build)
        assertEquals("210", metadata.bugType)
        assertEquals("ABC", metadata.incidentId)
        assertEquals("KEY", metadata.crashReporterKey)
        assertEquals("init", metadata.panicInitiator)
        assertEquals("SMC PANIC\nBSC failure", metadata.panicString)
        assertEquals("Darwin", metadata.kernel)
        assertEquals("T8110", metadata.socId)
        assertEquals("2026-01-01", metadata.timestamp)
        assertEquals("none", metadata.repairStatus)
        assertEquals("0", metadata.rootsInstalled)
    }

    @Test
    fun metadataFallsBackToRegexAndPanicBlock() {
        val text = """
            product: iPhone17,1
            OS Version: 18.0
            Build: 22A3354
            Incident Identifier: INC-1
            CrashReporter Key: CRK
            panic(cpu 2): SMC PANIC - BSC failure
            S.sensor array 0 - 7 is 0, 3145728, 0

            Backtrace:
        """.trimIndent()
        val metadata = MetadataExtractor.extract(text)

        assertEquals("iPhone17,1", metadata.product)
        assertEquals("18.0", metadata.osVersion)
        assertEquals("22A3354", metadata.build)
        assertEquals("INC-1", metadata.incidentId)
        assertEquals("CRK", metadata.crashReporterKey)
        assertTrue(metadata.panicString.orEmpty().contains("BSC failure"))
    }

    @Test
    fun metadataHandlesBlankMalformedAndPartialJsonPanic() {
        assertEquals(ParsedMetadata(), MetadataExtractor.extract("   "))

        val malformed = MetadataExtractor.extract("{not-valid-json product: iPhone14,4")
        assertEquals("iPhone14,4", malformed.product)

        val partial = MetadataExtractor.extract("prefix \"panicString\":\"line1\\nline2\\\"quoted\\\"\" suffix")
        assertEquals("line1\nline2\"quoted\"", partial.panicString)
    }

    @Test
    fun classifierPreservesFamilySemanticsAndWatchdogExclusion() {
        val many = """
            thermalmonitord Missing sensor(s): PRS0
            SMC BSC failure ASSERTION FAILED
            AOP NMI POWER Bosch control channel write failure
            ANS2 AppleSocHot SEP ROM Undefined Kernel Instruction
            i2c1 DCP iomfb baseband AppleBaseband BB_FATAL
            userspace watchdog timeout
        """.trimIndent()
        val families = PanicClassifier.classify(many, null)

        assertTrue(PanicFamily.THERMAL_MISSING_SENSOR in families)
        assertTrue(PanicFamily.SMC_BSC_FAILURE in families)
        assertTrue(PanicFamily.SMC_ASSERTION in families)
        assertTrue(PanicFamily.AOP_NMI_POWER in families)
        assertTrue(PanicFamily.AOP_BOSCH_CONTROL in families)
        assertTrue(PanicFamily.ANS2 in families)
        assertTrue(PanicFamily.APPLE_SOC_HOT in families)
        assertTrue(PanicFamily.SEP_ROM_BOOT in families)
        assertTrue(PanicFamily.UNDEFINED_KERNEL_INSTRUCTION in families)
        assertTrue(PanicFamily.I2C in families)
        assertTrue(PanicFamily.DCP_DISPLAY in families)
        assertTrue(PanicFamily.BASEBAND in families)
        assertFalse(PanicFamily.WATCHDOG_NO_CHECKIN in families)

        assertEquals(listOf(PanicFamily.WATCHDOG_NO_CHECKIN), PanicClassifier.classify("userspace watchdog timeout", null))
        assertEquals(listOf(PanicFamily.UNKNOWN), PanicClassifier.classify("ordinary log", null))
        assertTrue(PanicFamily.SMC_BSC_FAILURE in PanicClassifier.classify("plain", "SMC BSC failure"))
    }

    @Test
    fun sensorExtractorPreservesMissingAndArraySemantics() {
        val extracted = SensorExtractor.extract(
            """
                Missing sensor(s): PRS0, Mic1
                S.sensor array 0 - 7 is 0x0, 0x1000, 3145728, 0x1000, 0
                sensor_array: 524288
            """.trimIndent()
        )

        assertEquals(listOf("PRS0", "Mic1"), extracted.missingSensorTokens)
        assertEquals(listOf("0x1000", "3145728", "524288"), extracted.smcSensorCodes)
        assertEquals(listOf(4096L, 3145728L, 524288L), extracted.sensorCodes.map { it.numericValue })
        assertEquals(2, extracted.rawSensorArrayLines.size)
    }

    @Test
    fun sensorExtractorUsesStandaloneSmcFallbackAndPanicString() {
        val fallback = SensorExtractor.extract("SMC panic code 0x500000 and 0x0")
        assertEquals(listOf("0x500000"), fallback.smcSensorCodes)

        val fromPanic = SensorExtractor.extract("header", "S.sensor array = 0, 4096, 0")
        assertEquals(listOf("4096"), fromPanic.smcSensorCodes)
    }

    @Test
    fun deviceResolverPreservesStaticMappings() {
        assertEquals("iPhone X", DeviceResolver.resolveSynchronous(" iPhone10,3 ")?.marketingName)
        assertEquals("SMC_13_MINI", DeviceResolver.resolveSynchronous("iPhone14,4")?.diagnosticProfile)
        assertEquals("iPhone 17e", DeviceResolver.resolveSynchronous("iPhone18,5")?.marketingName)
        assertNull(DeviceResolver.resolveSynchronous("iPhone99,9"))
        assertNull(DeviceResolver.resolveSynchronous(" "))
        assertNull(DeviceResolver.resolveSynchronous(null))
    }

    @Test
    fun evidenceExtractorUsesInjectedIdsAndStableDomainMeaning() {
        val log = """
            product: iPhone10,3
            thermalmonitord: Missing sensor(s): PRS0
            SMC BSC failure S.sensor array is 0x1000
        """.trimIndent()
        val sensors = SensorExtractor.extract(log)
        val evidence = EvidenceExtractor.extractEvidences(
            logText = log,
            metadata = ParsedMetadata(product = "iPhone10,3"),
            panicFamilies = listOf(PanicFamily.THERMAL_MISSING_SENSOR, PanicFamily.SMC_BSC_FAILURE, PanicFamily.UNKNOWN),
            extractedSensors = sensors,
            idGenerator = SequenceIds()
        )

        assertTrue(evidence.any { it.type == "MISSING_SENSOR" && it.normalizedValue == "PRS0" })
        assertTrue(evidence.any { it.type == "SMC_CODE" && it.normalizedValue == "0x1000" })
        assertTrue(evidence.any { it.type == "PANIC_SIGNATURE" && it.normalizedValue == "THERMAL_MISSING_SENSOR" })
        assertTrue(evidence.any { it.type == "PRODUCT_CODE" && it.rawValue == "iPhone10,3" })
        assertEquals(evidence.size, evidence.map { it.id }.distinct().size)
        assertEquals("id-1", evidence.first().id)
    }

    @Test
    fun frozenI0Iphone13MiniExact0x1000RemainsEquivalent() {
        val result = runPipeline(
            """
                {"bug_type":"210","os_version":"iPhone OS 17.4 (21E236)","product":"iPhone14,4","build":"21E236"}
                panic(cpu 0): "SMC PANIC - BSC failure at address 0x1000 - S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0"
            """.trimIndent()
        )
        assertEquals("iphone13_mini_0x1000_dock_mic", result.primaryRule?.id)
        assertEquals("Micrófono Inferior / Flex de Carga (Mic1 / Dock)", result.primaryRule?.diagnosis?.label)
    }

    @Test
    fun frozenI0Iphone14BatteryAndWirelessDecimalRemainEquivalent() {
        val battery = runPipeline(
            """
                {"bug_type":"210","product":"iPhone14,7","os_version":"17.3"}
                panic(cpu 1): "SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0"
            """.trimIndent()
        )
        assertEquals("iphone14_base_0x500000_battery", battery.primaryRule?.id)

        val wireless = runPipeline(
            """
                {"bug_type":"210","product":"iPhone14,7"}
                panic(cpu 0): "SMC PANIC - BSC failure: S.sensor array 0 - 5 is 0, 4194304, 0"
            """.trimIndent()
        )
        assertEquals("smc14base_0x400000_wireless_charge_coil", wireless.primaryRule?.id)
    }

    @Test
    fun frozenI0Iphone16DecimalAndPrs0RemainEquivalent() {
        val iphone16 = runPipeline(
            """
                {"bug_type":"210","product":"iPhone17,1","os_version":"18.0"}
                panic(cpu 2): "SMC PANIC - BSC failure: S.sensor array 0 - 7 is 0, 3145728, 0, 0, 0"
            """.trimIndent()
        )
        assertEquals("iphone16_pro_3145728_battery_prox", iphone16.primaryRule?.id)

        val prs0 = runPipeline(
            """
                {"bug_type":"210","product":"iPhone10,3"}
                panic(cpu 0): "thermalmonitord: Missing sensor(s): PRS0, thermal runaway"
            """.trimIndent()
        )
        assertEquals("prs0_missing_sensor_rule", prs0.primaryRule?.id)
    }

    @Test
    fun frozenI0UnknownSmcUsesSafeFallback() {
        val result = runPipeline(
            """
                {"bug_type":"210","product":"iPhone14,7"}
                panic(cpu 0): "SMC PANIC - BSC failure: S.sensor array is 0xDEAD, 0x0"
            """.trimIndent()
        )
        assertEquals("smc_unknown_code_fallback", result.primaryRule?.id)
        assertEquals(ConfidenceLevel.UNKNOWN, result.primaryRule?.confidence)
    }

    @Test
    fun rulesEngineHonorsExactBeforeBitmaskAndScope() {
        val device = DeviceResolver.resolveSynchronous("iPhone14,5")
        val exact = testRule(
            id = "exact-0x3000",
            exactCodes = listOf("0x3000"),
            profiles = listOf("SMC_13"),
            priority = 200
        )
        val bitOne = testRule(
            id = "bit-0x1000",
            exactCodes = listOf("0x1000"),
            profiles = listOf("SMC_13"),
            allowBitmask = true
        )
        val bitTwo = testRule(
            id = "bit-0x2000",
            exactCodes = listOf("0x2000"),
            profiles = listOf("SMC_13"),
            allowBitmask = true
        )

        val exactResult = DiagnosticRulesEngine.evaluate(
            device,
            "iPhone14,5",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000)), smcSensorCodes = listOf("0x3000")),
            listOf(exact, bitOne, bitTwo, fallbackRule())
        )
        assertEquals("exact-0x3000", exactResult.primaryRule?.id)
        assertFalse("bit-0x1000" in exactResult.appliedRuleIds)

        val decomposed = DiagnosticRulesEngine.evaluate(
            device,
            "iPhone14,5",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000)), smcSensorCodes = listOf("0x3000")),
            listOf(bitOne, bitTwo, fallbackRule())
        )
        assertTrue("bit-0x1000" in decomposed.appliedRuleIds)
        assertTrue("bit-0x2000" in decomposed.appliedRuleIds)

        val wrongProfile = DiagnosticRulesEngine.evaluate(
            DeviceResolver.resolveSynchronous("iPhone14,7"),
            "iPhone14,7",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000)), smcSensorCodes = listOf("0x3000")),
            listOf(bitOne, bitTwo, fallbackRule())
        )
        assertEquals("smc_unknown_code_fallback", wrongProfile.primaryRule?.id)
    }

    @Test
    fun rulesEngineCoversFamilyOnlyInactiveUniversalAndNoPrimary() {
        val familyRule = testRule(
            id = "watchdog",
            families = listOf(PanicFamily.WATCHDOG_NO_CHECKIN),
            exactCodes = emptyList(),
            primaryEligible = false,
            confidence = ConfidenceLevel.MEDIUM
        )
        val inactive = testRule(id = "inactive", active = false, exactCodes = emptyList(), families = listOf(PanicFamily.WATCHDOG_NO_CHECKIN))

        val result = DiagnosticRulesEngine.evaluate(
            null,
            null,
            listOf(PanicFamily.WATCHDOG_NO_CHECKIN),
            ExtractedSensors(),
            listOf(inactive, familyRule)
        )
        assertEquals("watchdog", result.primaryRule?.id)
        assertFalse("inactive" in result.appliedRuleIds)
        assertTrue(result.alternativeRules.isEmpty())
    }

    @Test
    fun candidateRankerMapsPrimaryAndAlternatives() {
        val primaryRule = testRule("primary", priority = 200)
        val alternative = testRule("alt", confidence = ConfidenceLevel.MEDIUM)
        val (primary, alternatives) = CandidateRanker.toCandidates(primaryRule, listOf(alternative))

        assertEquals("primary", primary?.ruleId)
        assertEquals(true, primary?.isPrimary)
        assertEquals("alt", alternatives.single().ruleId)
        assertFalse(alternatives.single().isPrimary)

        val empty = CandidateRanker.toCandidates(null, emptyList())
        assertNull(empty.first)
        assertTrue(empty.second.isEmpty())
    }

    @Test
    fun reportBuilderUsesInjectedRuntimeAndRawLogPreference() {
        val candidate = CandidateRanker.toCandidates(testRule("primary"), emptyList()).first
        val report = DiagnosticReportBuilder.build(
            sourceFilename = "panic.ips",
            rawLog = "raw",
            metadata = ParsedMetadata(product = "iPhone14,7", osVersion = "17.3", build = "21D50", panicString = "line1\nline2\nline3\nline4"),
            deviceModel = DeviceResolver.resolveSynchronous("iPhone14,7"),
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            evidences = emptyList(),
            primaryCandidate = candidate,
            alternativeCandidates = emptyList(),
            kbVersion = "1.0.0",
            saveRawLogsPreference = true,
            clock = fixedClock,
            idGenerator = SequenceIds()
        )

        assertEquals("id-1", report.id)
        assertEquals(1_700_000_000_123L, report.createdAt)
        assertEquals("line1\nline2\nline3", report.panicStringSummary)
        assertEquals("raw", report.rawLog)
        assertTrue(report.rawLogSaved)
        assertEquals(ConfidenceLevel.HIGH, report.confidence)
    }

    @Test
    fun reportBuilderCoversFamilyAndEmptyFallbackSummaries() {
        val familyReport = DiagnosticReportBuilder.build(
            null, "raw", ParsedMetadata(), null,
            listOf(PanicFamily.I2C), emptyList(), null, emptyList(), "1", false,
            fixedClock, SequenceIds()
        )
        assertTrue(familyReport.panicStringSummary.contains("I2C"))
        assertNull(familyReport.rawLog)
        assertEquals("Desconocido", familyReport.productCode)
        assertEquals(ConfidenceLevel.UNKNOWN, familyReport.confidence)

        val emptyReport = DiagnosticReportBuilder.build(
            null, "raw", ParsedMetadata(), null,
            emptyList(), emptyList(), null, emptyList(), "1", false,
            fixedClock, SequenceIds()
        )
        assertEquals("No se detectó un mensaje de pánico estructurado.", emptyReport.panicStringSummary)
    }

    @Test
    fun ocrFrozenFixturesRepairHexDecimalAndDeviceBuild() {
        val result = OcrLogExtractor.processScannedText(
            """
                iPhone14,7 21E236
                S . sensor array 0 - 5 is Ox4OOOOO
                SMC PANIC BSC FAILURE
                decimal 524288
            """.trimIndent()
        )

        assertEquals("iPhone14,7", result.detectedDeviceModel)
        assertEquals("21E236", result.detectedBuild)
        assertTrue("0X400000" in result.detectedPanicCodes)
        assertTrue("0x80000" in result.detectedPanicCodes)
        assertTrue(result.cleanedText.contains("S.sensor array"))
        assertTrue(result.hasValidPanicSignatures)
        assertTrue(result.confidenceHint.startsWith("Excelente"))
    }

    @Test
    fun ocrCoversEmptyPartialTextAndWeakConfidenceBranches() {
        val empty = OcrLogExtractor.processScannedText("   ")
        assertEquals("Sin texto detectado", empty.confidenceHint)
        assertEquals(0, empty.lineCount)

        val codeOnly = OcrLogExtractor.processScannedText("sensor 0x1000")
        assertTrue(codeOnly.confidenceHint.startsWith("Buena"))

        val partial = OcrLogExtractor.processScannedText("SMC PANIC")
        assertTrue(partial.confidenceHint.startsWith("Captura parcial"))

        val text = OcrLogExtractor.processScannedText("a\nb\nc\nd\ne\nf")
        assertTrue(text.confidenceHint.startsWith("Texto detectado"))

        val weak = OcrLogExtractor.processScannedText("hello")
        assertTrue(weak.confidenceHint.startsWith("Captura débil"))
    }

    @Test
    fun ocrKeepsExistingStructuredLogAndAddsMinimalContextOnlyWhenNeeded() {
        val structured = OcrLogExtractor.processScannedText("{\"bug_type\":\"210\",\"product\":\"iPhone14,7\"}")
        assertEquals("{\"bug_type\":\"210\",\"product\":\"iPhone14,7\"}", structured.cleanedText)

        val snippet = OcrLogExtractor.processScannedText("iPhone14,7\nSMC PANIC")
        assertTrue(snippet.cleanedText.startsWith("{\"product\":\"iPhone14,7\""))
        assertTrue(snippet.cleanedText.contains("OCR_SCAN"))
    }

    private fun runPipeline(rawLog: String) = run {
        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized, metadata.panicString)
        DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = baselineRules()
        )
    }

    private fun baselineRules(): List<DiagnosticRule> = listOf(
        testRule(
            id = "iphone13_mini_0x1000_dock_mic",
            label = "Micrófono Inferior / Flex de Carga (Mic1 / Dock)",
            profiles = listOf("SMC_13_MINI"),
            products = listOf("iPhone14,4"),
            exactCodes = listOf("0x1000", "4096"),
            suspected = listOf(SuspectedComponent("Flex de puerto de carga (Dock Connector)", "PRIMARY"))
        ),
        testRule(
            id = "smc14base_0x400000_wireless_charge_coil",
            label = "Wireless Charging Coil",
            profiles = listOf("SMC_14_BASE"),
            exactCodes = listOf("0x400000")
        ),
        testRule(
            id = "iphone14_base_0x500000_battery",
            label = "Batería / Línea I2C Gas Gauge (BATT_HDQ / BATT_SWI)",
            profiles = listOf("SMC_14_BASE"),
            products = listOf("iPhone14,7", "iPhone14,8"),
            exactCodes = listOf("0x500000", "5242880"),
            priority = 130
        ),
        testRule(
            id = "iphone16_pro_3145728_battery_prox",
            label = "Batería + Sensor Proximidad / Auricular Superior",
            profiles = listOf("SMC_16_PRO"),
            products = listOf("iPhone17,1"),
            exactCodes = listOf("3145728", "0x300000"),
            priority = 125
        ),
        testRule(
            id = "prs0_missing_sensor_rule",
            label = "Barómetro / Flex de Carga (PRS0)",
            profiles = listOf("THERMAL_CLASSIC_X_TO_12"),
            families = listOf(PanicFamily.THERMAL_MISSING_SENSOR),
            tokens = listOf("PRS0"),
            exactCodes = emptyList()
        ),
        fallbackRule()
    )

    private fun fallbackRule(): DiagnosticRule = testRule(
        id = "smc_unknown_code_fallback",
        label = "Diagnóstico no concluyente (Código no catalogado)",
        confidence = ConfidenceLevel.UNKNOWN,
        verification = VerificationStatus.UNKNOWN,
        priority = 1,
        exactCodes = emptyList(),
        families = listOf(PanicFamily.SMC_BSC_FAILURE, PanicFamily.SMC_ASSERTION)
    )

    private fun testRule(
        id: String,
        label: String = id,
        active: Boolean = true,
        priority: Int = 100,
        profiles: List<String> = emptyList(),
        products: List<String> = emptyList(),
        families: List<PanicFamily> = listOf(PanicFamily.SMC_BSC_FAILURE, PanicFamily.SMC_ASSERTION),
        tokens: List<String> = emptyList(),
        exactCodes: List<String> = listOf("0x1000"),
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        verification: VerificationStatus = VerificationStatus.VERIFIED,
        primaryEligible: Boolean = true,
        allowBitmask: Boolean = false,
        suspected: List<SuspectedComponent> = emptyList()
    ): DiagnosticRule = DiagnosticRule(
        id = id,
        title = id,
        active = active,
        priority = priority,
        deviceScope = DeviceScope(diagnosticProfiles = profiles, productCodes = products),
        panicFamilies = families,
        sensorTokens = tokens,
        sensorCodesExact = exactCodes,
        diagnosis = DiagnosisDefinition(
            label = label,
            subsystem = "TEST",
            suspectedComponents = suspected,
            interpretation = "test"
        ),
        confidence = confidence,
        verificationStatus = verification,
        primaryEligible = primaryEligible,
        exactCodeOnly = !allowBitmask,
        allowBitmaskDecomposition = allowBitmask,
        repairFlow = RepairFlow(firstChecks = listOf("check"))
    )
}
