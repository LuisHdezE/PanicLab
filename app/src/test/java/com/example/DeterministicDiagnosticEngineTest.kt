package com.example

import com.example.diagnostic.CandidateRanker
import com.example.diagnostic.DiagnosticReportBuilder
import com.example.diagnostic.DiagnosticRulesEngine
import com.example.diagnostic.SensorExtractor
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.PanicFamily
import com.example.domain.model.VerificationStatus
import com.example.parser.DeviceResolver
import com.example.parser.EvidenceExtractor
import com.example.parser.LogNormalizer
import com.example.parser.MetadataExtractor
import com.example.parser.PanicClassifier
import com.example.util.HexUtils
import com.example.util.RulePackJsonParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeterministicDiagnosticEngineTest {

    private lateinit var parsedPack: com.example.util.RulePackParseResult

    private val sampleJsonRulePack = """
    {
      "schemaVersion": 1,
      "knowledgeBaseVersion": "1.0.0",
      "title": "PanicLab Core Diagnostic Rule Pack",
      "generatedAt": "2026-03-30T12:00:00Z",
      "deviceModels": [
        {
          "productCode": "iPhone10,3",
          "marketingName": "iPhone X",
          "family": "IPHONE_X",
          "variant": "X",
          "diagnosticProfile": "THERMAL_CLASSIC_X_TO_12",
          "releaseYear": 2017
        },
        {
          "productCode": "iPhone14,4",
          "marketingName": "iPhone 13 mini",
          "family": "IPHONE_13_MINI",
          "variant": "13_MINI",
          "diagnosticProfile": "SMC_13_MINI",
          "releaseYear": 2021
        },
        {
          "productCode": "iPhone14,7",
          "marketingName": "iPhone 14",
          "family": "IPHONE_14",
          "variant": "14",
          "diagnosticProfile": "SMC_14_BASE",
          "releaseYear": 2022
        },
        {
          "productCode": "iPhone17,1",
          "marketingName": "iPhone 16 Pro",
          "family": "IPHONE_16_PRO",
          "variant": "16_PRO",
          "diagnosticProfile": "SMC_16_PRO",
          "releaseYear": 2024
        }
      ],
      "diagnosticRules": [
        {
          "id": "iphone13_mini_0x1000_dock_mic",
          "title": "iPhone 13 mini — Fallo Sensor SMC 0x1000 (Micrófono Inferior / Dock Flex)",
          "active": true,
          "priority": 120,
          "deviceScope": {
            "diagnosticProfiles": ["SMC_13_MINI"],
            "productCodes": ["iPhone14,4"]
          },
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE", "SMC_ASSERTION"],
            "sensorCodesExactAny": ["0x1000", "4096"]
          },
          "diagnosis": {
            "label": "Micrófono Inferior / Flex de Carga (Mic1 / Dock)",
            "subsystem": "AUDIO_DOCK",
            "suspectedComponents": [
              { "name": "Flex de puerto de carga (Dock Connector)", "role": "PRIMARY" },
              { "name": "Circuito NTC / Micrófono 1 inferior", "role": "SECONDARY" }
            ],
            "interpretation": "El código 0x1000 en iPhone 13 mini corresponde al sensor térmico del micrófono inferior."
          },
          "confidence": "HIGH",
          "verificationStatus": "VERIFIED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false
        },
        {
          "id": "smc14base_0x400000_wireless_charge_coil",
          "title": "iPhone 14/14 Plus: 0x400000 (Wireless Charging Coil)",
          "active": true,
          "priority": 120,
          "deviceScope": {
            "diagnosticProfiles": ["SMC_14_BASE"],
            "productCodes": []
          },
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE", "SMC_ASSERTION"],
            "sensorCodesExactAny": ["0x400000"]
          },
          "diagnosis": {
            "label": "Wireless Charging Coil",
            "subsystem": "SMC_SENSOR",
            "suspectedComponents": [
              { "name": "Wireless Charging Coil", "role": "PRIMARY" }
            ],
            "interpretation": "Código 0x400000 correspondiente a la bobina de carga inalámbrica."
          },
          "confidence": "HIGH",
          "verificationStatus": "VERIFIED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false
        },
        {
          "id": "iphone14_base_0x500000_battery",
          "title": "iPhone 14 / 14 Plus — Fallo SMC 0x500000 (Batería / Gas Gauge)",
          "active": true,
          "priority": 130,
          "deviceScope": {
            "diagnosticProfiles": ["SMC_14_BASE"],
            "productCodes": ["iPhone14,7", "iPhone14,8"]
          },
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE", "SMC_ASSERTION"],
            "sensorCodesExactAny": ["0x500000", "5242880"]
          },
          "diagnosis": {
            "label": "Batería / Línea I2C Gas Gauge (BATT_HDQ / BATT_SWI)",
            "subsystem": "BATTERY_POWER",
            "suspectedComponents": [
              { "name": "Batería (BMS / Gas Gauge)", "role": "PRIMARY" }
            ],
            "interpretation": "En iPhone 14 y 14 Plus, el código 0x500000 es exacto para el subsistema de batería."
          },
          "confidence": "HIGH",
          "verificationStatus": "VERIFIED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false
        },
        {
          "id": "iphone16_pro_3145728_battery_prox",
          "title": "iPhone 16 Pro — Fallo Sensor Array 3145728 (0x300000)",
          "active": true,
          "priority": 125,
          "deviceScope": {
            "diagnosticProfiles": ["SMC_16_PRO"],
            "productCodes": ["iPhone17,1"]
          },
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE", "SMC_ASSERTION"],
            "sensorCodesExactAny": ["3145728", "0x300000"]
          },
          "diagnosis": {
            "label": "Batería + Sensor Proximidad / Auricular Superior",
            "subsystem": "BATTERY_POWER_AND_PROX",
            "suspectedComponents": [
              { "name": "Batería (BMS / Gas Gauge)", "role": "PRIMARY" },
              { "name": "Flex de Sensor de Proximidad / Auricular", "role": "SECONDARY" }
            ],
            "interpretation": "Valor decimal 3145728 (0x300000 en hex) en iPhone 16 Pro."
          },
          "confidence": "HIGH",
          "verificationStatus": "VERIFIED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false
        },
        {
          "id": "prs0_missing_sensor_rule",
          "title": "Pánico Térmico PRS0 (Barómetro / Flex de Carga)",
          "active": true,
          "priority": 100,
          "deviceScope": {
            "diagnosticProfiles": ["THERMAL_CLASSIC_X_TO_12"]
          },
          "match": {
            "panicFamiliesAny": ["THERMAL_MISSING_SENSOR"],
            "sensorTokensAny": ["PRS0"]
          },
          "diagnosis": {
            "label": "Barómetro / Flex de Carga (PRS0)",
            "subsystem": "THERMAL_BAROMETER",
            "suspectedComponents": [
              { "name": "Flex de puerto de carga (Barómetro)", "role": "PRIMARY" }
            ],
            "interpretation": "Sensor de presión barométrica PRS0 faltante."
          },
          "confidence": "HIGH",
          "verificationStatus": "VERIFIED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false
        },
        {
          "id": "smc_unknown_code_fallback",
          "title": "Fallo SMC con Código No Mapeado (Fallback Seguro)",
          "active": true,
          "priority": 1,
          "deviceScope": {},
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE", "SMC_ASSERTION"]
          },
          "diagnosis": {
            "label": "Diagnóstico no concluyente (Código no catalogado)",
            "subsystem": "SMC_UNKNOWN",
            "suspectedComponents": [],
            "interpretation": "Se ha detectado una parada de pánico SMC pero el código específico no está asociado a un componente confirmado."
          },
          "confidence": "UNKNOWN",
          "verificationStatus": "UNKNOWN",
          "primaryEligible": true,
          "exactCodeOnly": false,
          "allowBitmaskDecomposition": false
        }
      ]
    }
    """.trimIndent()

    @Before
    fun setUp() {
        parsedPack = RulePackJsonParser.parse(sampleJsonRulePack)
    }

    @Test
    fun testHexUtilsConversions() {
        assertEquals(4096L, HexUtils.parseCodeToLong("0x1000"))
        assertEquals(3145728L, HexUtils.parseCodeToLong("3145728"))
        assertEquals("0x1000", HexUtils.toCanonicalHex("4096"))
        assertEquals("0x300000", HexUtils.toCanonicalHex("3145728"))
        assertTrue(HexUtils.areCodesEquivalent("3145728", "0x300000"))
        assertTrue(HexUtils.areCodesEquivalent("0x1000", "4096"))
    }

    @Test
    fun testIPhone13Mini0x1000Diagnosis() {
        val rawLog = """
            {"bug_type":"210","os_version":"iPhone OS 17.4 (21E236)","product":"iPhone14,4","build":"21E236"}
            panic(cpu 0): "SMC PANIC - BSC failure at address 0x1000 - S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized)
        val evidences = EvidenceExtractor.extractEvidences(normalized, metadata, families, sensors)

        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Micrófono Inferior / Flex de Carga (Mic1 / Dock)", primary?.label)
        assertEquals(ConfidenceLevel.HIGH, primary?.confidence)
        assertEquals(VerificationStatus.VERIFIED, primary?.verificationStatus)
        assertEquals("Flex de puerto de carga (Dock Connector)", primary?.suspectedComponents?.first()?.name)
    }

    @Test
    fun testIPhone14Exact0x500000BatteryDiagnosis() {
        val rawLog = """
            {"bug_type":"210","product":"iPhone14,7","os_version":"17.3"}
            panic(cpu 1): "SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized)

        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Batería / Línea I2C Gas Gauge (BATT_HDQ / BATT_SWI)", primary?.label)
        assertEquals(ConfidenceLevel.HIGH, primary?.confidence)
    }

    @Test
    fun testIPhone16ProDecimal3145728Diagnosis() {
        val rawLog = """
            {"bug_type":"210","product":"iPhone17,1","os_version":"18.0"}
            panic(cpu 2): "SMC PANIC - BSC failure: S.sensor array 0 - 7 is 0, 3145728, 0, 0, 0"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized)

        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Batería + Sensor Proximidad / Auricular Superior", primary?.label)
    }

    @Test
    fun testIPhoneXMissingSensorPRS0Diagnosis() {
        val rawLog = """
            {"bug_type":"210","product":"iPhone10,3"}
            panic(cpu 0): "thermalmonitord: Missing sensor(s): PRS0, thermal runaway"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized)

        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Barómetro / Flex de Carga (PRS0)", primary?.label)
    }

    @Test
    fun testUnknownSMCCodeProducesNonConclusiveDiagnosisWithoutInventing() {
        val rawLog = """
            {"bug_type":"210","product":"iPhone14,7"}
            panic(cpu 0): "SMC PANIC - BSC failure at address 0x987654 - S.sensor array is 0x0, 0x987654"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized)

        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Diagnóstico no concluyente (Código no catalogado)", primary?.label)
        assertEquals(ConfidenceLevel.UNKNOWN, primary?.confidence)
        assertTrue(primary?.suspectedComponents?.isEmpty() == true)
    }

    @Test
    fun testSensorCodeModelParsing() {
        val fromDecimal = com.example.domain.model.SensorCode.parse("4194304")
        assertNotNull(fromDecimal)
        assertEquals("4194304", fromDecimal?.rawValue)
        assertEquals(4194304L, fromDecimal?.numericValue)
        assertEquals("0x400000", fromDecimal?.hexadecimal)
        assertEquals("4194304", fromDecimal?.decimal)

        val fromHex = com.example.domain.model.SensorCode.parse("0x400000")
        assertNotNull(fromHex)
        assertEquals("0x400000", fromHex?.rawValue)
        assertEquals(4194304L, fromHex?.numericValue)
        assertEquals("0x400000", fromHex?.hexadecimal)
        assertEquals("4194304", fromHex?.decimal)
    }

    @Test
    fun testHexUtilsEquivalencesRequiredBySpec() {
        assertEquals(HexUtils.parse("0x1000"), HexUtils.parse("4096"))
        assertEquals(HexUtils.parse("0x80000"), HexUtils.parse("524288"))
        assertEquals(HexUtils.parse("0x100000"), HexUtils.parse("1048576"))
        assertEquals(HexUtils.parse("0x200000"), HexUtils.parse("2097152"))
        assertEquals(HexUtils.parse("0x300000"), HexUtils.parse("3145728"))
        assertEquals(HexUtils.parse("0x400000"), HexUtils.parse("4194304"))
        assertEquals(HexUtils.parse("0x500000"), HexUtils.parse("5242880"))

        assertTrue(HexUtils.areCodesEquivalent("4096", "0x1000"))
        assertTrue(HexUtils.areCodesEquivalent("524288", "0x80000"))
        assertTrue(HexUtils.areCodesEquivalent("1048576", "0x100000"))
        assertTrue(HexUtils.areCodesEquivalent("2097152", "0x200000"))
        assertTrue(HexUtils.areCodesEquivalent("3145728", "0x300000"))
        assertTrue(HexUtils.areCodesEquivalent("4194304", "0x400000"))
        assertTrue(HexUtils.areCodesEquivalent("5242880", "0x500000"))
    }

    @Test
    fun testDynamicSensorArrayExtractionLengthsAndFormats() {
        // 0 - 5 format with decimal
        val sensors5 = SensorExtractor.extract("S.sensor array 0 - 5 is 0, 4194304, 0, 0, 0")
        assertEquals(listOf("4194304"), sensors5.smcSensorCodes)
        assertEquals(4194304L, sensors5.sensorCodes.first().numericValue)
        assertEquals("0x400000", sensors5.sensorCodes.first().hexadecimal)

        // 0 - 6 format with hex
        val sensors6 = SensorExtractor.extract("S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0, 0x0, 0x0, 0x0")
        assertEquals(listOf("0x1000"), sensors6.smcSensorCodes)
        assertEquals(4096L, sensors6.sensorCodes.first().numericValue)

        // 0 - 7 format with decimal
        val sensors7 = SensorExtractor.extract("S.sensor array 0 - 7 is 0, 3145728, 0, 0, 0, 0, 0")
        assertEquals(listOf("3145728"), sensors7.smcSensorCodes)
        assertEquals(3145728L, sensors7.sensorCodes.first().numericValue)
        assertEquals("0x300000", sensors7.sensorCodes.first().hexadecimal)

        // 0 - 12 format with double digit N
        val sensors12 = SensorExtractor.extract("S.sensor array 0 - 12 is 0x0, 0x2000, 0x0")
        assertEquals(listOf("0x2000"), sensors12.smcSensorCodes)
        assertEquals(0x2000L, sensors12.sensorCodes.first().numericValue)

        // Mixed hex and decimal elements
        val sensorsMixed = SensorExtractor.extract("S.sensor array 0 - 4 is 0x0, 4194304, 0x1000, 0")
        assertEquals(listOf("4194304", "0x1000"), sensorsMixed.smcSensorCodes)
        assertEquals(2, sensorsMixed.sensorCodes.size)
    }

    @Test
    fun testIPhone14WirelessCoilDecimalRegression() {
        val rawLog = """
            "product":"iPhone14,7"
            "panicString":"SMC PANIC - ASSERT: SMC BSC failure\nS.sensor array 0 - 5 is 0, 4194304, 0, 0, 0"
        """.trimIndent()

        val normalized = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalized)
        val device = DeviceResolver.resolveSynchronous(metadata.product)
        val families = PanicClassifier.classify(normalized, metadata.panicString)
        val sensors = SensorExtractor.extract(normalized, metadata.panicString)
        val evidences = EvidenceExtractor.extractEvidences(normalized, metadata, families, sensors)

        // Assert device
        assertEquals("iPhone14,7", metadata.product)
        assertEquals("iPhone 14", device?.marketingName)
        assertEquals("SMC_14_BASE", device?.diagnosticProfile)

        // Assert panic family
        assertTrue(families.contains(PanicFamily.SMC_BSC_FAILURE))

        // Assert sensor codes
        assertEquals(1, sensors.sensorCodes.size)
        val extractedCode = sensors.sensorCodes.first()
        assertEquals("4194304", extractedCode.rawValue)
        assertEquals("4194304", extractedCode.decimal)
        assertEquals("0x400000", extractedCode.hexadecimal)
        assertEquals(4194304L, extractedCode.numericValue)

        // Assert evaluation against sample rule pack (which has only "0x400000" in sensorCodesExactAny)
        val matchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = metadata.product,
            panicFamilies = families,
            extractedSensors = sensors,
            allRules = parsedPack.parsedPack.diagnosticRules
        )

        val (primary, _) = CandidateRanker.toCandidates(matchResult.primaryRule, matchResult.alternativeRules)

        assertNotNull(primary)
        assertEquals("Wireless Charging Coil", primary?.label)
        assertEquals(ConfidenceLevel.HIGH, primary?.confidence)
        assertEquals(VerificationStatus.VERIFIED, primary?.verificationStatus)
        assertEquals("Wireless Charging Coil", primary?.suspectedComponents?.first()?.name)

        // Assert evidence normalization
        val smcEvidence = evidences.find { it.type == "SMC_CODE" }
        assertNotNull(smcEvidence)
        assertEquals("4194304", smcEvidence?.rawValue)
        assertEquals("0x400000", smcEvidence?.normalizedValue)
    }

    @Test
    fun testPanicCodeHelperClass() {
        val codeFromDec = com.example.domain.model.PanicCode.parse("4194304")
        assertNotNull(codeFromDec)
        assertEquals(4194304L, codeFromDec?.numericValue)
        assertEquals("0x400000", codeFromDec?.hexadecimal)
        assertEquals("4194304", codeFromDec?.decimal)

        val codeFromHex = com.example.domain.model.PanicCode.parse("0x400000")
        assertNotNull(codeFromHex)
        assertEquals(4194304L, codeFromHex?.numericValue)
        assertEquals("0x400000", codeFromHex?.hexadecimal)
        assertEquals("4194304", codeFromHex?.decimal)

        assertEquals(codeFromDec?.numericValue, codeFromHex?.numericValue)

        val codeFromNum = com.example.domain.model.PanicCode.fromNumeric(4194304L)
        assertEquals(4194304L, codeFromNum.numericValue)
        assertEquals("0x400000", codeFromNum.hexadecimal)
        assertEquals("4194304", codeFromNum.decimal)
    }
}
