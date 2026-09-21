package com.example.appleknowledge

import com.example.appleknowledge.data.AppleKnowledgeCardSeedRecord
import com.example.appleknowledge.data.AppleKnowledgeSourceSeedRecord
import com.example.appleknowledge.data.AppleOfficialKnowledgeQueryMapper
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceReferenceAudit
import com.example.appleknowledge.model.AppleKnowledgeAuthority
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleKnowledgeSourceType
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppleOfficialKnowledgeQueryMapperTest {

    @Test
    fun mapperPreservesExactAmbiguousLocaleCandidateAndUnmappedReferences() {
        val sources = listOf(
            source("AOK-001", "https://support.apple.com/es-es/108044"),
            source("AOK-002", "https://support.apple.com/es-es/108044"),
            source("AOK-003", "https://support.apple.com/en-us/101965"),
            source("AOK-004", "https://support.apple.com/es-es/100464")
        )
        val card = card(
            primaryUrl = "https://support.apple.com/es-es/108044",
            secondaryRaw = listOf(
                "https://support.apple.com/es-es/101965",
                "https://support.apple.com/es-es/999999",
                "https://support.apple.com/es-es/100464"
            ).joinToString(" | ")
        )
        val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sources, listOf(card))

        val mapped = AppleOfficialKnowledgeQueryMapper.map(card, audit)

        assertEquals(AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS, mapped.primarySource.resolution)
        assertEquals(listOf("AOK-001", "AOK-002"), mapped.primarySource.exactSourceIds)

        assertEquals(AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE, mapped.secondarySources[0].resolution)
        assertEquals(emptyList(), mapped.secondarySources[0].exactSourceIds)
        assertEquals(listOf("AOK-003"), mapped.secondarySources[0].candidateSourceIds)

        assertEquals(AppleKnowledgeSourceReferenceResolution.UNMAPPED, mapped.secondarySources[1].resolution)
        assertTrue(mapped.secondarySources[1].exactSourceIds.isEmpty())
        assertTrue(mapped.secondarySources[1].candidateSourceIds.isEmpty())

        assertEquals(AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE, mapped.secondarySources[2].resolution)
        assertEquals(listOf("AOK-004"), mapped.secondarySources[2].exactSourceIds)
        assertEquals(AppleKnowledgeRulePackEffect.NONE, mapped.rulePackEffect)
        assertEquals(AppleKnowledgeSourceStatus.CURRENT, mapped.sourceStatus)
        assertEquals(listOf(AppleKnowledgeSourceStatus.HISTORICAL), mapped.contextualSourceStatuses)
    }

    @Test
    fun mapperRejectsAuditThatDoesNotContainEveryGovernedCardUrl() {
        val card = card(
            primaryUrl = "https://support.apple.com/es-es/123456",
            secondaryRaw = ""
        )
        val emptyAudit = AppleOfficialKnowledgeSourceReferenceAudit.audit(emptyList(), emptyList())

        assertFailsWith<IllegalArgumentException> {
            AppleOfficialKnowledgeQueryMapper.map(card, emptyAudit)
        }
    }

    private fun source(id: String, url: String) = AppleKnowledgeSourceSeedRecord(
        source = AppleKnowledgeSource(
            id = id,
            sourceType = AppleKnowledgeSourceType.TROUBLESHOOTING,
            topic = "Test source",
            applicableModels = listOf("iPhone 12"),
            title = "Test source $id",
            officialUrl = url,
            sourceStatus = AppleKnowledgeSourceStatus.CURRENT,
            authority = AppleKnowledgeAuthority.APPLE_OFFICIAL,
            verifiedAt = "2026-09-21"
        ),
        panicLabUtility = "Query test",
        proposedModule = "Apple Official Knowledge"
    )

    private fun card(primaryUrl: String, secondaryRaw: String) = AppleKnowledgeCardSeedRecord(
        id = "AOKF-12-999",
        modelScopeRaw = "iPhone 12",
        category = AppleKnowledgeCategory.BATTERY_CHARGING_POWER,
        subcategory = "Test",
        symptoms = "Test symptom",
        quickChecks = "Test check",
        appleDiagnostics = "Test diagnostics",
        inspectionOrDiscard = "Test inspection",
        appleAction = "Test action",
        primarySourceUrl = primaryUrl,
        secondarySourceRaw = secondaryRaw,
        sourceStatusRaw = "CURRENT + HISTORICAL",
        detailLevelRaw = "DETAILED",
        panicLabCorrelation = "Context only",
        rulePackEffectRaw = "NONE",
        stateRaw = "PILOT_READY",
        verifiedAt = "2026-09-21",
        applicabilityNotes = "Test applicability"
    )
}
