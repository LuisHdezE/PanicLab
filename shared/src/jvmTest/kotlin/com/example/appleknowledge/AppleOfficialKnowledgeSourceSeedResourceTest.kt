package com.example.appleknowledge

import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceValidator
import com.example.appleknowledge.model.AppleKnowledgeAuthority
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleKnowledgeSourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeSourceSeedResourceTest {

    @Test
    fun frozenV1SourceCatalogParsesAndValidatesExactlyNinetyOneSources() {
        val parts = (1..3).map { part ->
            AppleOfficialKnowledgeSourceJsonParser.parse(readPart(part))
        }
        val validation = AppleOfficialKnowledgeSourceValidator.validate(parts)
        val records = parts.flatMap { it.records }

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(listOf(1, 2, 3), parts.map { it.part })
        assertEquals(listOf(30, 31, 30), parts.map { it.records.size })
        assertEquals(91, records.size)
        assertEquals(91, records.map { it.source.id }.toSet().size)
        assertTrue(records.any { it.source.id == "AOK-001" })
        assertTrue(records.any { it.source.id == "AOK-098" })
    }

    @Test
    fun sourceCatalogPreservesHistoricalAuthorityAndToolBoundaries() {
        val records = (1..3)
            .flatMap { part -> AppleOfficialKnowledgeSourceJsonParser.parse(readPart(part)).records }

        val diagnosticsSsr = records.single { it.source.id == "AOK-031" }.source
        assertEquals(AppleKnowledgeSourceType.DIAGNOSTICS_SSR, diagnosticsSsr.sourceType)
        assertEquals(AppleKnowledgeSourceStatus.CURRENT, diagnosticsSsr.sourceStatus)
        assertEquals(AppleKnowledgeAuthority.APPLE_OFFICIAL, diagnosticsSsr.authority)

        val iphone7Program = records.single { it.source.id == "AOK-073" }.source
        assertEquals(AppleKnowledgeSourceStatus.ENDED, iphone7Program.sourceStatus)
        assertEquals(AppleKnowledgeAuthority.APPLE_OFFICIAL_STATUS, iphone7Program.authority)

        val iphone6PlusProgram = records.single { it.source.id == "AOK-079" }.source
        assertEquals(AppleKnowledgeSourceStatus.HISTORICAL, iphone6PlusProgram.sourceStatus)
        assertEquals(AppleKnowledgeAuthority.APPLE_OFFICIAL, iphone6PlusProgram.authority)

        val repairAssistant17 = records.single { it.source.id == "AOK-058" }.source
        assertEquals(AppleKnowledgeSourceType.REPAIR_ASSISTANT, repairAssistant17.sourceType)
    }

    private fun readPart(part: Int): String {
        val stream = assertNotNull(
            javaClass.classLoader.getResourceAsStream(
                "appleknowledge/apple_official_knowledge_sources_v1_part$part.json"
            )
        )
        return stream.bufferedReader().use { it.readText() }
    }
}