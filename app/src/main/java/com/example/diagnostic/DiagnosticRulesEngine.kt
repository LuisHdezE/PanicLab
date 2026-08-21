package com.example.diagnostic

import com.example.domain.model.*
import com.example.util.HexUtils

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

        // --- STAGE 1: Missing Sensors (thermalmonitord / PRS0, Mic1, TG0V, TG0B, etc.) ---
        if (extractedSensors.missingSensorTokens.isNotEmpty() || panicFamilies.contains(PanicFamily.THERMAL_MISSING_SENSOR)) {
            for (token in extractedSensors.missingSensorTokens) {
                // Find matching rules for this sensor token
                val matchingTokenRules = activeRules.filter { rule ->
                    val familyMatch = rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies }
                    val tokenMatch = rule.sensorTokens.any { it.equals(token, ignoreCase = true) }
                    val scopeMatch = isScopeMatch(rule, resolvedProduct, resolvedProfile)
                    familyMatch && tokenMatch && scopeMatch
                }
                matchedRules.addAll(matchingTokenRules)
            }
        }

        // --- STAGE 2: SMC Sensor Codes (0x800, 0x1000, 0x1800, 0x500000, 3145728, etc.) ---
        if (extractedSensors.smcSensorCodes.isNotEmpty() ||
            panicFamilies.contains(PanicFamily.SMC_BSC_FAILURE) ||
            panicFamilies.contains(PanicFamily.SMC_ASSERTION)
        ) {
            for (code in extractedSensors.smcSensorCodes) {
                val canonicalHex = HexUtils.toCanonicalHex(code)
                val decimalStr = HexUtils.toDecimalString(code)

                // 2.1 Exact code matches for this specific device/profile
                val exactMatches = activeRules.filter { rule ->
                    val familyMatch = rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies }
                    val codeMatch = rule.sensorCodesExact.any {
                        HexUtils.areCodesEquivalent(it, code) ||
                        it.equals(code, ignoreCase = true) ||
                        it.equals(canonicalHex, ignoreCase = true) ||
                        it.equals(decimalStr, ignoreCase = true)
                    }
                    val scopeMatch = isScopeMatch(rule, resolvedProduct, resolvedProfile)
                    familyMatch && codeMatch && scopeMatch
                }

                if (exactMatches.isNotEmpty()) {
                    matchedRules.addAll(exactMatches)
                } else {
                    // 2.2 Bitmask decomposition ONLY if device profile permits it and rule allows it
                    val bitmaskMatches = tryBitmaskDecomposition(
                        code = code,
                        profile = resolvedProfile,
                        product = resolvedProduct,
                        panicFamilies = panicFamilies,
                        activeRules = activeRules
                    )
                    if (bitmaskMatches.isNotEmpty()) {
                        matchedRules.addAll(bitmaskMatches)
                    }
                }
            }
        }

        // --- STAGE 3: Subsystem / Panic Family specific rules (AOP Bosch, ANS2, AppleSocHot, SEP ROM, I2C, Watchdog) ---
        for (family in panicFamilies) {
            val familyRules = activeRules.filter { rule ->
                val familyMatch = rule.panicFamilies.contains(family)
                val noSensorReq = rule.sensorTokens.isEmpty() && rule.sensorCodesExact.isEmpty()
                val scopeMatch = isScopeMatch(rule, resolvedProduct, resolvedProfile)
                familyMatch && noSensorReq && scopeMatch
            }
            matchedRules.addAll(familyRules)
        }

        val distinctMatchedRules = matchedRules.distinctBy { it.id }

        // --- STAGE 4: Unknown / Fallback handling ---
        // If an SMC failure or sensor code was present, but no primary-eligible rule was found
        val hasSmcContext = panicFamilies.contains(PanicFamily.SMC_BSC_FAILURE) ||
                            panicFamilies.contains(PanicFamily.SMC_ASSERTION) ||
                            extractedSensors.smcSensorCodes.isNotEmpty()

        val hasPrimaryMatch = distinctMatchedRules.any { it.primaryEligible && it.confidence != ConfidenceLevel.UNKNOWN && it.confidence != ConfidenceLevel.LOW }

        val finalMatchedList = distinctMatchedRules.toMutableList()

        if (hasSmcContext && !hasPrimaryMatch) {
            // Find fallback rule
            val fallbackRule = activeRules.find { it.id == "smc_unknown_code_fallback" }
            if (fallbackRule != null && !finalMatchedList.contains(fallbackRule)) {
                finalMatchedList.add(fallbackRule)
            }
        }

        // Rank candidates
        val rankedRules = rankRules(finalMatchedList, resolvedProduct, resolvedProfile)

        val primaryRule = rankedRules.firstOrNull { it.primaryEligible }
            ?: rankedRules.firstOrNull()

        val alternativeRules = rankedRules.filter { it.id != primaryRule?.id }

        return RuleMatchResult(
            matchedRules = rankedRules,
            appliedRuleIds = rankedRules.map { it.id },
            primaryRule = primaryRule,
            alternativeRules = alternativeRules
        )
    }

    private fun isScopeMatch(rule: DiagnosticRule, product: String?, profile: String?): Boolean {
        val hasProductScope = rule.deviceScope.productCodes.isNotEmpty()
        val hasProfileScope = rule.deviceScope.diagnosticProfiles.isNotEmpty()

        // If rule has no specific device scope, it applies universally
        if (!hasProductScope && !hasProfileScope) {
            return true
        }

        if (hasProductScope && product != null && rule.deviceScope.productCodes.contains(product)) {
            return true
        }

        if (hasProfileScope && profile != null && rule.deviceScope.diagnosticProfiles.contains(profile)) {
            return true
        }

        return false
    }

    private fun tryBitmaskDecomposition(
        code: String,
        profile: String?,
        product: String?,
        panicFamilies: List<PanicFamily>,
        activeRules: List<DiagnosticRule>
    ): List<DiagnosticRule> {
        // Bitmask policies check: only allowed for SMC_13, SMC_13_MINI, SMC_14_PRO
        val bitmaskAllowedProfiles = listOf("SMC_13", "SMC_13_MINI", "SMC_14_PRO")
        if (profile == null || !bitmaskAllowedProfiles.contains(profile)) {
            return emptyList()
        }

        val codeLong = HexUtils.parseCodeToLong(code) ?: return emptyList()
        if (codeLong == 0L) return emptyList()

        val decomposedMatches = mutableListOf<DiagnosticRule>()

        // Get exact component bit rules for this profile
        val componentRules = activeRules.filter { rule ->
            rule.allowBitmaskDecomposition &&
            rule.sensorCodesExact.isNotEmpty() &&
            isScopeMatch(rule, product, profile) &&
            (rule.panicFamilies.isEmpty() || rule.panicFamilies.any { it in panicFamilies })
        }

        for (compRule in componentRules) {
            for (exactCodeStr in compRule.sensorCodesExact) {
                val bitLong = HexUtils.parseCodeToLong(exactCodeStr)
                if (bitLong != null && bitLong > 0L && HexUtils.hasBits(codeLong, bitLong)) {
                    decomposedMatches.add(compRule)
                }
            }
        }

        return decomposedMatches.distinctBy { it.id }
    }

    private fun rankRules(rules: List<DiagnosticRule>, product: String?, profile: String?): List<DiagnosticRule> {
        return rules.sortedWith(
            compareByDescending<DiagnosticRule> { it.primaryEligible }
                .thenByDescending { getConfidenceScore(it.confidence) }
                .thenByDescending { it.priority }
                .thenByDescending { getScopeSpecificity(it, product, profile) }
        )
    }

    private fun getConfidenceScore(confidence: ConfidenceLevel): Int = when (confidence) {
        ConfidenceLevel.HIGH -> 4
        ConfidenceLevel.MEDIUM -> 3
        ConfidenceLevel.LOW -> 2
        ConfidenceLevel.UNKNOWN -> 1
    }

    private fun getScopeSpecificity(rule: DiagnosticRule, product: String?, profile: String?): Int {
        if (product != null && rule.deviceScope.productCodes.contains(product)) return 3
        if (profile != null && rule.deviceScope.diagnosticProfiles.contains(profile)) return 2
        return 1
    }
}
