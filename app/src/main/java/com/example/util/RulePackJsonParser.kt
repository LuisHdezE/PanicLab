package com.example.util

import com.example.data.local.entity.DeviceModelEntity
import com.example.data.local.entity.DiagnosticRuleEntity
import com.example.data.local.entity.RulePackEntity
import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

data class ParsedRulePack(
    val rulePackEntity: RulePackEntity,
    val deviceModelEntities: List<DeviceModelEntity>,
    val diagnosticRuleEntities: List<DiagnosticRuleEntity>,
    val domainRules: List<DiagnosticRule>,
    val domainDevices: List<DeviceModel>
)

object RulePackJsonParser {

    fun parse(jsonString: String, isDefault: Boolean = false): ParsedRulePack {
        val root = JSONObject(jsonString)
        val schemaVersion = root.optInt("schemaVersion", 1)
        val kbVersion = root.optString("knowledgeBaseVersion", "1.0.0")
        val title = root.optString("title", "PanicLab Rule Pack")
        val generatedAt = root.optString("generatedAt", "")

        val deviceModelsArray = root.optJSONArray("deviceModels") ?: JSONArray()
        val diagnosticRulesArray = root.optJSONArray("diagnosticRules") ?: JSONArray()

        val deviceEntities = mutableListOf<DeviceModelEntity>()
        val domainDevices = mutableListOf<DeviceModel>()

        for (i in 0 until deviceModelsArray.length()) {
            val devObj = deviceModelsArray.getJSONObject(i)
            val productCode = devObj.getString("productCode")
            val marketingName = devObj.getString("marketingName")
            val family = devObj.getString("family")
            val variant = devObj.optString("variant", "")
            val diagnosticProfile = devObj.getString("diagnosticProfile")
            val releaseYear = devObj.optInt("releaseYear", 2020)
            val sourceIdsArr = devObj.optJSONArray("sourceIds") ?: JSONArray()
            val sourceIds = (0 until sourceIdsArr.length()).map { sourceIdsArr.getString(it) }

            deviceEntities.add(
                DeviceModelEntity(
                    productCode = productCode,
                    marketingName = marketingName,
                    family = family,
                    variant = variant,
                    diagnosticProfile = diagnosticProfile,
                    releaseYear = releaseYear,
                    sourceIdsJson = sourceIdsArr.toString(),
                    notes = null
                )
            )

            domainDevices.add(
                DeviceModel(
                    productCode = productCode,
                    marketingName = marketingName,
                    family = family,
                    variant = variant,
                    diagnosticProfile = diagnosticProfile,
                    releaseYear = releaseYear,
                    sourceIds = sourceIds
                )
            )
        }

        val ruleEntities = mutableListOf<DiagnosticRuleEntity>()
        val domainRules = mutableListOf<DiagnosticRule>()

        for (i in 0 until diagnosticRulesArray.length()) {
            val ruleObj = diagnosticRulesArray.getJSONObject(i)
            val id = ruleObj.getString("id")
            val ruleTitle = ruleObj.getString("title")
            val active = ruleObj.optBoolean("active", true)
            val priority = ruleObj.optInt("priority", 100)

            val deviceScopeObj = ruleObj.optJSONObject("deviceScope") ?: JSONObject()
            val diagProfilesArr = deviceScopeObj.optJSONArray("diagnosticProfiles") ?: JSONArray()
            val productCodesArr = deviceScopeObj.optJSONArray("productCodes") ?: JSONArray()
            val diagnosticProfiles = (0 until diagProfilesArr.length()).map { diagProfilesArr.getString(it) }
            val productCodes = (0 until productCodesArr.length()).map { productCodesArr.getString(it) }

            val matchObj = ruleObj.optJSONObject("match") ?: JSONObject()
            val panicFamiliesArr = matchObj.optJSONArray("panicFamiliesAny") ?: JSONArray()
            val panicFamilies = (0 until panicFamiliesArr.length()).mapNotNull {
                val famStr = panicFamiliesArr.getString(it)
                try { PanicFamily.valueOf(famStr) } catch (e: Exception) { null }
            }

            val sensorTokensArr = matchObj.optJSONArray("sensorTokensAny") ?: JSONArray()
            val sensorTokens = (0 until sensorTokensArr.length()).map { sensorTokensArr.getString(it) }

            val sensorCodesArr = matchObj.optJSONArray("sensorCodesExactAny") ?: JSONArray()
            val sensorCodes = (0 until sensorCodesArr.length()).map { sensorCodesArr.getString(it) }

            val reqTermsArr = matchObj.optJSONArray("requiredTermsAll") ?: JSONArray()
            val requiredTerms = (0 until reqTermsArr.length()).map { reqTermsArr.getString(it) }

            val rawTermsArr = matchObj.optJSONArray("rawTermsAny") ?: JSONArray()
            val rawTerms = (0 until rawTermsArr.length()).map { rawTermsArr.getString(it) }

            val diagObj = ruleObj.getJSONObject("diagnosis")
            val label = diagObj.getString("label")
            val subsystem = diagObj.optString("subsystem", "GENERAL")
            val interpretation = diagObj.optString("interpretation", "")
            val suspectedCompsArr = diagObj.optJSONArray("suspectedComponents") ?: JSONArray()
            val suspectedComps = (0 until suspectedCompsArr.length()).map {
                val scObj = suspectedCompsArr.getJSONObject(it)
                SuspectedComponent(
                    name = scObj.getString("name"),
                    role = scObj.optString("role", "PRIMARY")
                )
            }

            val confidenceStr = ruleObj.optString("confidence", "UNKNOWN")
            val confidence = try {
                ConfidenceLevel.valueOf(confidenceStr)
            } catch (e: Exception) {
                ConfidenceLevel.UNKNOWN
            }

            val verificationStr = ruleObj.optString("verificationStatus", "UNKNOWN")
            val verificationStatus = try {
                VerificationStatus.valueOf(verificationStr)
            } catch (e: Exception) {
                VerificationStatus.UNKNOWN
            }

            val primaryEligible = ruleObj.optBoolean("primaryEligible", true)
            val exactCodeOnly = ruleObj.optBoolean("exactCodeOnly", true)
            val allowBitmaskDecomposition = ruleObj.optBoolean("allowBitmaskDecomposition", false)

            val repairFlowObj = ruleObj.optJSONObject("repairFlow") ?: JSONObject()
            val firstChecksArr = repairFlowObj.optJSONArray("firstChecks") ?: JSONArray()
            val firstChecks = (0 until firstChecksArr.length()).map { firstChecksArr.getString(it) }
            val knownGoodTest = if (repairFlowObj.isNull("knownGoodTest")) null else repairFlowObj.optString("knownGoodTest")
            val boardStepsArr = repairFlowObj.optJSONArray("boardLevelNextSteps") ?: JSONArray()
            val boardSteps = (0 until boardStepsArr.length()).map { boardStepsArr.getString(it) }
            val cautionsArr = repairFlowObj.optJSONArray("cautions") ?: JSONArray()
            val cautions = (0 until cautionsArr.length()).map { cautionsArr.getString(it) }

            val repairFlow = RepairFlow(
                firstChecks = firstChecks,
                knownGoodTest = knownGoodTest,
                boardLevelNextSteps = boardSteps,
                cautions = cautions
            )

            val sourceIdsArr = ruleObj.optJSONArray("sourceIds") ?: JSONArray()
            val sourceIds = (0 until sourceIdsArr.length()).map { sourceIdsArr.getString(it) }
            val notes = if (ruleObj.isNull("notes")) null else ruleObj.optString("notes")

            ruleEntities.add(
                DiagnosticRuleEntity(
                    id = id,
                    title = ruleTitle,
                    active = active,
                    priority = priority,
                    diagnosticProfilesJson = diagProfilesArr.toString(),
                    productCodesJson = productCodesArr.toString(),
                    panicFamiliesJson = panicFamiliesArr.toString(),
                    sensorTokensJson = sensorTokensArr.toString(),
                    sensorCodesExactJson = sensorCodesArr.toString(),
                    requiredTermsJson = reqTermsArr.toString(),
                    rawTermsJson = rawTermsArr.toString(),
                    label = label,
                    subsystem = subsystem,
                    suspectedComponentsJson = suspectedCompsArr.toString(),
                    interpretation = interpretation,
                    confidence = confidence.name,
                    verificationStatus = verificationStatus.name,
                    primaryEligible = primaryEligible,
                    exactCodeOnly = exactCodeOnly,
                    allowBitmaskDecomposition = allowBitmaskDecomposition,
                    firstChecksJson = firstChecksArr.toString(),
                    knownGoodTest = knownGoodTest,
                    boardLevelNextStepsJson = boardStepsArr.toString(),
                    cautionsJson = cautionsArr.toString(),
                    sourceIdsJson = sourceIdsArr.toString(),
                    notes = notes,
                    version = kbVersion
                )
            )

            domainRules.add(
                DiagnosticRule(
                    id = id,
                    title = ruleTitle,
                    active = active,
                    priority = priority,
                    deviceScope = DeviceScope(
                        diagnosticProfiles = diagnosticProfiles,
                        productCodes = productCodes
                    ),
                    panicFamilies = panicFamilies,
                    sensorTokens = sensorTokens,
                    sensorCodesExact = sensorCodes,
                    requiredTermsAll = requiredTerms,
                    rawTermsAny = rawTerms,
                    diagnosis = DiagnosisDefinition(
                        label = label,
                        subsystem = subsystem,
                        suspectedComponents = suspectedComps,
                        interpretation = interpretation
                    ),
                    confidence = confidence,
                    verificationStatus = verificationStatus,
                    primaryEligible = primaryEligible,
                    exactCodeOnly = exactCodeOnly,
                    allowBitmaskDecomposition = allowBitmaskDecomposition,
                    repairFlow = repairFlow,
                    sourceIds = sourceIds,
                    notes = notes,
                    version = kbVersion
                )
            )
        }

        val rulePackEntity = RulePackEntity(
            version = kbVersion,
            title = title,
            generatedAt = generatedAt,
            schemaVersion = schemaVersion,
            rulesCount = ruleEntities.size,
            modelsCount = deviceEntities.size,
            isDefault = isDefault
        )

        return ParsedRulePack(
            rulePackEntity = rulePackEntity,
            deviceModelEntities = deviceEntities,
            diagnosticRuleEntities = ruleEntities,
            domainRules = domainRules,
            domainDevices = domainDevices
        )
    }

