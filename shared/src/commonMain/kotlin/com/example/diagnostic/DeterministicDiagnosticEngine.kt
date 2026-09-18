package com.example.diagnostic

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DeviceModel
import com.example.domain.model.DiagnosisCandidate
import com.example.domain.model.DiagnosticEvidence
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.DiagnosticRule
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.domain.model.RepairFlow
import com.example.domain.model.SensorCode
import com.example.domain.model.VerificationStatus
import com.example.platform.Clock
import com.example.platform.IdGenerator
import com.example.util.HexUtils

data class ExtractedSensors(
    val missingSensorTokens: List<String> = emptyList(),
    val smcSensorCodes: List<String> = emptyList(),
    val sensorCodes: List<SensorCode> = emptyList(),
    val rawSensorArrayLines: List<String> = emptyList()
)

object SensorExtractor {
    private val sensorArrayPayloadRegex = Regex(
        """(?:S\.)?sensor[ _]array(?:\s+\d+\s*-\s*\d+)?\s*(?:is|:|=)\s*([^\n\r"]+)""",
        RegexOption.IGNORE_CASE
    )
    private val missingSensorRegex = Regex(
        """Missing sensor\(s\)\s*:\s*([A-Za-z0-9_,\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val valueTokenRegex = Regex("""(0x[0-9a-fA-F]+|\b\d+\b)""")
    private val standaloneSmcCodeRegex = Regex("""\b(0x[0-9a-fA-F]{2,8})\b""")

    fun extract(logText: String, panicString: String? = null): ExtractedSensors {
        val missingSensors = mutableListOf<String>()
        val smcCodes = mutableListOf<String>()
        val sensorCodes = mutableListOf<SensorCode>()
        val rawArrayLines = mutableListOf<String>()

        val combinedText = buildString {
            append(logText.replace("\\n", "\n").replace("\\r", "\r"))
            if (!panicString.isNullOrBlank()) {
                append("\n").append(panicString.replace("\\n", "\n").replace("\\r", "\r"))
            }
        }

        for (line in combinedText.lines()) {
            val trimmedLine = line.trim()

            if (trimmedLine.contains("Missing sensor", ignoreCase = true)) {
                val sensorsPart = missingSensorRegex.find(trimmedLine)?.groupValues?.getOrNull(1)
                if (sensorsPart != null) {
                    missingSensors += sensorsPart.split(",", " ", "\t")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.length in 2..10 }
                }
            }

            if (trimmedLine.contains("sensor array", ignoreCase = true) ||
                trimmedLine.contains("sensor_array", ignoreCase = true)
            ) {
                rawArrayLines += trimmedLine

                val payload = sensorArrayPayloadRegex.find(trimmedLine)?.groupValues?.getOrNull(1)
                    ?: run {
                        val isIndex = trimmedLine.indexOf(" is ", ignoreCase = true)
                        val colonIndex = trimmedLine.indexOf(":")
                        val equalsIndex = trimmedLine.indexOf("=")
                        val splitIndex = when {
                            isIndex >= 0 -> isIndex + 4
                            colonIndex >= 0 -> colonIndex + 1
                            equalsIndex >= 0 -> equalsIndex + 1
                            else -> 0
                        }
                        trimmedLine.substring(splitIndex)
                    }

                valueTokenRegex.findAll(payload).forEach { match ->
                    val rawToken = match.groupValues[1]
                    val sensorCode = SensorCode.parse(rawToken)
                    if (sensorCode != null && sensorCode.numericValue > 0L) {
                        smcCodes += sensorCode.rawValue
                        sensorCodes += sensorCode
                    }
                }
            }
        }

        if (smcCodes.isEmpty() && combinedText.contains("SMC", ignoreCase = true)) {
            standaloneSmcCodeRegex.findAll(combinedText).forEach { match ->
                val sensorCode = SensorCode.parse(match.groupValues[1])
                if (sensorCode != null && sensorCode.numericValue > 0L) {
                    smcCodes += sensorCode.rawValue
                    sensorCodes += sensorCode
                }
            }
        }

        return ExtractedSensors(
            missingSensorTokens = missingSensors.distinct(),
            smcSensorCodes = smcCodes.distinct(),
            sensorCodes = sensorCodes.distinctBy { it.numericValue },
            rawSensorArrayLines = rawArrayLines.distinct()
        )
    }
}

data class RuleMatchResult(
    val matchedRules: List<DiagnosticRule>,
    val appliedRuleIds: List<String>,
    val primaryRule: DiagnosticRule?,
    val alternativeRules: List<DiagnosticRule>
)

object DiagnosticRulesEngine {
    fun evaluate(
        deviceModel: DeviceModel?,
        productCode: String?,
        panicFamilies: List<PanicFamily>,
        extractedSensors: ExtractedSensors,
        allRules: List<DiagnosticRule>
    ): RuleMatchResult {
        val activeRules = allRules.filter { it.active }
        val matchedRules = mutableListOf<DiagnosticRule>()
        val resolvedProfile = deviceModel?.diagnosticProfile
        val resolvedProduct = productCode ?: deviceModel?.productCode

        if (extractedSensors.missingSensorTokens.isNotEmpty() ||
            panicFamilies.contains(PanicFamily.THERMAL_MISSING_SENSOR)
        ) {
            for (token in extractedSensors.missingSensorTokens) {
                matchedRules += activeRules.filter { rule ->
                    val familyMatch = rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies }
                    val tokenMatch = rule.sensorTokens.any { it.equals(token, ignoreCase = true) }
                    familyMatch && tokenMatch && isScopeMatch(rule, resolvedProduct, resolvedProfile)
                }
            }
        }

        if (extractedSensors.smcSensorCodes.isNotEmpty() ||
            extractedSensors.sensorCodes.isNotEmpty() ||
            panicFamilies.contains(PanicFamily.SMC_BSC_FAILURE) ||
            panicFamilies.contains(PanicFamily.SMC_ASSERTION)
        ) {
            val codesToEvaluate = if (extractedSensors.sensorCodes.isNotEmpty()) {
                extractedSensors.sensorCodes
            } else {
                extractedSensors.smcSensorCodes.mapNotNull { SensorCode.parse(it) }
            }

            for (sensorCode in codesToEvaluate) {
                val exactMatches = activeRules.filter { rule ->
                    val familyMatch = rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies }
                    val codeMatch = rule.sensorCodesExact.any { ruleCode ->
                        val numericRuleCode = HexUtils.parseCodeToLong(ruleCode)
                        if (numericRuleCode != null) {
                            numericRuleCode == sensorCode.numericValue
                        } else {
                            ruleCode.equals(sensorCode.rawValue, ignoreCase = true) ||
                                ruleCode.equals(sensorCode.hexadecimal, ignoreCase = true) ||
                                ruleCode.equals(sensorCode.decimal, ignoreCase = true)
                        }
                    }
                    familyMatch && codeMatch && isScopeMatch(rule, resolvedProduct, resolvedProfile)
                }

                if (exactMatches.isNotEmpty()) {
                    matchedRules += exactMatches
                } else {
                    matchedRules += tryBitmaskDecomposition(
                        codeLong = sensorCode.numericValue,
                        profile = resolvedProfile,
                        product = resolvedProduct,
                        panicFamilies = panicFamilies,
                        activeRules = activeRules
                    )
                }
            }
        }

        for (family in panicFamilies) {
            matchedRules += activeRules.filter { rule ->
                rule.panicFamilies.contains(family) &&
                    rule.sensorTokens.isEmpty() &&
                    rule.sensorCodesExact.isEmpty() &&
                    isScopeMatch(rule, resolvedProduct, resolvedProfile)
            }
        }

        val distinctMatchedRules = matchedRules.distinctBy { it.id }
        val hasSmcContext = panicFamilies.contains(PanicFamily.SMC_BSC_FAILURE) ||
            panicFamilies.contains(PanicFamily.SMC_ASSERTION) ||
            extractedSensors.smcSensorCodes.isNotEmpty()
        val hasPrimaryMatch = distinctMatchedRules.any {
            it.primaryEligible && it.confidence != ConfidenceLevel.UNKNOWN && it.confidence != ConfidenceLevel.LOW
        }

        val finalMatchedList = distinctMatchedRules.toMutableList()
        if (hasSmcContext && !hasPrimaryMatch) {
            val fallbackRule = activeRules.find { it.id == "smc_unknown_code_fallback" }
            if (fallbackRule != null && !finalMatchedList.contains(fallbackRule)) {
                finalMatchedList += fallbackRule
            }
        }

        val rankedRules = rankRules(finalMatchedList, resolvedProduct, resolvedProfile)
        val primaryRule = rankedRules.firstOrNull { it.primaryEligible } ?: rankedRules.firstOrNull()
        val alternatives = rankedRules.filter { it.id != primaryRule?.id }

        return RuleMatchResult(
            matchedRules = rankedRules,
            appliedRuleIds = rankedRules.map { it.id },
            primaryRule = primaryRule,
            alternativeRules = alternatives
        )
    }

    private fun isScopeMatch(rule: DiagnosticRule, product: String?, profile: String?): Boolean {
        val hasProductScope = rule.deviceScope.productCodes.isNotEmpty()
        val hasProfileScope = rule.deviceScope.diagnosticProfiles.isNotEmpty()
        if (!hasProductScope && !hasProfileScope) return true
        if (hasProductScope && product != null && rule.deviceScope.productCodes.contains(product)) return true
        if (hasProfileScope && profile != null && rule.deviceScope.diagnosticProfiles.contains(profile)) return true
        return false
    }

    private fun tryBitmaskDecomposition(
        codeLong: Long,
        profile: String?,
        product: String?,
        panicFamilies: List<PanicFamily>,
        activeRules: List<DiagnosticRule>
    ): List<DiagnosticRule> {
        val bitmaskAllowedProfiles = listOf("SMC_13", "SMC_13_MINI", "SMC_14_PRO")
        if (profile == null || profile !in bitmaskAllowedProfiles || codeLong == 0L) return emptyList()

        val componentRules = activeRules.filter { rule ->
            rule.allowBitmaskDecomposition &&
                rule.sensorCodesExact.isNotEmpty() &&
                isScopeMatch(rule, product, profile) &&
                (rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies })
        }

        return componentRules.filter { rule ->
            rule.sensorCodesExact.any { exactCode ->
                val bit = HexUtils.parseCodeToLong(exactCode)
                bit != null && bit > 0L && HexUtils.hasBits(codeLong, bit)
            }
        }.distinctBy { it.id }
    }

    private fun rankRules(rules: List<DiagnosticRule>, product: String?, profile: String?): List<DiagnosticRule> =
        rules.sortedWith(
            compareByDescending<DiagnosticRule> { it.primaryEligible }
                .thenByDescending { confidenceScore(it.confidence) }
                .thenByDescending { it.priority }
                .thenByDescending { scopeSpecificity(it, product, profile) }
        )

    private fun confidenceScore(confidence: ConfidenceLevel): Int = when (confidence) {
        ConfidenceLevel.HIGH -> 4
        ConfidenceLevel.MEDIUM -> 3
        ConfidenceLevel.LOW -> 2
        ConfidenceLevel.UNKNOWN -> 1
    }

    private fun scopeSpecificity(rule: DiagnosticRule, product: String?, profile: String?): Int {
        if (product != null && rule.deviceScope.productCodes.contains(product)) return 3
        if (profile != null && rule.deviceScope.diagnosticProfiles.contains(profile)) return 2
        return 1
    }
}

object CandidateRanker {
    fun toCandidates(
        primaryRule: DiagnosticRule?,
        alternativeRules: List<DiagnosticRule>
    ): Pair<DiagnosisCandidate?, List<DiagnosisCandidate>> {
        val primary = primaryRule?.toCandidate(isPrimary = true)
        val alternatives = alternativeRules.map { it.toCandidate(isPrimary = false) }
        return primary to alternatives
    }

    private fun DiagnosticRule.toCandidate(isPrimary: Boolean): DiagnosisCandidate = DiagnosisCandidate(
        ruleId = id,
        label = diagnosis.label,
        subsystem = diagnosis.subsystem,
        suspectedComponents = diagnosis.suspectedComponents,
        interpretation = diagnosis.interpretation,
        confidence = confidence,
        verificationStatus = verificationStatus,
        isPrimary = isPrimary,
        repairFlow = repairFlow
    )
}

object DiagnosticReportBuilder {
    fun build(
        sourceFilename: String?,
        rawLog: String,
        metadata: ParsedMetadata,
        deviceModel: DeviceModel?,
        panicFamilies: List<PanicFamily>,
        evidences: List<DiagnosticEvidence>,
        primaryCandidate: DiagnosisCandidate?,
        alternativeCandidates: List<DiagnosisCandidate>,
        kbVersion: String,
        saveRawLogsPreference: Boolean,
        clock: Clock,
        idGenerator: IdGenerator
    ): DiagnosticReport {
        val resolvedConfidence = primaryCandidate?.confidence ?: ConfidenceLevel.UNKNOWN
        val resolvedVerification = primaryCandidate?.verificationStatus ?: VerificationStatus.UNKNOWN
        val resolvedRepairFlow = primaryCandidate?.repairFlow ?: RepairFlow()

        val panicSummary = when {
            !metadata.panicString.isNullOrBlank() -> metadata.panicString.lines().take(3).joinToString("\n")
            panicFamilies.isNotEmpty() -> "Familias de pánico identificadas: ${panicFamilies.joinToString(", ") { it.name }}"
            else -> "No se detectó un mensaje de pánico estructurado."
        }

        return DiagnosticReport(
            id = idGenerator.nextId(),
            createdAt = clock.nowEpochMillis(),
            sourceFilename = sourceFilename,
            deviceModel = deviceModel,
            productCode = metadata.product ?: deviceModel?.productCode ?: "Desconocido",
            osVersion = metadata.osVersion ?: "Desconocido",
            build = metadata.build ?: "N/A",
            panicFamilies = panicFamilies,
            panicStringSummary = panicSummary,
            evidences = evidences,
            primaryCandidate = primaryCandidate,
            alternativeCandidates = alternativeCandidates,
            confidence = resolvedConfidence,
            verificationStatus = resolvedVerification,
            repairFlow = resolvedRepairFlow,
            knowledgeBaseVersion = kbVersion,
            rawLog = if (saveRawLogsPreference) rawLog else null,
            rawLogSaved = saveRawLogsPreference
        )
    }
}
