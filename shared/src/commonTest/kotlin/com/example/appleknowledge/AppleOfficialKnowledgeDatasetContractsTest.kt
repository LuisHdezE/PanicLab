package com.example.appleknowledge

import com.example.appleknowledge.data.AppleKnowledgeDatasetExpectedCounts
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesSeed
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesValidator
import com.example.appleknowledge.data.AppleOfficialKnowledgeDatasetManifest
import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleModelCapability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppleOfficialKnowledgeDatasetContractsTest {

    @Test
    fun parserKeepsAppleRepairCapabilitiesIndependent() {
        val json = """
            {
              "manifest": {
                "schemaVersion": 1,
                "datasetVersion": "1.0.0",
                "datasetId": "APPLE_OFFICIAL_KNOWLEDGE_V1",
                "importStage": "MODEL_CAPABILITIES",
                "scopeLowerBound": "iPhone 5",
                "scopeUpperBound": "current audited generations",
                "scopeFrozen": true,
                "expectedCounts": { "families": 19, "sources": 91, "cards": 241 }
              },
              "modelCapabilities": [
                {
                  "family": "iPhone SE 3rd gen",
                  "exactModels": ["iPhone SE (3.ª generación)"],
                  "releaseYears": "2022",
                  "publicRepairManual": { "availability": "SUPPORTED" },
                  "diagnosticsSsr": { "availability": "SUPPORTED" },
                  "recoveryDiagnosticsMode": { "availability": "NOT_SUPPORTED" },
                  "repairAssistant": { "availability": "SUPPORTED", "notes": "Batería + Touch ID" },
                  "partsServiceHistoryCapabilities": ["Batería"],
                  "troubleshooting": { "availability": "SUPPORTED" },
                  "historicalProgramSourceIds": [],
                  "primaryOfficialUrl": "https://support.apple.com/en-ie/101527",
                  "verifiedAt": "2026-09-21"
                }
              ]
            }
        """.trimIndent()

        val parsed = AppleOfficialKnowledgeCapabilitiesJsonParser.parse(json)
        val capability = parsed.modelCapabilities.single()

        assertEquals(AppleCapabilityAvailability.SUPPORTED, capability.diagnosticsSsr.availability)
        assertEquals(AppleCapabilityAvailability.NOT_SUPPORTED, capability.recoveryDiagnosticsMode.availability)
        assertEquals(AppleCapabilityAvailability.SUPPORTED, capability.repairAssistant.availability)
        assertEquals(listOf("Batería"), capability.partsServiceHistoryCapabilities)
    }

    @Test
    fun validatorRejectsFrozenDatasetCountDrift() {
        val seed = validSeed(
            manifest = validManifest().copy(
                expectedCounts = AppleKnowledgeDatasetExpectedCounts(
                    families = 18,
                    sources = 91,
                    cards = 241
                )
            )
        )

        val result = AppleOfficialKnowledgeCapabilitiesValidator.validate(seed)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path == "manifest.expectedCounts.families" })
    }

    @Test
    fun validatorRejectsNonApplePrimaryUrls() {
        val seed = validSeed(
            capability = validCapability().copy(
                primaryOfficialUrl = "https://example.com/iphone"
            )
        )

        val result = AppleOfficialKnowledgeCapabilitiesValidator.validate(seed)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path.endsWith("primaryOfficialUrl") })
    }

    @Test
    fun validatorRejectsMalformedHistoricalSourceIds() {
        val seed = validSeed(
            capability = validCapability().copy(
                historicalProgramSourceIds = listOf("PROGRAM-27")
            )
        )

        val result = AppleOfficialKnowledgeCapabilitiesValidator.validate(seed)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path.endsWith("historicalProgramSourceIds") })
    }

    private fun validSeed(
        manifest: AppleOfficialKnowledgeDatasetManifest = validManifest(),
        capability: AppleModelCapability = validCapability()
    ) = AppleOfficialKnowledgeCapabilitiesSeed(
        manifest = manifest,
        modelCapabilities = listOf(capability)
    )

    private fun validManifest() = AppleOfficialKnowledgeDatasetManifest(
        schemaVersion = 1,
        datasetVersion = "1.0.0",
        datasetId = "APPLE_OFFICIAL_KNOWLEDGE_V1",
        importStage = "MODEL_CAPABILITIES",
        scopeLowerBound = "iPhone 5",
        scopeUpperBound = "current audited generations",
        scopeFrozen = true,
        expectedCounts = AppleKnowledgeDatasetExpectedCounts(
            families = 19,
            sources = 91,
            cards = 241
        )
    )

    private fun validCapability() = AppleModelCapability(
        family = "iPhone 5",
        exactModels = listOf("iPhone 5"),
        releaseYears = "2012",
        publicRepairManual = AppleCapability(AppleCapabilityAvailability.NOT_FOUND_PUBLICLY),
        diagnosticsSsr = AppleCapability(AppleCapabilityAvailability.NOT_SUPPORTED),
        recoveryDiagnosticsMode = AppleCapability(AppleCapabilityAvailability.NOT_SUPPORTED),
        repairAssistant = AppleCapability(AppleCapabilityAvailability.NOT_SUPPORTED),
        partsServiceHistoryCapabilities = emptyList(),
        troubleshooting = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
        historicalProgramSourceIds = listOf("AOK-097", "AOK-098"),
        primaryOfficialUrl = "https://support.apple.com/es-es/docs/iphone/133778",
        verifiedAt = "2026-09-21"
    )
}