    fun entityToDomain(entity: DiagnosticRuleEntity): DiagnosticRule {
        val diagProfiles = parseJsonStringArray(entity.diagnosticProfilesJson)
        val productCodes = parseJsonStringArray(entity.productCodesJson)
        val panicFamilies = parseJsonStringArray(entity.panicFamiliesJson).mapNotNull {
            try { PanicFamily.valueOf(it) } catch (e: Exception) { null }
        }
        val sensorTokens = parseJsonStringArray(entity.sensorTokensJson)
        val sensorCodes = parseJsonStringArray(entity.sensorCodesExactJson)
        val reqTerms = parseJsonStringArray(entity.requiredTermsJson)
        val rawTerms = parseJsonStringArray(entity.rawTermsJson)

        val suspectedComponents = parseSuspectedComponents(entity.suspectedComponentsJson)
        val firstChecks = parseJsonStringArray(entity.firstChecksJson)
        val boardSteps = parseJsonStringArray(entity.boardLevelNextStepsJson)
        val cautions = parseJsonStringArray(entity.cautionsJson)
        val sourceIds = parseJsonStringArray(entity.sourceIdsJson)

        val confidence = try { ConfidenceLevel.valueOf(entity.confidence) } catch (e: Exception) { ConfidenceLevel.UNKNOWN }
        val verificationStatus = try { VerificationStatus.valueOf(entity.verificationStatus) } catch (e: Exception) { VerificationStatus.UNKNOWN }

        return DiagnosticRule(
            id = entity.id,
            title = entity.title,
            active = entity.active,
            priority = entity.priority,
            deviceScope = DeviceScope(diagnosticProfiles = diagProfiles, productCodes = productCodes),
            panicFamilies = panicFamilies,
            sensorTokens = sensorTokens,
            sensorCodesExact = sensorCodes,
            requiredTermsAll = reqTerms,
            rawTermsAny = rawTerms,
            diagnosis = DiagnosisDefinition(
                label = entity.label,
                subsystem = entity.subsystem,
                suspectedComponents = suspectedComponents,
                interpretation = entity.interpretation
            ),
            confidence = confidence,
            verificationStatus = verificationStatus,
            primaryEligible = entity.primaryEligible,
            exactCodeOnly = entity.exactCodeOnly,
            allowBitmaskDecomposition = entity.allowBitmaskDecomposition,
            repairFlow = RepairFlow(
                firstChecks = firstChecks,
                knownGoodTest = entity.knownGoodTest,
                boardLevelNextSteps = boardSteps,
                cautions = cautions
            ),
            sourceIds = sourceIds,
            notes = entity.notes,
            version = entity.version
        )
    }

    fun entityToDevice(entity: DeviceModelEntity): DeviceModel {
        val sourceIds = parseJsonStringArray(entity.sourceIdsJson)
        return DeviceModel(
            productCode = entity.productCode,
            marketingName = entity.marketingName,
            family = entity.family,
            variant = entity.variant,
            diagnosticProfile = entity.diagnosticProfile,
            releaseYear = entity.releaseYear,
            sourceIds = sourceIds
        )
    }

    fun parseJsonStringArray(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonString)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSuspectedComponents(jsonString: String?): List<SuspectedComponent> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonString)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                SuspectedComponent(
                    name = obj.getString("name"),
                    role = obj.optString("role", "PRIMARY")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
