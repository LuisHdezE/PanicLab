package com.example.appleknowledge

import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleKnowledgeAuthority
import com.example.appleknowledge.model.AppleKnowledgeCard
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleKnowledgeSourceType
import com.example.appleknowledge.model.AppleModelCapability
import com.example.appleknowledge.model.AppleModelScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppleOfficialKnowledgeContractsTest {

    @Test
    fun rulePackEffectIsStructurallyLimitedToNone() {
        assertEquals(
            listOf(AppleKnowledgeRulePackEffect.NONE),
            AppleKnowledgeRulePackEffect.entries.toList()
        )
    }

    @Test
    fun exactModelScopeDoesNotTreatFamilyLabelAsAnExactModel() {
        val scope = AppleModelScope(
            displayScope = "iPhone 12 family",
            exactModels = listOf("iPhone 12", "iPhone 12 mini", "iPhone 12 Pro", "iPhone 12 Pro Max")
        )

        assertTrue(scope.appliesToExactModel("iPhone 12 Pro"))
        assertTrue(scope.appliesToExactModel("  iphone 12 pro max  "))
        assertFalse(scope.appliesToExactModel("iPhone 12 family"))
        assertFalse(scope.appliesToExactModel("iPhone 13"))
    }

    @Test
    fun officialSourceRejectsNonHttpsUrls() {
        assertFailsWith<IllegalArgumentException> {
            AppleKnowledgeSource(
                id = "AOK-001",
                sourceType = AppleKnowledgeSourceType.TECH_SPECS,
                topic = "Architecture",
                applicableModels = listOf("iPhone 12"),
                title = "Example",
                officialUrl = "http://support.apple.com/example",
                sourceStatus = AppleKnowledgeSourceStatus.CURRENT,
                authority = AppleKnowledgeAuthority.APPLE_OFFICIAL,
                verifiedAt = "2026-09-21"
            )
        }
    }

    @Test
    fun cardDefaultsPreserveGovernanceFirewall() {
        val card = AppleKnowledgeCard(
            id = "AOKF-12-001",
            modelScope = AppleModelScope(
                displayScope = "iPhone 12 family",
                exactModels = listOf("iPhone 12")
            ),
            category = AppleKnowledgeCategory.BATTERY_CHARGING_POWER,
            subcategory = "Battery / power",
            symptoms = "No power",
            quickChecks = "Check known-good power source",
            appleDiagnostics = "Use supported Apple diagnostics when available",
            inspectionOrDiscard = "Separate external power from device failure",
            appleAction = "Escalate to service if unresolved",
            primarySourceId = "AOK-001",
            sourceStatus = AppleKnowledgeSourceStatus.CURRENT,
            panicLabCorrelation = "Informational context only",
            verifiedAt = "2026-09-21"
        )

        assertEquals(AppleKnowledgeRulePackEffect.NONE, card.rulePackEffect)
        assertTrue(card.modelScope.appliesToExactModel("iPhone 12"))
    }

    @Test
    fun capabilityContractKeepsDifferentAppleRepairConceptsIndependent() {
        val capability = AppleModelCapability(
            family = "iPhone SE 3rd gen",
            exactModels = listOf("iPhone SE (3rd generation)"),
            releaseYears = "2022",
            publicRepairManual = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            diagnosticsSsr = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            recoveryDiagnosticsMode = AppleCapability(AppleCapabilityAvailability.NOT_SUPPORTED),
            repairAssistant = AppleCapability(
                AppleCapabilityAvailability.SUPPORTED,
                notes = "Battery + Touch ID"
            ),
            partsServiceHistoryCapabilities = listOf("Battery"),
            troubleshooting = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            primaryOfficialUrl = "https://support.apple.com/example",
            verifiedAt = "2026-09-21"
        )

        assertEquals(AppleCapabilityAvailability.SUPPORTED, capability.diagnosticsSsr.availability)
        assertEquals(
            AppleCapabilityAvailability.NOT_SUPPORTED,
            capability.recoveryDiagnosticsMode.availability
        )
        assertEquals(listOf("Battery"), capability.partsServiceHistoryCapabilities)
    }
}
