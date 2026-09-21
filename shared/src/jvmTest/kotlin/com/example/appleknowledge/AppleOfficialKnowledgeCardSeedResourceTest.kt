package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeCardJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCardValidator
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeCardSeedResourceTest {

    @Test
    fun frozenV1CardDatasetParsesAndValidatesExactlyTwoHundredFortyOneCards() {
        val fragments = resourceNames.map { name ->
            AppleOfficialKnowledgeCardJsonParser.parse(readResource(name))
        }
        val validation = AppleOfficialKnowledgeCardValidator.validate(fragments)
        val records = fragments.flatMap { it.records }

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(17, fragments.size)
        assertEquals(setOf(1, 2, 3, 4, 5), fragments.map { it.part }.toSet())
        assertEquals(49, fragments.single { it.part == 1 }.records.size)
        assertEquals(48, fragments.filter { it.part == 2 }.sumOf { it.records.size })
        assertEquals(48, fragments.filter { it.part == 3 }.sumOf { it.records.size })
        assertEquals(48, fragments.filter { it.part == 4 }.sumOf { it.records.size })
        assertEquals(48, fragments.filter { it.part == 5 }.sumOf { it.records.size })
        assertEquals(241, records.size)
        assertEquals(241, records.map { it.id }.toSet().size)
        assertEquals("AOKF-12-001", records.first().id)
        assertEquals("AOKF-5S-011", records.last().id)
        assertTrue(records.all { it.rulePackEffectRaw == "NONE" })
        assertTrue(records.all { it.stateRaw == "PILOT_READY" })
        assertTrue(records.all { it.detailLevelRaw == "DETAILED" })
    }

    @Test
    fun cardDatasetPreservesHistoricalContextWithoutPromotingItToCurrentDiagnosis() {
        val records = resourceNames
            .flatMap { name -> AppleOfficialKnowledgeCardJsonParser.parse(readResource(name)).records }

        val currentWithHistorical = records.single { it.id == "AOKF-12-013" }
        val normalizedHistorical = AppleOfficialKnowledgeCardJsonParser.normalizeSourceStatus(
            currentWithHistorical.sourceStatusRaw
        )
        assertEquals(AppleKnowledgeSourceStatus.CURRENT, normalizedHistorical.primary)
        assertEquals(listOf(AppleKnowledgeSourceStatus.HISTORICAL), normalizedHistorical.contextual)

        val currentWithEndedContext = records.single { it.id == "AOKF-11-008" }
        val normalizedEnded = AppleOfficialKnowledgeCardJsonParser.normalizeSourceStatus(
            currentWithEndedContext.sourceStatusRaw
        )
        assertEquals(AppleKnowledgeSourceStatus.CURRENT, normalizedEnded.primary)
        assertEquals(listOf(AppleKnowledgeSourceStatus.ENDED), normalizedEnded.contextual)

        val endedProgram = records.single { it.id == "AOKF-7-013" }
        assertEquals("ENDED", endedProgram.sourceStatusRaw)
        assertEquals("NONE", endedProgram.rulePackEffectRaw)
    }

    private fun readResource(name: String): String {
        val stream = assertNotNull(javaClass.classLoader.getResourceAsStream(name))
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        val resourceNames = listOf(
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
