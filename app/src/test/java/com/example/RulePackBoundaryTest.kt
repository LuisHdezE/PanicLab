package com.example

import com.example.data.local.entity.DeviceModelEntity
import com.example.data.local.entity.DiagnosticRuleEntity
import com.example.data.local.entity.RulePackEntity
import com.example.data.mapper.RulePackPersistenceMapper
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.RulePackOrigin
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RulePackBoundaryTest {
    @Test
    fun rulePackEntityMapsToDomainMetadataWithoutLeakingRawJson() {
        val entity = RulePackEntity(
            version = "2.0.0",
            title = "Pack",
            generatedAt = "2026-09-18T00:00:00Z",
            schemaVersion = 1,
            rulesCount = 3,
            modelsCount = 2,
            classifiersCount = 1,
            sourcesCount = 4,
            bitmaskCount = 1,
            origin = "BUNDLED",
            sourceFilename = "pack.json",
            checksum = "abc",
            isActive = true,
            isDefault = true,
            previousVersion = "1.0.0",
            rawJson = "{secret-persistence-payload}",
            importedAt = 1234L
        )

        val metadata = RulePackPersistenceMapper.toMetadata(entity)

        assertEquals("2.0.0", metadata.version)
        assertEquals(RulePackOrigin.BUNDLED, metadata.origin)
        assertEquals("abc", metadata.checksum)
        assertTrue(metadata.isActive)
        assertEquals(1234L, metadata.importedAt)
    }

    @Test
    fun unknownPersistenceOriginFallsBackToUserImported() {
        val entity = RulePackEntity(
            version = "2.0.0",
            title = "Pack",
            generatedAt = "",
            schemaVersion = 1,
            rulesCount = 0,
            modelsCount = 0,
            origin = "FUTURE_ORIGIN"
        )

        assertEquals(
            RulePackOrigin.USER_IMPORTED,
            RulePackPersistenceMapper.toMetadata(entity).origin
        )
    }

    @Test
    fun ruleAndDeviceEntitiesMapToDomainModels() {
        val ruleEntity = DiagnosticRuleEntity(
            id = "r1",
            title = "Rule",
            active = true,
            priority = 90,
            diagnosticProfilesJson = "[\"SMC_13_MINI\"]",
            productCodesJson = "[\"iPhone14,4\"]",
            panicFamiliesJson = "[\"SMC_BSC_FAILURE\"]",
            sensorTokensJson = "[\"PRS0\"]",
            sensorCodesExactJson = "[\"0x1000\"]",
            requiredTermsJson = "[\"SMC\"]",
            rawTermsJson = "[\"BSC failure\"]",
            label = "Front sensor",
            subsystem = "SMC_SENSOR",
            suspectedComponentsJson = "[{\"name\":\"Front Sensor\",\"role\":\"PRIMARY\"}]",
            interpretation = "Interpretation",
            confidence = "HIGH",
            verificationStatus = "VERIFIED",
            primaryEligible = true,
            exactCodeOnly = true,
            allowBitmaskDecomposition = false,
            firstChecksJson = "[\"Inspect\"]",
            knownGoodTest = "Known good",
            boardLevelNextStepsJson = "[\"Diode mode\"]",
            cautionsJson = "[\"Disconnect battery\"]",
            sourceIdsJson = "[\"source\"]",
            notes = "notes",
            version = "2.0.0"
        )
        val deviceEntity = DeviceModelEntity(
            productCode = "iPhone14,4",
            marketingName = "iPhone 13 mini",
            family = "IPHONE_13_MINI",
            variant = "13_MINI",
            diagnosticProfile = "SMC_13_MINI",
            releaseYear = 2021,
            sourceIdsJson = "[\"source\"]"
        )

        val rule = RulePackPersistenceMapper.toDomainRule(ruleEntity)
        val device = RulePackPersistenceMapper.toDomainDevice(deviceEntity)

        assertEquals(ConfidenceLevel.HIGH, rule.confidence)
        assertEquals(VerificationStatus.VERIFIED, rule.verificationStatus)
        assertEquals(listOf("SMC_13_MINI"), rule.deviceScope.diagnosticProfiles)
        assertEquals("Front Sensor", rule.diagnosis.suspectedComponents.single().name)
        assertEquals("Known good", rule.repairFlow.knownGoodTest)
        assertEquals(listOf("source"), rule.sourceIds)
        assertEquals("iPhone14,4", device.productCode)
        assertEquals(listOf("source"), device.sourceIds)
    }

    @Test
    fun malformedPersistenceFieldsFailSafeToDomainDefaults() {
        val entity = DiagnosticRuleEntity(
            id = "r1",
            title = "Rule",
            diagnosticProfilesJson = "not-json",
            productCodesJson = "",
            panicFamiliesJson = "[\"NOT_A_FAMILY\"]",
            label = "Label",
            subsystem = "GENERAL",
            suspectedComponentsJson = "broken",
            interpretation = "",
            confidence = "FUTURE_CONFIDENCE",
            verificationStatus = "FUTURE_STATUS"
        )

        val rule = RulePackPersistenceMapper.toDomainRule(entity)

        assertTrue(rule.deviceScope.diagnosticProfiles.isEmpty())
        assertTrue(rule.panicFamilies.isEmpty())
        assertTrue(rule.diagnosis.suspectedComponents.isEmpty())
        assertEquals(ConfidenceLevel.UNKNOWN, rule.confidence)
        assertEquals(VerificationStatus.UNKNOWN, rule.verificationStatus)
    }
}
