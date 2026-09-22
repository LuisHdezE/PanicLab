package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeCardJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceReferenceAudit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeSourceReferenceAuditTest {

    @Test
    fun frozenV1SourceReferenceAuditKeepsGovernedMappingMetricsStable() {
        val sources = (1..3)
            .flatMap { part -> AppleOfficialKnowledgeSourceJsonParser.parse(readSourcePart(part)).records }
        val cards = cardResourceNames
            .flatMap { name -> AppleOfficialKnowledgeCardJsonParser.parse(readResource(name)).records }

        val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sources, cards)
        val summary = audit.summary

        assertEquals(776, summary.totalOccurrences)
        assertEquals(117, summary.uniqueUrls)
        assertEquals(68, summary.exactSingleUniqueUrls)
        assertEquals(4, summary.exactAmbiguousUniqueUrls)
        assertEquals(45, summary.unmatchedUniqueUrls)
        assertEquals(9, summary.localePathCandidateUniqueUrls)
        assertEquals(36, summary.noCandidateUniqueUrls)
        assertEquals(
            summary.totalOccurrences,
            summary.exactSingleOccurrences +
                summary.exactAmbiguousOccurrences +
                summary.unmatchedOccurrences
        )
    }

    @Test
    fun auditNeverInventsIdsForUnmappedOrAmbiguousUrls() {
        val sources = (1..3)
            .flatMap { part -> AppleOfficialKnowledgeSourceJsonParser.parse(readSourcePart(part)).records }
        val cards = cardResourceNames
            .flatMap { name -> AppleOfficialKnowledgeCardJsonParser.parse(readResource(name)).records }

        val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sources, cards)

        val unmapped101969 = audit.entries.single {
            it.url == "https://support.apple.com/es-es/101969"
        }
        assertTrue(unmapped101969.exactSourceIds.isEmpty())
        assertTrue(unmapped101969.isUnmapped)

        val sharedIdentification = audit.entries.single {
            it.url == "https://support.apple.com/es-es/108044"
        }
        assertTrue(sharedIdentification.isExactAmbiguous)
        assertTrue(sharedIdentification.exactSourceIds.size > 1)

        val diagnosticsSsr = audit.entries.single {
            it.url == "https://support.apple.com/es-es/101965"
        }
        assertEquals(listOf("AOK-031"), diagnosticsSsr.exactSourceIds)
    }

    private fun readSourcePart(part: Int): String = readResource(
        "appleknowledge/apple_official_knowledge_sources_v1_part$part.json"
    )

    private fun readResource(name: String): String {
        val stream = assertNotNull(javaClass.classLoader.getResourceAsStream(name))
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        val cardResourceNames = listOf(
            "appleknowledge/apple_official_knowledge_cards_v1_part1.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part2a.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part2b.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part2c.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part2d.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part3a.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part3b.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part3c.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part3d.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part4a.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part4b.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part4c.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part4d.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part5a.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part5b.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part5c.json",
            "appleknowledge/apple_official_knowledge_cards_v1_part5d.json"
        )
    }
}
