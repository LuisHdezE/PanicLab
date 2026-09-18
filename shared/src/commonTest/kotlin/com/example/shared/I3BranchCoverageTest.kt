package com.example.shared

import com.example.diagnostic.DiagnosticRulesEngine
import com.example.diagnostic.ExtractedSensors
import com.example.diagnostic.SensorExtractor
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceScope
import com.example.domain.model.DiagnosisDefinition
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.domain.model.RepairFlow
import com.example.domain.model.SensorCode
import com.example.domain.model.VerificationStatus
import com.example.ocr.OcrLogExtractor
import com.example.parser.EvidenceExtractor
import com.example.parser.MetadataExtractor
import com.example.platform.IdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class I3BranchCoverageTest {

    private class Ids : IdGenerator {
        private var n = 0
        override fun nextId(): String = "branch-${++n}"
    }

    @Test
    fun metadataCoversPrimaryJsonKeysEmptyJsonAndPanicOnlyJson() {
        val primary = MetadataExtractor.extract(
            """{"product":"iPhone14,4","os_version":"17.4","incident_id":"INC","panicString":"SMC PANIC","build":"21E236"}"""
        )
        assertEquals("iPhone14,4", primary.product)
        assertEquals("17.4", primary.osVersion)
        assertEquals("INC", primary.incidentId)

        val emptyObject = MetadataExtractor.extract("{}")
        assertNull(emptyObject.product)
        assertNull(emptyObject.panicString)

        val panicOnly = MetadataExtractor.extract("""{"panicString":"watchdog timeout"}""")
        assertEquals("watchdog timeout", panicOnly.panicString)
        assertNull(panicOnly.product)
    }

    @Test
    fun metadataRegexCoversSecondaryFieldsAndPanicLineFallback() {
        val text = """
            iPhone15,4
            bug_type: 210
            panicInitiator: kernel_task
            Kernel Version: Darwin 23.4.0
            socId: T8120
            timestamp: 2026-09-18T10:00:00Z
            repairStatus: pending
            roots_installed: 0
            watchdog timeout detected
        """.trimIndent()

        val result = MetadataExtractor.extract(text)
        assertEquals("iPhone15,4", result.product)
        assertEquals("210", result.bugType)
        assertEquals("kernel_task", result.panicInitiator)
        assertTrue(result.kernel.orEmpty().startsWith("Darwin"))
        assertEquals("T8120", result.socId)
        assertTrue(result.timestamp.orEmpty().startsWith("2026-09-18"))
        assertEquals("pending", result.repairStatus)
        assertEquals("0", result.rootsInstalled)
        assertTrue(result.panicString.orEmpty().contains("watchdog timeout"))
    }

    @Test
    fun sensorExtractorExercisesFallbackSeparatorsAndEmptySmcPath() {
        val sensors = SensorExtractor.extract(
            """
                Missing sensor text without a colon payload
                sensor array values is 4096
                sensor array values: 8192
                sensor array values=16384
                sensor array values 32768
            """.trimIndent()
        )

        assertTrue(4096L in sensors.sensorCodes.map { it.numericValue })
        assertTrue(8192L in sensors.sensorCodes.map { it.numericValue })
        assertTrue(16384L in sensors.sensorCodes.map { it.numericValue })
        assertTrue(32768L in sensors.sensorCodes.map { it.numericValue })

        val empty = SensorExtractor.extract("ordinary line without SMC")
        assertTrue(empty.smcSensorCodes.isEmpty())
        assertTrue(empty.sensorCodes.isEmpty())
    }

    @Test
    fun evidenceExtractorCoversEveryPanicKeywordAndMissingExcerptFallbacks() {
        val log = """
            Bosch control failure
            NMI POWER event
            ANS2 panic
            AppleSocHot Hot Hot Hot
            SEP ROM boot panic
            Undefined Kernel Instruction
            i2c timeout
            DCP iomfb display coprocessor
            baseband AppleBaseband failure
            watchdog timeout
            BSC failure
            ASSERTION FAILED
            thermalmonitord missing sensor
        """.trimIndent()

        val families = listOf(
            PanicFamily.AOP_BOSCH_CONTROL,
            PanicFamily.AOP_NMI_POWER,
            PanicFamily.ANS2,
            PanicFamily.APPLE_SOC_HOT,
            PanicFamily.SEP_ROM_BOOT,
            PanicFamily.UNDEFINED_KERNEL_INSTRUCTION,
            PanicFamily.I2C,
            PanicFamily.DCP_DISPLAY,
            PanicFamily.BASEBAND,
            PanicFamily.WATCHDOG_NO_CHECKIN,
            PanicFamily.SMC_BSC_FAILURE,
            PanicFamily.SMC_ASSERTION,
            PanicFamily.THERMAL_MISSING_SENSOR
        )
        val evidence = EvidenceExtractor.extractEvidences(
            logText = log,
            metadata = ParsedMetadata(),
            panicFamilies = families,
            extractedSensors = ExtractedSensors(
                missingSensorTokens = listOf("NOT_IN_LOG"),
                smcSensorCodes = listOf("4096")
            ),
            idGenerator = Ids()
        )

        assertEquals(13, evidence.count { it.type == "PANIC_SIGNATURE" })
        assertTrue(evidence.any { it.type == "MISSING_SENSOR" && it.lineNumber == -1 })
        assertTrue(evidence.any { it.type == "SMC_CODE" && it.lineNumber == -1 })
        assertFalse(evidence.any { it.type == "PRODUCT_CODE" })
    }

    @Test
    fun evidenceExtractorCoversProductFallbackAndMissingSignatureExcerpt() {
        val evidence = EvidenceExtractor.extractEvidences(
            logText = "unrelated",
            metadata = ParsedMetadata(product = "iPhone14,7"),
            panicFamilies = listOf(PanicFamily.I2C, PanicFamily.UNKNOWN),
            extractedSensors = ExtractedSensors(),
            idGenerator = Ids()
        )
        assertEquals(1, evidence.size)
        assertEquals("PRODUCT_CODE", evidence.single().type)
        assertEquals(-1, evidence.single().lineNumber)
        assertEquals("product: iPhone14,7", evidence.single().excerpt)
    }

    @Test
    fun rulesEngineCoversNoContextFallbackMissingAndSmcCodesParsingBranch() {
        val noContext = DiagnosticRulesEngine.evaluate(
            deviceModel = null,
            productCode = null,
            panicFamilies = emptyList(),
            extractedSensors = ExtractedSensors(),
            allRules = emptyList()
        )
        assertNull(noContext.primaryRule)
        assertTrue(noContext.matchedRules.isEmpty())

        val smcWithoutFallbackRule = DiagnosticRulesEngine.evaluate(
            deviceModel = null,
            productCode = null,
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            extractedSensors = ExtractedSensors(smcSensorCodes = listOf("0x1000")),
            allRules = listOf(rule("wrong", codes = listOf("0x2000")))
        )
        assertNull(smcWithoutFallbackRule.primaryRule)

        val parsedFromStrings = DiagnosticRulesEngine.evaluate(
            deviceModel = null,
            productCode = null,
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            extractedSensors = ExtractedSensors(smcSensorCodes = listOf("4096")),
            allRules = listOf(rule("match", codes = listOf("0x1000")))
        )
        assertEquals("match", parsedFromStrings.primaryRule?.id)
    }

    @Test
    fun rulesEngineCoversScopeVariantsAndFamilyEmptyRules() {
        val productRule = rule("product", products = listOf("iPhone14,7"), families = emptyList())
        val profileRule = rule("profile", profiles = listOf("SMC_14_BASE"), families = emptyList(), priority = 90)
        val universalRule = rule("universal", families = emptyList(), priority = 80)

        val device = com.example.parser.DeviceResolver.resolveSynchronous("iPhone14,7")
        val matched = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = "iPhone14,7",
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            extractedSensors = ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(4096))),
            allRules = listOf(productRule, profileRule, universalRule)
        )
        assertEquals("product", matched.primaryRule?.id)
        assertEquals(3, matched.matchedRules.size)

        val noProduct = DiagnosticRulesEngine.evaluate(
            deviceModel = null,
            productCode = null,
            panicFamilies = listOf(PanicFamily.SMC_BSC_FAILURE),
            extractedSensors = ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(4096))),
            allRules = listOf(productRule)
        )
        assertNull(noProduct.primaryRule)
    }

    @Test
    fun rulesEngineCoversBitmaskEarlyReturnsAndComponentFilters() {
        val bit = rule("bit", codes = listOf("0x1000"), profiles = listOf("SMC_13"), allowBitmask = true)
        val invalidBit = rule("invalid", codes = listOf("not-a-code"), profiles = listOf("SMC_13"), allowBitmask = true)
        val zeroBit = rule("zero", codes = listOf("0"), profiles = listOf("SMC_13"), allowBitmask = true)
        val noBitmask = rule("disabled", codes = listOf("0x1000"), profiles = listOf("SMC_13"), allowBitmask = false)
        val emptyCodes = rule("empty", codes = emptyList(), profiles = listOf("SMC_13"), allowBitmask = true)
        val wrongScope = rule("wrong-scope", codes = listOf("0x1000"), profiles = listOf("SMC_14_PRO"), allowBitmask = true)

        val nullProfile = DiagnosticRulesEngine.evaluate(
            null, null, listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000))),
            listOf(bit)
        )
        assertNull(nullProfile.primaryRule)

        val disallowedProfile = DiagnosticRulesEngine.evaluate(
            com.example.parser.DeviceResolver.resolveSynchronous("iPhone14,7"), "iPhone14,7",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000))),
            listOf(bit)
        )
        assertNull(disallowedProfile.primaryRule)

        val zeroCode = DiagnosticRulesEngine.evaluate(
            com.example.parser.DeviceResolver.resolveSynchronous("iPhone14,5"), "iPhone14,5",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0))),
            listOf(bit)
        )
        assertNull(zeroCode.primaryRule)

        val filtered = DiagnosticRulesEngine.evaluate(
            com.example.parser.DeviceResolver.resolveSynchronous("iPhone14,5"), "iPhone14,5",
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(sensorCodes = listOf(SensorCode.fromNumeric(0x3000))),
            listOf(bit, invalidBit, zeroBit, noBitmask, emptyCodes, wrongScope)
        )
        assertTrue("bit" in filtered.appliedRuleIds)
        assertFalse("invalid" in filtered.appliedRuleIds)
        assertFalse("zero" in filtered.appliedRuleIds)
    }

    @Test
    fun rulesEngineCoversLowUnknownMediumRankingAndExistingFallback() {
        val highNotPrimary = rule("high-np", confidence = ConfidenceLevel.HIGH, primary = false, priority = 10, codes = emptyList())
        val mediumNotPrimary = rule("medium-np", confidence = ConfidenceLevel.MEDIUM, primary = false, priority = 10, codes = emptyList())
        val lowPrimary = rule("low", confidence = ConfidenceLevel.LOW, priority = 30, codes = emptyList())
        val unknownPrimary = rule("unknown", confidence = ConfidenceLevel.UNKNOWN, priority = 20, codes = emptyList())
        val fallback = rule("smc_unknown_code_fallback", confidence = ConfidenceLevel.UNKNOWN, priority = 1, codes = emptyList())

        val result = DiagnosticRulesEngine.evaluate(
            null, null,
            listOf(PanicFamily.SMC_BSC_FAILURE),
            ExtractedSensors(),
            listOf(highNotPrimary, mediumNotPrimary, lowPrimary, unknownPrimary, fallback)
        )
        assertTrue(result.appliedRuleIds.contains("smc_unknown_code_fallback"))
        assertEquals("low", result.primaryRule?.id)
    }

    @Test
    fun ocrCoversAllKeywordDetectorsAndNoModelBuildBranches() {
        val result = OcrLogExtractor.processScannedText(
            """
                I2C timeout
                WDT watchdog
                THERMAL PRESSURE
                AOP PANIC ALWAYS ON
                SEP PANIC SECURE ENCLAVE
                random line
            """.trimIndent()
        )
        assertNull(result.detectedDeviceModel)
        assertNull(result.detectedBuild)
        assertTrue(result.detectedKeywords.contains("I2C Bus Timeout"))
        assertTrue(result.detectedKeywords.contains("Watchdog Reset"))
        assertTrue(result.detectedKeywords.contains("Sensor Térmico"))
        assertTrue(result.detectedKeywords.contains("AOP Panic"))
        assertTrue(result.detectedKeywords.contains("SEP Panic"))
    }

    @Test
    fun ocrCoversMacosStructuredBranchAndDefaultBuildInsertion() {
        val macos = OcrLogExtractor.processScannedText("panic(cpu 0): failure\nmacOS version: 14")
        assertEquals("panic(cpu 0): failure\nmacOS version: 14", macos.cleanedText)

        val noBuild = OcrLogExtractor.processScannedText("iPhone14,7\nSMC PANIC")
        assertTrue(noBuild.cleanedText.contains("\"build\":\"21D61\""))
    }

    private fun rule(
        id: String,
        codes: List<String> = listOf("0x1000"),
        profiles: List<String> = emptyList(),
        products: List<String> = emptyList(),
        families: List<PanicFamily> = listOf(PanicFamily.SMC_BSC_FAILURE),
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        primary: Boolean = true,
        priority: Int = 100,
        allowBitmask: Boolean = false
    ) = DiagnosticRule(
        id = id,
        title = id,
        active = true,
        priority = priority,
        deviceScope = DeviceScope(diagnosticProfiles = profiles, productCodes = products),
        panicFamilies = families,
        sensorTokens = emptyList(),
        sensorCodesExact = codes,
        diagnosis = DiagnosisDefinition(label = id, subsystem = "TEST", interpretation = "test"),
        confidence = confidence,
        verificationStatus = VerificationStatus.VERIFIED,
        primaryEligible = primary,
        exactCodeOnly = !allowBitmask,
        allowBitmaskDecomposition = allowBitmask,
        repairFlow = RepairFlow()
    )
}
