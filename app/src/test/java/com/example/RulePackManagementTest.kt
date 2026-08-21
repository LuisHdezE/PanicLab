package com.example

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.RuleDiffItem
import com.example.domain.model.RulePackOrigin
import com.example.domain.model.VerificationStatus
import com.example.util.RulePackDiffCalculator
import com.example.util.RulePackJsonParser
import com.example.util.RulePackValidator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RulePackManagementTest {

    private val validRulePackJson = """
    {
      "schemaVersion": 1,
      "knowledgeBaseVersion": "1.1.0",
      "title": "PanicLab Rule Pack Test v1.1.0",
      "generatedAt": "2026-08-21T12:00:00Z",
      "locale": "es-UY",
      "enginePolicy": {
        "deterministic": true
      },
      "sources": [
        {
          "id": "src_ifixit_test",
          "title": "iFixit Wiki Test",
          "publisher": "iFixit",
          "url": "https://www.ifixit.com/Wiki/Test",
          "checkedAt": "2026-08-21",
          "trustLevel": "HIGH",
          "supports": ["SMC Panic"]
        }
      ],
      "deviceModels": [
        {
          "productCode": "iPhone14,4",
          "marketingName": "iPhone 13 mini",
          "family": "IPHONE_13_MINI",
          "variant": "13_MINI",
          "diagnosticProfile": "SMC_13_MINI",
          "releaseYear": 2021,
          "sourceIds": ["src_ifixit_test"]
        }
      ],
      "panicClassifiers": [
        {
          "id": "classifier_smc_bsc",
          "family": "SMC_BSC_FAILURE",
          "priority": 100,
          "match": {
            "allTerms": ["SMC", "BSC failure"]
          },
          "notes": "Test classifier"
        }
      ],
      "bitmaskPolicies": [
        {
          "diagnosticProfile": "SMC_13_MINI",
          "enabled": true,
          "knownBits": ["0x400", "0x800", "0x1000"],
          "exactRulesAlwaysWin": true
        }
      ],
      "diagnosticRules": [
        {
          "id": "smc13_0x1000_front_sensor",
          "title": "iPhone 13: 0x1000",
          "active": true,
          "priority": 100,
          "deviceScope": {
            "diagnosticProfiles": ["SMC_13_MINI"],
            "productCodes": []
          },
          "match": {
            "panicFamiliesAny": ["SMC_BSC_FAILURE"],
            "sensorTokensAny": [],
            "sensorCodesExactAny": ["0x1000"],
            "requiredTermsAll": [],
            "rawTermsAny": []
          },
          "diagnosis": {
            "label": "Sensor frontal / proximidad",
            "subsystem": "SMC_SENSOR",
            "suspectedComponents": [
              { "name": "Front Sensor / Proximity Assembly", "role": "PRIMARY" }
            ],
            "interpretation": "Sensor frontal iPhone 13 mini"
          },
          "confidence": "HIGH",
          "verificationStatus": "WELL_DOCUMENTED",
          "primaryEligible": true,
          "exactCodeOnly": true,
          "allowBitmaskDecomposition": false,
          "repairFlow": {
            "firstChecks": ["Inspeccionar flex frontal"],
            "knownGoodTest": "Probar flex conocido como bueno",
            "boardLevelNextSteps": ["Modo diodo en FPC"],
            "cautions": []
          },
          "sourceIds": ["src_ifixit_test"],
          "notes": null
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testValidRulePackParsingAndValidation() {
        val parseResult = RulePackJsonParser.parse(
            jsonString = validRulePackJson,
            origin = RulePackOrigin.USER_IMPORTED,
            sourceFilename = "test_pack.json"
        )
        val validation = RulePackValidator.validate(parseResult.parsedPack)

        assertTrue("Validation should pass for valid RulePack", validation.isValid)
        assertEquals(0, validation.errors.size)
        assertEquals("1.1.0", validation.knowledgeBaseVersion)
        assertEquals(1, validation.rulesCount)
        assertEquals(1, validation.modelsCount)
        assertEquals(1, validation.classifiersCount)
        assertEquals(1, validation.sourcesCount)
        assertEquals(64, validation.checksum.length) // SHA-256 length
    }

    @Test
    fun testValidationCatchesInvalidSemVerAndMissingSource() {
        val invalidJson = validRulePackJson
            .replace("\"1.1.0\"", "\"1.1-beta\"") // Invalid SemVer
            .replace("\"sourceIds\": [\"src_ifixit_test\"]", "\"sourceIds\": [\"non_existent_source_id\"]") // Missing source

        val parseResult = RulePackJsonParser.parse(invalidJson)
        val validation = RulePackValidator.validate(parseResult.parsedPack)

        assertFalse("Validation must fail when invalid SemVer and missing sourceId are present", validation.isValid)
        assertTrue("Must contain SemVer error", validation.errors.any { it.path == "knowledgeBaseVersion" })
        assertTrue("Must contain missing sourceId error", validation.errors.any { it.path.contains("sourceIds") })
    }

    @Test
    fun testValidationCatchesDuplicateRuleId() {
        val duplicateRulesJson = validRulePackJson.replace(
            "\"diagnosticRules\": [",
            "\"diagnosticRules\": [\n" +
                    """
                    {
                      "id": "smc13_0x1000_front_sensor",
                      "title": "Duplicated Rule",
                      "active": true,
                      "priority": 100,
                      "deviceScope": { "diagnosticProfiles": [], "productCodes": [] },
                      "match": { "panicFamiliesAny": [], "sensorTokensAny": [], "sensorCodesExactAny": [], "requiredTermsAll": [], "rawTermsAny": [] },
                      "diagnosis": { "label": "Dup", "subsystem": "GEN", "suspectedComponents": [], "interpretation": "" },
                      "confidence": "HIGH",
                      "verificationStatus": "WELL_DOCUMENTED",
                      "primaryEligible": true,
                      "exactCodeOnly": true,
                      "allowBitmaskDecomposition": false,
                      "repairFlow": { "firstChecks": [], "knownGoodTest": null, "boardLevelNextSteps": [], "cautions": [] },
                      "sourceIds": ["src_ifixit_test"],
                      "notes": null
                    },
                    """.trimIndent()
        )

        val parseResult = RulePackJsonParser.parse(duplicateRulesJson)
        val validation = RulePackValidator.validate(parseResult.parsedPack)

        assertFalse("Validation must fail on duplicate rule IDs", validation.isValid)
        assertTrue("Must report duplicate rule ID error", validation.errors.any { it.message.contains("duplicado") })
    }

    @Test
    fun testDiffCalculationBetweenVersions() {
        val basePack = RulePackJsonParser.parse(validRulePackJson).parsedPack

        // Create updated version 1.2.0 with 1 new rule and 1 modified confidence
        val updatedJson = validRulePackJson
            .replace("\"1.1.0\"", "\"1.2.0\"")
            .replace("\"WELL_DOCUMENTED\"", "\"VERIFIED\"")
            .replace(
                "\"diagnosticRules\": [",
                "\"diagnosticRules\": [\n" +
                        """
                        {
                          "id": "smc13_0x800_charge_port",
                          "title": "iPhone 13: 0x800",
                          "active": true,
                          "priority": 100,
                          "deviceScope": { "diagnosticProfiles": ["SMC_13_MINI"], "productCodes": [] },
                          "match": { "panicFamiliesAny": ["SMC_BSC_FAILURE"], "sensorTokensAny": [], "sensorCodesExactAny": ["0x800"], "requiredTermsAll": [], "rawTermsAny": [] },
                          "diagnosis": { "label": "Puerto de carga", "subsystem": "SMC_SENSOR", "suspectedComponents": [], "interpretation": "" },
                          "confidence": "HIGH",
                          "verificationStatus": "WELL_DOCUMENTED",
                          "primaryEligible": true,
                          "exactCodeOnly": true,
                          "allowBitmaskDecomposition": false,
                          "repairFlow": { "firstChecks": [], "knownGoodTest": null, "boardLevelNextSteps": [], "cautions": [] },
                          "sourceIds": ["src_ifixit_test"],
                          "notes": null
                        },
                        """.trimIndent()
            )

        val updatedPack = RulePackJsonParser.parse(updatedJson).parsedPack
        val diff = RulePackDiffCalculator.calculateDiff(basePack, updatedPack)

        assertEquals("1.1.0", diff.currentVersion)
        assertEquals("1.2.0", diff.incomingVersion)
        assertEquals(1, diff.addedRulesCount)
        assertEquals(1, diff.modifiedRulesCount)
        assertEquals(0, diff.removedRulesCount)
        assertEquals(2, diff.ruleDiffs.size)

        val addedItem = diff.ruleDiffs.find { it.ruleId == "smc13_0x800_charge_port" }
        assertNotNull(addedItem)
        assertEquals(RuleDiffItem.ChangeType.ADDED, addedItem?.changeType)

        val modifiedItem = diff.ruleDiffs.find { it.ruleId == "smc13_0x1000_front_sensor" }
        assertNotNull(modifiedItem)
        assertEquals(RuleDiffItem.ChangeType.MODIFIED, modifiedItem?.changeType)
        assertTrue(modifiedItem?.statusChange?.contains("VERIFIED") == true)
    }

    @Test
    fun testUnknownCaseRedactorSanitizesSensitiveData() {
        val testRawLog = """
            {"incident_id":"A1B2C3D4-E5F6-7890-ABCD-EF1234567890","crashreporter_key":"a1b2c3d4e5f678901234567890abcdef12345678"}
            Date/Time: 2026-08-21 14:00:00
            Incident Identifier: A1B2C3D4-E5F6-7890-ABCD-EF1234567890
            MAC Address: 00:1A:2B:3C:4D:5E
            IMEI: 356938035643803
            Serial Number: F2LK89ABCDEF
            FilePath: /Users/john_smith/Desktop/logs/panic.ips
            S.sensor array 0 - 6 is 0x0, 0x1000, 0x0
        """.trimIndent()

        val sanitized = com.example.util.UnknownCaseRedactor.sanitizeRawLog(testRawLog)

        assertFalse("UUID must be redacted", sanitized.contains("A1B2C3D4-E5F6-7890-ABCD-EF1234567890"))
        assertFalse("CrashReporter key must be redacted", sanitized.contains("a1b2c3d4e5f678901234567890abcdef12345678"))
        assertFalse("MAC address must be redacted", sanitized.contains("00:1A:2B:3C:4D:5E"))
        assertFalse("IMEI must be redacted", sanitized.contains("356938035643803"))
        assertFalse("User path must be redacted", sanitized.contains("john_smith"))
        assertTrue("Sensor code must be preserved for diagnosis", sanitized.contains("0x1000"))
        assertTrue("Sensor array must be preserved", sanitized.contains("sensor array"))
    }
}
