package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesValidator
import com.example.appleknowledge.model.AppleCapabilityAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeSeedResourceTest {

    @Test
    fun frozenV1CapabilityResourceParsesAndValidates() {
        val resource = assertNotNull(
            javaClass.classLoader.getResourceAsStream(
                "appleknowledge/apple_official_knowledge_capabilities_v1.json"
            )
        )
        val json = resource.bufferedReader().use { it.readText() }

        val seed = AppleOfficialKnowledgeCapabilitiesJsonParser.parse(json)
        val validation = AppleOfficialKnowledgeCapabilitiesValidator.validate(seed)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(19, seed.modelCapabilities.size)
        assertEquals(91, seed.manifest.expectedCounts.sources)
        assertEquals(241, seed.manifest.expectedCounts.cards)
        assertEquals(48, seed.modelCapabilities.sumOf { it.exactModels.size })
    }

    @Test
    fun frozenV1CapabilityResourcePreservesCriticalModelBoundaries() {
        val resource = assertNotNull(
            javaClass.classLoader.getResourceAsStream(
                "appleknowledge/apple_official_knowledge_capabilities_v1.json"
            )
        )
        val seed = AppleOfficialKnowledgeCapabilitiesJsonParser.parse(
            resource.bufferedReader().use { it.readText() }
        )

        val iphone12 = seed.modelCapabilities.single { it.family == "iPhone 12" }
        assertEquals(AppleCapabilityAvailability.SUPPORTED, iphone12.diagnosticsSsr.availability)
        assertEquals(
            AppleCapabilityAvailability.NOT_SUPPORTED,
            iphone12.recoveryDiagnosticsMode.availability
        )

        val se3 = seed.modelCapabilities.single { it.family == "iPhone SE 3rd gen" }
        assertEquals(AppleCapabilityAvailability.SUPPORTED, se3.publicRepairManual.availability)
        assertEquals(AppleCapabilityAvailability.SUPPORTED, se3.diagnosticsSsr.availability)
        assertEquals(AppleCapabilityAvailability.NOT_SUPPORTED, se3.recoveryDiagnosticsMode.availability)
        assertEquals(listOf("Batería"), se3.partsServiceHistoryCapabilities)

        val iphone16 = seed.modelCapabilities.single { it.family == "iPhone 16" }
        assertTrue("iPhone 16e" in iphone16.exactModels)

        val iphone17 = seed.modelCapabilities.single { it.family == "iPhone 17" }
        assertTrue("iPhone Air" in iphone17.exactModels)
        assertTrue("iPhone 17e" in iphone17.exactModels)
    }
}