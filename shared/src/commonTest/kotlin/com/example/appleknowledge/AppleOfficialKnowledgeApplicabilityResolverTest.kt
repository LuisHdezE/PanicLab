package com.example.appleknowledge

import com.example.appleknowledge.data.AppleKnowledgeCardSeedRecord
import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleModelCapability
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeApplicabilityResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppleOfficialKnowledgeApplicabilityResolverTest {

    private val capabilities = listOf(
        capability(
            family = "iPhone 5c",
            models = listOf("iPhone 5c", "iPhone 5c 8 GB")
        ),
        capability(
            family = "iPhone 6",
            models = listOf("iPhone 6", "iPhone 6 Plus")
        ),
        capability(
            family = "iPhone X",
            models = listOf("iPhone X")
        ),
        capability(
            family = "iPhone XR/XS",
            models = listOf("iPhone XR", "iPhone XS", "iPhone XS Max")
        ),
        capability(
            family = "iPhone 12",
            models = listOf("iPhone 12", "iPhone 12 mini", "iPhone 12 Pro", "iPhone 12 Pro Max")
        ),
        capability(
            family = "iPhone 17",
            models = listOf("iPhone 17", "iPhone 17 Pro", "iPhone 17 Pro Max", "iPhone Air", "iPhone 17e")
        )
    )

    @Test
    fun explicitHistoricalScopeDoesNotBroadenToUnlistedVariants() {
        val card = card("iPhone 12 / 12 Pro para known issue histórico; audio troubleshooting general en iPhone 12")

        assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 12", capabilities))
        assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 12 Pro", capabilities))
        assertFalse(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 12 mini", capabilities))
        assertFalse(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 12 Pro Max", capabilities))
    }

    @Test
    fun broadFamilyScopeExpandsOnlyInsideAuditedFamily() {
        val card = card("iPhone 17 family + Air + 17e, según región")

        assertEquals(
            listOf("iPhone 17", "iPhone 17 Pro", "iPhone 17 Pro Max", "iPhone Air", "iPhone 17e"),
            AppleOfficialKnowledgeApplicabilityResolver.resolveExactModels(card.modelScopeRaw, capabilities)
        )
        assertFalse(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 12", capabilities))
    }

    @Test
    fun crossGenerationXScopeKeepsNamedModelsAndVariantGuardrails() {
        val broad = card("iPhone X / XR / XS / XS Max")
        val xrOnly = card("iPhone XR / XS / XS Max")

        listOf("iPhone X", "iPhone XR", "iPhone XS", "iPhone XS Max").forEach {
            assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(broad, it, capabilities))
        }
        assertFalse(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(xrOnly, "iPhone X", capabilities))
        listOf("iPhone XR", "iPhone XS", "iPhone XS Max").forEach {
            assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(xrOnly, it, capabilities))
        }
    }

    @Test
    fun exactPlusOnlyScopeDoesNotLeakToBaseModel() {
        val card = card("iPhone 6 Plus")

        assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 6 Plus", capabilities))
        assertFalse(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 6", capabilities))
    }

    @Test
    fun storageOnly5cAliasSharesTheSameHardwareKnowledge() {
        val card = card("iPhone 5c")

        assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 5c", capabilities))
        assertTrue(AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(card, "iPhone 5c 8 GB", capabilities))
    }

    private fun card(scope: String) = AppleKnowledgeCardSeedRecord(
        id = "AOKF-X-999",
        modelScopeRaw = scope,
        category = AppleKnowledgeCategory.MODEL_GUARDRAILS,
        subcategory = "Applicability",
        symptoms = "",
        quickChecks = "",
        appleDiagnostics = "",
        inspectionOrDiscard = "",
        appleAction = "",
        primarySourceUrl = "https://support.apple.com/es-es/108044",
        secondarySourceRaw = "",
        sourceStatusRaw = "CURRENT",
        detailLevelRaw = "DETAILED",
        panicLabCorrelation = "Context only",
        rulePackEffectRaw = "NONE",
        stateRaw = "PILOT_READY",
        verifiedAt = "2026-09-21",
        applicabilityNotes = ""
    )

    private fun capability(family: String, models: List<String>) = AppleModelCapability(
        family = family,
        exactModels = models,
        releaseYears = "2026",
        publicRepairManual = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        diagnosticsSsr = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        recoveryDiagnosticsMode = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        repairAssistant = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        troubleshooting = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        primaryOfficialUrl = "https://support.apple.com/es-es/108044",
        verifiedAt = "2026-09-21"
    )
}
