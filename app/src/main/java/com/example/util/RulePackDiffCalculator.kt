package com.example.util

import com.example.domain.model.DiagnosticRule
import com.example.domain.model.ParsedRulePack
import com.example.domain.model.RuleDiffItem
import com.example.domain.model.RulePackDiffSummary

object RulePackDiffCalculator {

    fun calculateDiff(
        currentPack: ParsedRulePack?,
        incomingPack: ParsedRulePack
    ): RulePackDiffSummary {
        if (currentPack == null) {
            val allNewDiffs = incomingPack.diagnosticRules.map { rule ->
                RuleDiffItem(
                    ruleId = rule.id,
                    title = rule.title,
                    changeType = RuleDiffItem.ChangeType.ADDED,
                    previousSummary = null,
                    newSummary = "${rule.confidence} | ${rule.verificationStatus} | ${rule.diagnosis.label}",
                    confidenceChange = null,
                    statusChange = null,
                    details = listOf("Nueva regla en la base de conocimiento.")
                )
            }
            return RulePackDiffSummary(
                currentVersion = "Ninguna",
                incomingVersion = incomingPack.knowledgeBaseVersion,
                addedRulesCount = incomingPack.diagnosticRules.size,
                modifiedRulesCount = 0,
                deactivatedRulesCount = 0,
                removedRulesCount = 0,
                addedModelsCount = incomingPack.deviceModels.size,
                modifiedModelsCount = 0,
                addedSourcesCount = incomingPack.sources.size,
                addedClassifiersCount = incomingPack.panicClassifiers.size,
                ruleDiffs = allNewDiffs
            )
        }

        val currentRuleMap = currentPack.diagnosticRules.associateBy { it.id }
        val incomingRuleMap = incomingPack.diagnosticRules.associateBy { it.id }

        val diffItems = mutableListOf<RuleDiffItem>()
        var addedRules = 0
        var modifiedRules = 0
        var deactivatedRules = 0
        var removedRules = 0

        // Check new or modified rules
        incomingPack.diagnosticRules.forEach { newRule ->
            val oldRule = currentRuleMap[newRule.id]
            if (oldRule == null) {
                addedRules++
                diffItems.add(
                    RuleDiffItem(
                        ruleId = newRule.id,
                        title = newRule.title,
                        changeType = RuleDiffItem.ChangeType.ADDED,
                        previousSummary = null,
                        newSummary = "${newRule.confidence} | ${newRule.verificationStatus} | ${newRule.diagnosis.label}",
                        confidenceChange = null,
                        statusChange = null,
                        details = listOf("Nueva regla añadida.")
                    )
                )
            } else {
                val details = mutableListOf<String>()
                var confChange: String? = null
                var statChange: String? = null

                if (oldRule.active && !newRule.active) {
                    deactivatedRules++
                    details.add("Regla desactivada.")
                }

                if (oldRule.confidence != newRule.confidence) {
                    confChange = "${oldRule.confidence} → ${newRule.confidence}"
                    details.add("Confianza: $confChange")
                }

                if (oldRule.verificationStatus != newRule.verificationStatus) {
                    statChange = "${oldRule.verificationStatus} → ${newRule.verificationStatus}"
                    details.add("Estado de verificación: $statChange")
                }

                if (oldRule.diagnosis.label != newRule.diagnosis.label) {
                    details.add("Diagnóstico: '${oldRule.diagnosis.label}' → '${newRule.diagnosis.label}'")
                }

                if (oldRule.priority != newRule.priority) {
                    details.add("Prioridad: ${oldRule.priority} → ${newRule.priority}")
                }

                if (oldRule.primaryEligible != newRule.primaryEligible) {
                    details.add("Elegible como principal: ${oldRule.primaryEligible} → ${newRule.primaryEligible}")
                }

                if (details.isNotEmpty()) {
                    modifiedRules++
                    diffItems.add(
                        RuleDiffItem(
                            ruleId = newRule.id,
                            title = newRule.title,
                            changeType = if (!newRule.active && oldRule.active) RuleDiffItem.ChangeType.DEACTIVATED else RuleDiffItem.ChangeType.MODIFIED,
                            previousSummary = "${oldRule.confidence} | ${oldRule.verificationStatus}",
                            newSummary = "${newRule.confidence} | ${newRule.verificationStatus}",
                            confidenceChange = confChange,
                            statusChange = statChange,
                            details = details
                        )
                    )
                }
            }
        }

        // Check removed rules
        currentPack.diagnosticRules.forEach { oldRule ->
            if (!incomingRuleMap.containsKey(oldRule.id)) {
                removedRules++
                diffItems.add(
                    RuleDiffItem(
                        ruleId = oldRule.id,
                        title = oldRule.title,
                        changeType = RuleDiffItem.ChangeType.REMOVED,
                        previousSummary = "${oldRule.confidence} | ${oldRule.verificationStatus} | ${oldRule.diagnosis.label}",
                        newSummary = null,
                        confidenceChange = null,
                        statusChange = null,
                        details = listOf("Regla eliminada del Rule Pack.")
                    )
                )
            }
        }

        // Models diff
        val currentModelCodes = currentPack.deviceModels.map { it.productCode }.toSet()
        val incomingModelCodes = incomingPack.deviceModels.map { it.productCode }.toSet()
        val addedModels = incomingModelCodes.minus(currentModelCodes).size
        val modifiedModels = incomingPack.deviceModels.count { inc ->
            val curr = currentPack.deviceModels.find { it.productCode == inc.productCode }
            curr != null && (curr.marketingName != inc.marketingName || curr.diagnosticProfile != inc.diagnosticProfile)
        }

        // Sources & Classifiers
        val currentSourceIds = currentPack.sources.map { it.id }.toSet()
        val addedSources = incomingPack.sources.count { !currentSourceIds.contains(it.id) }

        val currentClassifierIds = currentPack.panicClassifiers.map { it.id }.toSet()
        val addedClassifiers = incomingPack.panicClassifiers.count { !currentClassifierIds.contains(it.id) }

        return RulePackDiffSummary(
            currentVersion = currentPack.knowledgeBaseVersion,
            incomingVersion = incomingPack.knowledgeBaseVersion,
            addedRulesCount = addedRules,
            modifiedRulesCount = modifiedRules,
            deactivatedRulesCount = deactivatedRules,
            removedRulesCount = removedRules,
            addedModelsCount = addedModels,
            modifiedModelsCount = modifiedModels,
            addedSourcesCount = addedSources,
            addedClassifiersCount = addedClassifiers,
            ruleDiffs = diffItems
        )
    }
}
