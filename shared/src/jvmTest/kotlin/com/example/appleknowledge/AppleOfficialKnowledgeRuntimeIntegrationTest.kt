package com.example.appleknowledge

import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceBundle
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceNames
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntime
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntimeFactory
import com.example.appleknowledge.runtime.NativeAppleOfficialKnowledgeFacade
import com.example.appleknowledge.runtime.NativeEmbeddedAppleOfficialKnowledgeFacade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeRuntimeIntegrationTest {

    @Test
    fun frozenResourcesBuildCompleteRuntimeCatalogWithoutApplicabilityHoles() {
        assertCatalog(AppleOfficialKnowledgeRuntime.create(loadBundle()))
    }

    @Test
    fun generatedEmbeddedDatasetMatchesTheGovernedFrozenResources() {
        val fileRuntime = AppleOfficialKnowledgeRuntime.create(loadBundle())
        val embeddedRuntime = AppleOfficialKnowledgeRuntimeFactory.embedded()

        assertCatalog(embeddedRuntime)
        assertEquals(fileRuntime.catalog.datasetVersion, embeddedRuntime.catalog.datasetVersion)
        assertEquals(fileRuntime.catalog.exactModels, embeddedRuntime.catalog.exactModels)

        fileRuntime.catalog.exactModels.forEach { model ->
            assertEquals(
                fileRuntime.catalog.getCardsForExactModel(model).map { it.id },
                embeddedRuntime.catalog.getCardsForExactModel(model).map { it.id },
                "Embedded applicability differs for $model"
            )
        }
    }

    @Test
    fun nativeFacadesPreservePresentationFirewallAndSourceResolution() {
        val bundle = loadBundle()
        val resourceFacade = NativeAppleOfficialKnowledgeFacade(
            capabilitiesJson = bundle.capabilitiesJson,
            sourceJsonParts = bundle.sourceJsonParts,
            cardJsonParts = bundle.cardJsonParts
        )
        val embeddedFacade = NativeEmbeddedAppleOfficialKnowledgeFacade()

        listOf(resourceFacade, embeddedFacade).forEach { facade ->
            assertEquals(48, facade.exactModels().size)
            val summary = assertNotNull(facade.modelSummary("iPhone 12"))
            assertEquals("iPhone 12", summary.family)
            assertEquals("SUPPORTED", summary.publicRepairManual)
            assertEquals("NOT_SUPPORTED", summary.recoveryDiagnosticsMode)

            val cards = facade.cards("iPhone 12")
            assertEquals(13, cards.size)
            val card = assertNotNull(facade.cardDetail("AOKF-12-001"))
            assertEquals("NONE", runtimeRulePackEffect(card.id))
            assertTrue(card.sourceReferences.any { it.resolution == "UNMAPPED" })
            assertTrue(card.sourceReferences.any { it.resolution == "EXACT_SINGLE" })
        }
    }

    private fun assertCatalog(runtime: AppleOfficialKnowledgeRuntime) {
        val catalog = runtime.catalog

        assertEquals("1.0.0", catalog.datasetVersion)
        assertEquals(19, catalog.capabilities.size)
        assertEquals(48, catalog.exactModels.size)

        val allMappedCards = catalog.exactModels
            .flatMap(catalog::getCardsForExactModel)
            .associateBy { it.id }

        assertEquals(241, allMappedCards.size)
        assertTrue(allMappedCards.values.all { it.rulePackEffect.name == "NONE" })

        assertEquals(13, catalog.getCardsForExactModel("iPhone 12").size)
        assertEquals(9, catalog.getCardsForExactModel("iPhone 12 mini").size)
        assertEquals(10, catalog.getCardsForExactModel("iPhone 12 Pro").size)
        assertEquals(9, catalog.getCardsForExactModel("iPhone 12 Pro Max").size)

        assertTrue(catalog.getCardsForExactModel("iPhone 12 Pro").any { it.id == "AOKF-12-013" })
        assertFalse(catalog.getCardsForExactModel("iPhone 12 mini").any { it.id == "AOKF-12-013" })
        assertFalse(catalog.getCardsForExactModel("iPhone 12 Pro Max").any { it.id == "AOKF-12-013" })

        assertFalse(catalog.getCardsForExactModel("iPhone X").any { it.id == "AOKF-X-012" })
        assertTrue(catalog.getCardsForExactModel("iPhone XR").any { it.id == "AOKF-X-012" })
        assertEquals(
            listOf("AOKF-6-013"),
            catalog.getCardsForExactModel("iPhone 6 Plus")
                .filter { it.id == "AOKF-6-013" }
                .map { it.id }
        )
        assertFalse(catalog.getCardsForExactModel("iPhone 6").any { it.id == "AOKF-6-013" })
    }

    private fun runtimeRulePackEffect(cardId: String): String {
        val runtime = AppleOfficialKnowledgeRuntimeFactory.embedded()
        return assertNotNull(runtime.catalog.getCardById(cardId)).rulePackEffect.name
    }

    private fun loadBundle() = AppleOfficialKnowledgeResourceBundle(
        capabilitiesJson = readResource(AppleOfficialKnowledgeResourceNames.capabilities),
        sourceJsonParts = AppleOfficialKnowledgeResourceNames.sources.map(::readResource),
        cardJsonParts = AppleOfficialKnowledgeResourceNames.cards.map(::readResource)
    )

    private fun readResource(name: String): String {
        val stream = assertNotNull(javaClass.classLoader.getResourceAsStream(name), "Missing resource $name")
        return stream.bufferedReader().use { it.readText() }
    }
}
