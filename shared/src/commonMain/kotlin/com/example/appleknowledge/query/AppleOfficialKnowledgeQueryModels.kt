package com.example.appleknowledge.query

import com.example.appleknowledge.model.AppleKnowledgeCardState
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeDetailLevel
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleModelCapability

enum class AppleKnowledgeSourceReferenceRole {
    PRIMARY,
    SECONDARY
}

enum class AppleKnowledgeSourceReferenceResolution {
    EXACT_SINGLE,
    EXACT_AMBIGUOUS,
    LOCALE_PATH_CANDIDATE,
    UNMAPPED
}

data class AppleKnowledgeSourceReferenceView(
    val role: AppleKnowledgeSourceReferenceRole,
    val url: String,
    val resolution: AppleKnowledgeSourceReferenceResolution,
    val exactSourceIds: List<String> = emptyList(),
    val candidateSourceIds: List<String> = emptyList()
) {
    init {
        require(url.startsWith("https://")) { "Apple source reference URL must use https" }
        require(exactSourceIds.all { it.startsWith("AOK-") }) {
            "exactSourceIds must contain only AOK source ids"
        }
        require(candidateSourceIds.all { it.startsWith("AOK-") }) {
            "candidateSourceIds must contain only AOK source ids"
        }

        when (resolution) {
            AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE -> {
                require(exactSourceIds.size == 1) { "EXACT_SINGLE requires exactly one source id" }
                require(candidateSourceIds.isEmpty()) { "EXACT_SINGLE must not expose candidate ids" }
            }
            AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS -> {
                require(exactSourceIds.size > 1) { "EXACT_AMBIGUOUS requires multiple exact source ids" }
                require(candidateSourceIds.isEmpty()) { "EXACT_AMBIGUOUS must not expose candidate ids" }
            }
            AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE -> {
                require(exactSourceIds.isEmpty()) { "LOCALE_PATH_CANDIDATE must not claim an exact source id" }
                require(candidateSourceIds.isNotEmpty()) { "LOCALE_PATH_CANDIDATE requires candidate ids" }
            }
            AppleKnowledgeSourceReferenceResolution.UNMAPPED -> {
                require(exactSourceIds.isEmpty()) { "UNMAPPED must not claim an exact source id" }
                require(candidateSourceIds.isEmpty()) { "UNMAPPED must not expose candidate ids" }
            }
        }
    }
}

data class AppleKnowledgeCardView(
    val id: String,
    val modelScope: String,
    val category: AppleKnowledgeCategory,
    val subcategory: String,
    val symptoms: String,
    val quickChecks: String,
    val appleDiagnostics: String,
    val inspectionOrDiscard: String,
    val appleAction: String,
    val primarySource: AppleKnowledgeSourceReferenceView,
    val secondarySources: List<AppleKnowledgeSourceReferenceView>,
    val sourceStatus: AppleKnowledgeSourceStatus,
    val contextualSourceStatuses: List<AppleKnowledgeSourceStatus>,
    val detailLevel: AppleKnowledgeDetailLevel,
    val panicLabCorrelation: String,
    val rulePackEffect: AppleKnowledgeRulePackEffect,
    val state: AppleKnowledgeCardState,
    val verifiedAt: String,
    val applicabilityNotes: String
) {
    init {
        require(id.startsWith("AOKF-")) { "Apple knowledge card view id must start with AOKF-" }
        require(modelScope.isNotBlank()) { "modelScope must not be blank" }
        require(primarySource.role == AppleKnowledgeSourceReferenceRole.PRIMARY) {
            "primarySource must use PRIMARY role"
        }
        require(secondarySources.all { it.role == AppleKnowledgeSourceReferenceRole.SECONDARY }) {
            "secondarySources must use SECONDARY role"
        }
        require(rulePackEffect == AppleKnowledgeRulePackEffect.NONE) {
            "Apple Official Knowledge query projections must never affect Rule Pack"
        }
    }
}

data class AppleOfficialKnowledgeModelView(
    val exactModel: String,
    val capability: AppleModelCapability,
    val cards: List<AppleKnowledgeCardView>
)

data class AppleOfficialKnowledgeCardDetail(
    val card: AppleKnowledgeCardView,
    val resolvedExactSources: List<AppleKnowledgeSource>
)
