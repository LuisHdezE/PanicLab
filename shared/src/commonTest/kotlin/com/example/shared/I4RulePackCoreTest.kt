package com.example.shared

import com.example.domain.model.BitmaskPolicy
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceModel
import com.example.domain.model.DeviceScope
import com.example.domain.model.DiagnosisDefinition
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicClassifier
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedRulePack
import com.example.domain.model.RepairFlow
import com.example.domain.model.RuleDiffItem
import com.example.domain.model.RulePackOrigin
import com.example.domain.model.RulePackSource
import com.example.domain.model.SuspectedComponent
import com.example.domain.model.VerificationStatus
import com.example.platform.Sha256Hasher
import com.example.rulepack.RulePackDiffCalculator
import com.example.rulepack.RulePackJsonParser
import com.example.rulepack.RulePackValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class I4RulePackCoreTest {
    private val hasher = Sha256Hasher { "a".repeat(64) }

    private val validJson = """
        {
          "schemaVersion": 1,
          "knowledgeBaseVersion": "1.1.0",
          "title": "PanicLab Rule Pack Test",
          "generatedAt": "2026-08-21T12:00:00Z",
          "locale": "es-UY",
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
                "anyTerms": ["panic"],
                "allTerms": ["SMC", "BSC failure"],
                "regexAny": ["SMC.*failure"],
                "notRegex": ["watchdog"]
              },
              "notes": "Test classifier"
            }
          ],
          "bitmaskPolicies": [
            {
              "diagnosticProfile": "SMC_13_MINI",
              "enabled": true,
              "knownBits": ["0x400", "0x800", "0x1000"],
              "exactRulesAlwaysWin": true,
              "notes": "policy"
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
                "sensorTokensAny": ["PRS0"],
                "sensorCodesExactAny": ["0x1000"],
                "requiredTermsAll": ["SMC"],
                "rawTermsAny": ["BSC failure"]
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
                "cautions": ["Desconectar batería"]
              },
              "sourceIds": ["src_ifixit_test"],
              "notes": "rule note"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun fullParserPreservesRulePackMeaning() {
        val pack = RulePackJsonParser.parse(
            jsonString = validJson,
            origin = RulePackOrigin.USER_IMPORTED,
            hasher = hasher
        )

        assertEquals(1, pack.schemaVersion)
        assertEquals("1.1.0", pack.knowledgeBaseVersion)
        assertEquals("PanicLab Rule Pack Test", pack.title)
        assertEquals("2026-08-21T12:00:00Z", pack.generatedAt)
        assertEquals("es-UY", pack.locale)
        assertEquals(1, pack.sources.size)
        assertEquals("HIGH", pack.sources.single().trustLevel)
        assertEquals(listOf("SMC Panic"), pack.sources.single().supports)
        assertEquals("iPhone14,4", pack.deviceModels.single().productCode)
        assertEquals(2021, pack.deviceModels.single().releaseYear)
        assertEquals(PanicFamily.SMC_BSC_FAILURE, pack.diagnosticRules.single().panicFamilies.single())
        assertEquals(ConfidenceLevel.HIGH, pack.diagnosticRules.single().confidence)
        assertEquals(VerificationStatus.WELL_DOCUMENTED, pack.diagnosticRules.single().verificationStatus)
        assertEquals("Probar flex conocido como bueno", pack.diagnosticRules.single().repairFlow.knownGoodTest)
        assertEquals("rule note", pack.diagnosticRules.single().notes)
        assertEquals(64, pack.checksum.length)
        assertEquals(RulePackOrigin.USER_IMPORTED, pack.origin)
        assertEquals(validJson, pack.rawJson)
    }

    @Test
    fun parserUsesLegacyCompatibleDefaultsAndEnumFallbacks() {
        val sparseJson = """
            {
              "deviceModels": [
                {
                  "productCode": "iPhone14,4",
                  "marketingName": "iPhone 13 mini",
                  "family": "IPHONE_13_MINI",
                  "diagnosticProfile": "SMC_13_MINI"
                }
              ],
              "panicClassifiers": [
                { "id": "c", "family": "UNKNOWN", "match": {} }
              ],
              "bitmaskPolicies": [
                { "diagnosticProfile": "SMC_13_MINI" }
              ],
              "diagnosticRules": [
                {
                  "id": "r1",
                  "title": "Rule",
                  "match": {
                    "panicFamiliesAny": ["NOT_A_FAMILY", "SMC_BSC_FAILURE"]
                  },
                  "diagnosis": {
                    "label": "Label",
                    "suspectedComponents": [ { "name": "Component" } ]
                  },
                  "confidence": "NOT_A_CONFIDENCE",
                  "verificationStatus": "NOT_A_STATUS",
                  "repairFlow": { "knownGoodTest": null }
                }
              ]
            }
        """.trimIndent()

        val pack = RulePackJsonParser.parse(sparseJson, RulePackOrigin.BUNDLED, hasher)

        assertEquals("1.0.0", pack.knowledgeBaseVersion)
        assertEquals("PanicLab Rule Pack", pack.title)
        assertEquals("", pack.generatedAt)
        assertEquals("es-UY", pack.locale)
        assertTrue(pack.sources.isEmpty())
        assertEquals("", pack.deviceModels.single().variant)
        assertEquals(2020, pack.deviceModels.single().releaseYear)
        assertEquals(100, pack.panicClassifiers.single().priority)
        assertNull(pack.panicClassifiers.single().notes)
        assertFalse(pack.bitmaskPolicies.single().enabled)
        assertTrue(pack.bitmaskPolicies.single().exactRulesAlwaysWin)
        assertNull(pack.bitmaskPolicies.single().notes)
        val rule = pack.diagnosticRules.single()
        assertEquals(listOf(PanicFamily.SMC_BSC_FAILURE), rule.panicFamilies)
        assertEquals("GENERAL", rule.diagnosis.subsystem)
        assertEquals("PRIMARY", rule.diagnosis.suspectedComponents.single().role)
        assertEquals(ConfidenceLevel.UNKNOWN, rule.confidence)
        assertEquals(VerificationStatus.UNKNOWN, rule.verificationStatus)
        assertTrue(rule.primaryEligible)
        assertTrue(rule.exactCodeOnly)
        assertFalse(rule.allowBitmaskDecomposition)
        assertNull(rule.repairFlow.knownGoodTest)
        assertNull(rule.notes)
        assertEquals(RulePackOrigin.BUNDLED, pack.origin)
    }

    @Test
    fun validPackPassesValidator() {
        val pack = RulePackJsonParser.parse(validJson, hasher = hasher)
        val result = RulePackValidator.validate(pack)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals("1.1.0", result.knowledgeBaseVersion)
        assertEquals(1, result.rulesCount)
        assertEquals(1, result.modelsCount)
        assertEquals(1, result.classifiersCount)
        assertEquals(1, result.sourcesCount)
        assertEquals(1, result.bitmaskCount)
        assertEquals(pack.checksum, result.checksum)
    }

    @Test
    fun validatorReportsStructuralReferenceRegexCodeAndTrustProblems() {
        val source = RulePackSource(
            id = "dup",
            title = "Source",
            publisher = "Publisher",
            url = "ftp://invalid",
            checkedAt = "",
            trustLevel = "COMMUNITY"
        )
        val baseDiagnosis = DiagnosisDefinition("Label", "GEN", emptyList(), "")
        val badRules = listOf(
            DiagnosticRule(
                id = "dup-rule",
                title = "",
                diagnosis = baseDiagnosis.copy(label = ""),
                confidence = ConfidenceLevel.LOW,
                verificationStatus = VerificationStatus.CONFLICTING_SOURCE,
                sensorCodesExact = listOf("0xNOTHEX", "not-decimal"),
                sourceIds = listOf("missing")
            ),
            DiagnosticRule(
                id = "dup-rule",
                title = "Second",
                diagnosis = baseDiagnosis,
                confidence = ConfidenceLevel.HIGH,
                verificationStatus = VerificationStatus.COMMUNITY_SUPPORTED,
                sourceIds = listOf("dup")
            ),
            DiagnosticRule(
                id = "",
                title = "Third",
                diagnosis = baseDiagnosis,
                confidence = ConfidenceLevel.HIGH,
                verificationStatus = VerificationStatus.EXPERIMENTAL,
                primaryEligible = false,
                sourceIds = listOf("dup")
            )
        )
        val invalid = ParsedRulePack(
            schemaVersion = 2,
            knowledgeBaseVersion = "1.1-beta",
            title = "Invalid",
            generatedAt = "",
            locale = "es-UY",
            sources = listOf(source, source, source.copy(id = "")),
            deviceModels = listOf(
                DeviceModel("bad", "", "F", "", "P", 2020, listOf("missing")),
                DeviceModel("iPhone14,4", "One", "F", "", "P", 2020),
                DeviceModel("iPhone14,4", "Two", "F", "", "P", 2020)
            ),
            panicClassifiers = listOf(
                PanicClassifier("", "CUSTOM", regexAny = listOf("["), notRegex = listOf("(")),
                PanicClassifier("same", "UNKNOWN"),
                PanicClassifier("same", "SMC_BSC_FAILURE")
            ),
            bitmaskPolicies = emptyList(),
            diagnosticRules = badRules,
            checksum = "checksum"
        )

        val result = RulePackValidator.validate(invalid)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path == "schemaVersion" })
        assertTrue(result.errors.any { it.path == "knowledgeBaseVersion" })
        assertTrue(result.errors.any { it.message.contains("ID de fuente duplicado") })
        assertTrue(result.errors.any { it.message.contains("productCode duplicado") })
        assertTrue(result.errors.any { it.message.contains("ID de clasificador duplicado") })
        assertTrue(result.errors.any { it.message.contains("Expresión regular inválida") })
        assertTrue(result.errors.any { it.message.contains("ID de regla duplicado") })
        assertTrue(result.errors.any { it.message.contains("Código numérico/hexadecimal inválido") })
        assertTrue(result.errors.any { it.message.contains("sourceId inexistente") })
        assertTrue(result.warnings.any { it.message.contains("URL de fuente") })
        assertTrue(result.warnings.any { it.message.contains("referencia fuente inexistente") })
        assertTrue(result.warnings.any { it.message.contains("Familia no estándar") })
        assertTrue(result.warnings.any { it.message.contains("CONFLICTING_SOURCE") })
        assertTrue(result.warnings.any { it.message.contains("COMMUNITY_SUPPORTED") })
        assertTrue(result.warnings.any { it.message.contains("EXPERIMENTAL") })
        assertTrue(result.warnings.any { it.message.contains("LOW confidence") })
    }

    @Test
    fun diffCoversNewModifiedDeactivatedRemovedModelsSourcesAndClassifiers() {
        val currentRule = rule(
            id = "r1",
            label = "Old label",
            active = true,
            confidence = ConfidenceLevel.HIGH,
            status = VerificationStatus.WELL_DOCUMENTED,
            priority = 100,
            primary = true
        )
        val removedRule = rule(id = "removed", label = "Removed")
        val current = pack(
            version = "1.0.0",
            rules = listOf(currentRule, removedRule),
            devices = listOf(DeviceModel("iPhone14,4", "Old name", "F", "", "OLD", 2021)),
            sources = listOf(source("s1")),
            classifiers = listOf(PanicClassifier("c1", "SMC_BSC_FAILURE"))
        )
        val changedRule = currentRule.copy(
            active = false,
            confidence = ConfidenceLevel.MEDIUM,
            verificationStatus = VerificationStatus.VERIFIED,
            priority = 90,
            primaryEligible = false,
            diagnosis = currentRule.diagnosis.copy(label = "New label")
        )
        val incoming = pack(
            version = "2.0.0",
            rules = listOf(changedRule, rule(id = "added", label = "Added")),
            devices = listOf(
                DeviceModel("iPhone14,4", "New name", "F", "", "NEW", 2021),
                DeviceModel("iPhone15,2", "Added model", "F", "", "P", 2022)
            ),
            sources = listOf(source("s1"), source("s2")),
            classifiers = listOf(
                PanicClassifier("c1", "SMC_BSC_FAILURE"),
                PanicClassifier("c2", "I2C")
            )
        )

        val diff = RulePackDiffCalculator.calculateDiff(current, incoming)

        assertEquals("1.0.0", diff.currentVersion)
        assertEquals("2.0.0", diff.incomingVersion)
        assertEquals(1, diff.addedRulesCount)
        assertEquals(1, diff.modifiedRulesCount)
        assertEquals(1, diff.deactivatedRulesCount)
        assertEquals(1, diff.removedRulesCount)
        assertEquals(1, diff.addedModelsCount)
        assertEquals(1, diff.modifiedModelsCount)
        assertEquals(1, diff.addedSourcesCount)
        assertEquals(1, diff.addedClassifiersCount)
        assertEquals(RuleDiffItem.ChangeType.DEACTIVATED, diff.ruleDiffs.first { it.ruleId == "r1" }.changeType)
        assertNotNull(diff.ruleDiffs.first { it.ruleId == "r1" }.confidenceChange)
        assertNotNull(diff.ruleDiffs.first { it.ruleId == "r1" }.statusChange)
        assertEquals(RuleDiffItem.ChangeType.ADDED, diff.ruleDiffs.first { it.ruleId == "added" }.changeType)
        assertEquals(RuleDiffItem.ChangeType.REMOVED, diff.ruleDiffs.first { it.ruleId == "removed" }.changeType)
    }

    @Test
    fun diffAgainstNoCurrentPackTreatsEverythingAsAdded() {
        val incoming = pack(
            version = "1.0.0",
            rules = listOf(rule("a", "A")),
            devices = listOf(DeviceModel("iPhone14,4", "Phone", "F", "", "P", 2021)),
            sources = listOf(source("s")),
            classifiers = listOf(PanicClassifier("c", "SMC_BSC_FAILURE"))
        )

        val diff = RulePackDiffCalculator.calculateDiff(null, incoming)

        assertEquals("Ninguna", diff.currentVersion)
        assertEquals(1, diff.addedRulesCount)
        assertEquals(1, diff.addedModelsCount)
        assertEquals(1, diff.addedSourcesCount)
        assertEquals(1, diff.addedClassifiersCount)
        assertEquals(RuleDiffItem.ChangeType.ADDED, diff.ruleDiffs.single().changeType)
    }

    @Test
    fun unchangedRuleProducesNoRuleDiff() {
        val sameRule = rule("same", "Same")
        val current = pack("1.0.0", listOf(sameRule))
        val incoming = pack("1.0.1", listOf(sameRule))

        val diff = RulePackDiffCalculator.calculateDiff(current, incoming)

        assertEquals(0, diff.modifiedRulesCount)
        assertTrue(diff.ruleDiffs.isEmpty())
    }

    private fun rule(
        id: String,
        label: String,
        active: Boolean = true,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        status: VerificationStatus = VerificationStatus.VERIFIED,
        priority: Int = 100,
        primary: Boolean = true
    ) = DiagnosticRule(
        id = id,
        title = id,
        active = active,
        priority = priority,
        deviceScope = DeviceScope(),
        diagnosis = DiagnosisDefinition(label, "TEST", listOf(SuspectedComponent("C", "PRIMARY")), "test"),
        confidence = confidence,
        verificationStatus = status,
        primaryEligible = primary,
        repairFlow = RepairFlow()
    )

    private fun source(id: String) = RulePackSource(
        id = id,
        title = id,
        publisher = "P",
        url = "https://example.com/$id",
        checkedAt = "2026-09-18",
        trustLevel = "HIGH"
    )

    private fun pack(
        version: String,
        rules: List<DiagnosticRule>,
        devices: List<DeviceModel> = emptyList(),
        sources: List<RulePackSource> = emptyList(),
        classifiers: List<PanicClassifier> = emptyList()
    ) = ParsedRulePack(
        schemaVersion = 1,
        knowledgeBaseVersion = version,
        title = "Pack $version",
        generatedAt = "",
        locale = "es-UY",
        sources = sources,
        deviceModels = devices,
        panicClassifiers = classifiers,
        bitmaskPolicies = listOf(BitmaskPolicy("P", true)),
        diagnosticRules = rules,
        checksum = "checksum"
    )
}
