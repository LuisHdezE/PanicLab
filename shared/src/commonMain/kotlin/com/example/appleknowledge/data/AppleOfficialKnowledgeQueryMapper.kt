package com.example.appleknowledge.data

import com.example.appleknowledge.model.AppleKnowledgeCardState
import com.example.appleknowledge.model.AppleKnowledgeDetailLevel
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.query.AppleKnowledgeCardView
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceRole
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceView

/**
 * Lossless bridge from governed I2 card rows to the I3 read model.
 *
 * Exact source ids are only exposed when the audit proves an exact URL match. Locale/path matches
 * remain candidates and unmapped URLs remain unmapped. This mapper never fabricates AOK ids.
 */
object AppleOfficialKnowledgeQueryMapper {

    fun mapAll(
        cards: List<AppleKnowledgeCardSeedRecord>,
        audit: AppleSourceReferenceAuditResult
    ): List<AppleKnowledgeCardView> {
        val entriesByUrl = audit.entries.associateBy { it.url }
        return cards.map { card -> map(card, entriesByUrl) }
    }

    fun map(
        card: AppleKnowledgeCardSeedRecord,
        audit: AppleSourceReferenceAuditResult
    ): AppleKnowledgeCardView = map(card, audit.entries.associateBy { it.url })

    private fun map(
        card: AppleKnowledgeCardSeedRecord,
        entriesByUrl: Map<String, AppleSourceReferenceAuditEntry>
    ): AppleKnowledgeCardView {
        val normalizedStatus = AppleOfficialKnowledgeCardJsonParser.normalizeSourceStatus(card.sourceStatusRaw)
        val primary = reference(
            role = AppleKnowledgeSourceReferenceRole.PRIMARY,
            url = card.primarySourceUrl,
            entriesByUrl = entriesByUrl
        )
        val secondary = card.secondarySourceRaw
            .split(" | ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { url ->
                reference(
                    role = AppleKnowledgeSourceReferenceRole.SECONDARY,
                    url = url,
                    entriesByUrl = entriesByUrl
                )
            }

        return AppleKnowledgeCardView(
            id = card.id,
            modelScope = card.modelScopeRaw,
            category = card.category,
            subcategory = card.subcategory,
            symptoms = card.symptoms,
            quickChecks = card.quickChecks,
            appleDiagnostics = card.appleDiagnostics,
            inspectionOrDiscard = card.inspectionOrDiscard,
            appleAction = card.appleAction,
            primarySource = primary,
            secondarySources = secondary,
            sourceStatus = normalizedStatus.primary,
            contextualSourceStatuses = normalizedStatus.contextual,
            detailLevel = enumValueOfStrict(card.detailLevelRaw, "detailLevelRaw"),
            panicLabCorrelation = card.panicLabCorrelation,
            rulePackEffect = enumValueOfStrict(card.rulePackEffectRaw, "rulePackEffectRaw"),
            state = enumValueOfStrict(card.stateRaw, "stateRaw"),
            verifiedAt = card.verifiedAt,
            applicabilityNotes = card.applicabilityNotes
        )
    }

    private fun reference(
        role: AppleKnowledgeSourceReferenceRole,
        url: String,
        entriesByUrl: Map<String, AppleSourceReferenceAuditEntry>
    ): AppleKnowledgeSourceReferenceView {
        val entry = requireNotNull(entriesByUrl[url]) {
            "Source-reference audit is missing governed card URL '$url'"
        }

        val resolution = when {
            entry.exactSourceIds.size == 1 -> AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE
            entry.exactSourceIds.size > 1 -> AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS
            entry.localePathCandidateIds.isNotEmpty() -> AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE
            else -> AppleKnowledgeSourceReferenceResolution.UNMAPPED
        }

        return AppleKnowledgeSourceReferenceView(
            role = role,
            url = url,
            resolution = resolution,
            exactSourceIds = entry.exactSourceIds,
            candidateSourceIds = entry.localePathCandidateIds
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOfStrict(raw: String, field: String): T =
        runCatching { enumValueOf<T>(raw) }
            .getOrElse { throw IllegalArgumentException("Unsupported $field '$raw'") }
}
