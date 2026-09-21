package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeCardJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeQueryMapper
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceReferenceAudit
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeQueryResourceIntegrationTest {

    @Test
    fun frozenV1ResourcesProduceTwoHundredFortyOneLosslessQueryCards() {
        val sources = (1..3)
            .flatMap { part ->
                AppleOfficialKnowledgeSourceJsonParser.parse(
                    readResource("appleknowledge/apple_official_knowledge_sources_v1_part$part.json")
                ).records
            }
        val cards = cardResourceNames.flatMap { name ->
            AppleOfficialKnowledgeCardJsonParser.parse(readResource(name)).records
        }
        val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sources, cards)

        val projections = AppleOfficialKnowledgeQueryMapper.mapAll(cards, audit)
        val references = projections.flatMap { card -> listOf(card.primarySource) + card.secondarySources }

        assertEquals(241, projections.size)
        assertEquals(241, projections.map { it.id }.toSet().size)
        assertEquals(776, references.size)
        assertTrue(projections.all { it.rulePackEffect == AppleKnowledgeRulePackEffect.NONE })
        assertEquals(
            AppleKnowledgeSourceReferenceResolution.UNMAPPED,
            projections.single { it.id == "AOKF-12-001" }.primarySource.resolution
        )

        val iphone12Battery = projections.single { it.id == "AOKF-12-001" }
        val diagnosticsReference = iphone12Battery.secondarySources.single {
            it.url == "https://support.apple.com/es-es/101965"
        }
        assertEquals(AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE, diagnosticsReference.resolution)
        assertEquals(listOf("AOK-031"), diagnosticsReference.exactSourceIds)

        assertTrue(references.any { it.resolution == AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS })
        assertTrue(references.any { it.resolution == AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE })
        assertTrue(references.any { it.resolution == AppleKnowledgeSourceReferenceResolution.UNMAPPED })
    }

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
