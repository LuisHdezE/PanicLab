package com.example.diagnostic

import com.example.domain.model.DiagnosisCandidate
import com.example.domain.model.DiagnosticRule

object CandidateRanker {

    fun toCandidates(
        primaryRule: DiagnosticRule?,
        alternativeRules: List<DiagnosticRule>
    ): Pair<DiagnosisCandidate?, List<DiagnosisCandidate>> {
        val primaryCandidate = primaryRule?.let { rule ->
            DiagnosisCandidate(
                ruleId = rule.id,
                label = rule.diagnosis.label,
                subsystem = rule.diagnosis.subsystem,
                suspectedComponents = rule.diagnosis.suspectedComponents,
                interpretation = rule.diagnosis.interpretation,
                confidence = rule.confidence,
                verificationStatus = rule.verificationStatus,
                isPrimary = true,
                repairFlow = rule.repairFlow
            )
        }

        val altCandidates = alternativeRules.map { rule ->
            DiagnosisCandidate(
                ruleId = rule.id,
                label = rule.diagnosis.label,
                subsystem = rule.diagnosis.subsystem,
                suspectedComponents = rule.diagnosis.suspectedComponents,
                interpretation = rule.diagnosis.interpretation,
                confidence = rule.confidence,
                verificationStatus = rule.verificationStatus,
                isPrimary = false,
                repairFlow = rule.repairFlow
            )
        }

        return Pair(primaryCandidate, altCandidates)
    }
}
