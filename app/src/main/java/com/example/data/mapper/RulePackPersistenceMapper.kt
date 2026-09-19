package com.example.data.mapper

import com.example.data.local.entity.DeviceModelEntity
import com.example.data.local.entity.DiagnosticRuleEntity
import com.example.data.local.entity.RulePackEntity
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceModel
import com.example.domain.model.DeviceScope
import com.example.domain.model.DiagnosisDefinition
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicFamily
import com.example.domain.model.RepairFlow
import com.example.domain.model.RulePackMetadata
import com.example.domain.model.RulePackOrigin
import com.example.domain.model.SuspectedComponent
import com.example.domain.model.VerificationStatus
import org.json.JSONArray

object RulePackPersistenceMapper {
    fun toMetadata(entity: RulePackEntity): RulePackMetadata = RulePackMetadata(
        version = entity.version,
        title = entity.title,
        generatedAt = entity.generatedAt,
        schemaVersion = entity.schemaVersion,
        rulesCount = entity.rulesCount,
        modelsCount = entity.modelsCount,
        classifiersCount = entity.classifiersCount,
        sourcesCount = entity.sourcesCount,
        bitmaskCount = entity.bitmaskCount,
        origin = runCatching { RulePackOrigin.valueOf(entity.origin) }
            .getOrDefault(RulePackOrigin.USER_IMPORTED),
        sourceFilename = entity.sourceFilename,
        checksum = entity.checksum,
        isActive = entity.isActive,
        isDefault = entity.isDefault,
        previousVersion = entity.previousVersion,
        importedAt = entity.importedAt
    )

    fun toDomainRule(entity: DiagnosticRuleEntity): DiagnosticRule {
        val confidence = runCatching { ConfidenceLevel.valueOf(entity.confidence) }
            .getOrDefault(ConfidenceLevel.UNKNOWN)
        val verificationStatus = runCatching { VerificationStatus.valueOf(entity.verificationStatus) }
            .getOrDefault(VerificationStatus.UNKNOWN)

        return DiagnosticRule(
            id = entity.id,
            title = entity.title,
            active = entity.active,
            priority = entity.priority,
            deviceScope = DeviceScope(
                diagnosticProfiles = parseStringArray(entity.diagnosticProfilesJson),
                productCodes = parseStringArray(entity.productCodesJson)
            ),
            panicFamilies = parseStringArray(entity.panicFamiliesJson).mapNotNull { raw ->
                runCatching { PanicFamily.valueOf(raw) }.getOrNull()
            },
            sensorTokens = parseStringArray(entity.sensorTokensJson),
            sensorCodesExact = parseStringArray(entity.sensorCodesExactJson),
            requiredTermsAll = parseStringArray(entity.requiredTermsJson),
            rawTermsAny = parseStringArray(entity.rawTermsJson),
            diagnosis = DiagnosisDefinition(
                label = entity.label,
                subsystem = entity.subsystem,
                suspectedComponents = parseSuspectedComponents(entity.suspectedComponentsJson),
                interpretation = entity.interpretation
            ),
            confidence = confidence,
            verificationStatus = verificationStatus,
            primaryEligible = entity.primaryEligible,
            exactCodeOnly = entity.exactCodeOnly,
            allowBitmaskDecomposition = entity.allowBitmaskDecomposition,
            repairFlow = RepairFlow(
                firstChecks = parseStringArray(entity.firstChecksJson),
                knownGoodTest = entity.knownGoodTest,
                boardLevelNextSteps = parseStringArray(entity.boardLevelNextStepsJson),
                cautions = parseStringArray(entity.cautionsJson)
            ),
            sourceIds = parseStringArray(entity.sourceIdsJson),
            notes = entity.notes,
            version = entity.version
        )
    }

    fun toDomainDevice(entity: DeviceModelEntity): DeviceModel = DeviceModel(
        productCode = entity.productCode,
        marketingName = entity.marketingName,
        family = entity.family,
        variant = entity.variant,
        diagnosticProfile = entity.diagnosticProfile,
        releaseYear = entity.releaseYear,
        sourceIds = parseStringArray(entity.sourceIdsJson)
    )

    private fun parseStringArray(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(jsonString)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun parseSuspectedComponents(jsonString: String?): List<SuspectedComponent> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(jsonString)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                SuspectedComponent(
                    name = obj.getString("name"),
                    role = obj.optString("role", "PRIMARY")
                )
            }
        }.getOrDefault(emptyList())
    }
}
