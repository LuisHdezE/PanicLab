package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesValidator
import com.example.appleknowledge.data.AppleOfficialKnowledgeCardJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCardValidator
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceReferenceAudit
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceValidator
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceBundle
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceNames
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntimeFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeAcceptanceTest {

    @Test
    fun frozenV1DatasetPassesFinalAcceptanceContract() {
        val capabilities = AppleOfficialKnowledgeCapabilitiesJsonParser.parse(
            readResource(AppleOfficialKnowledgeResourceNames.capabilities)
        )
        val sourceParts = AppleOfficialKnowledgeResourceNames.sources
            .map { AppleOfficialKnowledgeSourceJsonParser.parse(readResource(it)) }
        val cardParts = AppleOfficialKnowledgeResourceNames.cards
            .map { AppleOfficialKnowledgeCardJsonParser.parse(readResource(it)) }

        assertTrue(AppleOfficialKnowledgeCapabilitiesValidator.validate(capabilities).isValid)
        assertTrue(AppleOfficialKnowledgeSourceValidator.validate(sourceParts).isValid)
        assertTrue(AppleOfficialKnowledgeCardValidator.validate(cardParts).isValid)

        assertEquals("1.0.0", capabilities.manifest.datasetVersion)
        assertEquals("iPhone 5", capabilities.manifest.scopeLowerBound)
        assertEquals("current audited generations", capabilities.manifest.scopeUpperBound)
        assertTrue(capabilities.manifest.scopeFrozen)
        assertEquals(19, capabilities.manifest.expectedCounts.families)
        assertEquals(91, capabilities.manifest.expectedCounts.sources)
        assertEquals(241, capabilities.manifest.expectedCounts.cards)

        val sources = sourceParts.flatMap { it.records }
        val cards = cardParts.flatMap { it.records }
        assertEquals(91, sources.size)
        assertEquals(241, cards.size)
        assertEquals(91, sources.map { it.source.id }.distinct().size)
        assertEquals(241, cards.map { it.id }.distinct().size)

        assertTrue(sources.all { record ->
            val host = record.source.officialUrl
                .removePrefix("https://")
                .substringBefore('/')
                .lowercase()
            host == "apple.com" || host.endsWith(".apple.com")
        })
        assertTrue(cards.all { it.rulePackEffectRaw == "NONE" })
        assertTrue(cards.all { it.stateRaw == "PILOT_READY" })
        assertTrue(cards.all { it.detailLevelRaw == "DETAILED" })

        val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sources, cards).summary
        assertEquals(776, audit.totalOccurrences)
        assertEquals(117, audit.uniqueUrls)
        assertEquals(68, audit.exactSingleUniqueUrls)
        assertEquals(4, audit.exactAmbiguousUniqueUrls)
        assertEquals(45, audit.unmatchedUniqueUrls)
        assertEquals(9, audit.localePathCandidateUniqueUrls)
        assertEquals(36, audit.noCandidateUniqueUrls)
        assertEquals(521, audit.exactSingleOccurrences)
        assertEquals(111, audit.exactAmbiguousOccurrences)
        assertEquals(144, audit.unmatchedOccurrences)
    }

    @Test
    fun modelToolAndHistoricalGuardrailsRemainFailClosed() {
        val catalog = AppleOfficialKnowledgeRuntimeFactory.embedded().catalog

        val iPhone12 = assertNotNull(catalog.getModelCapability("iPhone 12"))
        val iPhone13 = assertNotNull(catalog.getModelCapability("iPhone 13"))
        val iPhone14 = assertNotNull(catalog.getModelCapability("iPhone 14"))

        assertEquals(AppleCapabilityAvailability.SUPPORTED, iPhone12.diagnosticsSsr.availability)
        assertEquals(AppleCapabilityAvailability.NOT_SUPPORTED, iPhone12.recoveryDiagnosticsMode.availability)
        assertEquals(AppleCapabilityAvailability.SUPPORTED, iPhone13.diagnosticsSsr.availability)
        assertEquals(AppleCapabilityAvailability.NOT_SUPPORTED, iPhone13.recoveryDiagnosticsMode.availability)
        assertEquals(AppleCapabilityAvailability.SUPPORTED, iPhone14.diagnosticsSsr.availability)
        assertEquals(AppleCapabilityAvailability.SUPPORTED, iPhone14.recoveryDiagnosticsMode.availability)

        val allCards = catalog.exactModels
            .flatMap(catalog::getCardsForExactModel)
            .associateBy { it.id }
            .values

        val historicalProgramCards = allCards.filter {
            it.category == AppleKnowledgeCategory.HISTORICAL_SERVICE_PROGRAM
        }
        assertTrue(historicalProgramCards.isNotEmpty())
        historicalProgramCards.forEach { card ->
            assertTrue(
                card.sourceStatus != AppleKnowledgeSourceStatus.CURRENT ||
                    card.contextualSourceStatuses.any {
                        it == AppleKnowledgeSourceStatus.HISTORICAL ||
                            it == AppleKnowledgeSourceStatus.ENDED ||
                            it == AppleKnowledgeSourceStatus.ARCHIVE_VERIFICATION_REQUIRED
                    },
                "${card.id} must not render as plain current knowledge"
            )
        }

        val noSound = assertNotNull(catalog.getCardById("AOKF-12-013"))
        assertEquals(AppleKnowledgeSourceStatus.CURRENT, noSound.sourceStatus)
        assertTrue(AppleKnowledgeSourceStatus.HISTORICAL in noSound.contextualSourceStatuses)
        assertTrue(catalog.getCardsForExactModel("iPhone 12").any { it.id == noSound.id })
        assertTrue(catalog.getCardsForExactModel("iPhone 12 Pro").any { it.id == noSound.id })
        assertFalse(catalog.getCardsForExactModel("iPhone 12 mini").any { it.id == noSound.id })
        assertFalse(catalog.getCardsForExactModel("iPhone 12 Pro Max").any { it.id == noSound.id })

        val magSafe = assertNotNull(catalog.getCardById("AOKF-12-002"))
        assertTrue(magSafe.appleAction.contains("no autoriza", ignoreCase = true))
        assertTrue(magSafe.applicabilityNotes.contains("no tiene procedimiento público", ignoreCase = true))

        val buttons = assertNotNull(catalog.getCardById("AOKF-12-012"))
        assertTrue(buttons.appleAction.contains("iPhone 15+", ignoreCase = true))
        assertTrue(buttons.appleAction.contains("no proyectar", ignoreCase = true))

        assertTrue(allCards.all { it.rulePackEffect.name == "NONE" })
    }

    @Test
    fun unresolvedSourceReferencesStayUnresolvedAndNeverInventIds() {
        val catalog = AppleOfficialKnowledgeRuntimeFactory.embedded().catalog
        val detail = assertNotNull(catalog.getCardDetail("AOKF-12-001"))

        assertEquals(AppleKnowledgeSourceReferenceResolution.UNMAPPED, detail.card.primarySource.resolution)
        assertTrue(detail.card.primarySource.exactSourceIds.isEmpty())
        assertTrue(detail.card.primarySource.candidateSourceIds.isEmpty())

        val exact = detail.card.secondarySources.single {
            it.url == "https://support.apple.com/es-es/101965"
        }
        assertEquals(AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE, exact.resolution)
        assertEquals(listOf("AOK-031"), exact.exactSourceIds)
        assertTrue(exact.candidateSourceIds.isEmpty())

        val resolvedIds = detail.resolvedExactSources.map { it.id }.toSet()
        assertTrue("AOK-031" in resolvedIds)
        assertFalse(resolvedIds.any { it.isBlank() })
    }

    private fun readResource(name: String): String {
        val stream = assertNotNull(javaClass.classLoader.getResourceAsStream(name), "Missing resource $name")
        return stream.bufferedReader().use { it.readText() }
    }

    @Suppress("unused")
    private fun governedBundle() = AppleOfficialKnowledgeResourceBundle(
        capabilitiesJson = readResource(AppleOfficialKnowledgeResourceNames.capabilities),
        sourceJsonParts = AppleOfficialKnowledgeResourceNames.sources.map(::readResource),
        cardJsonParts = AppleOfficialKnowledgeResourceNames.cards.map(::readResource)
    )
}
