package com.example.appleknowledge.repository

import com.example.appleknowledge.model.AppleKnowledgeCard
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleModelCapability

/**
 * Read-only boundary for Apple Official Knowledge.
 *
 * Implementations may load a bundled dataset, database, or another curated source, but this
 * contract deliberately exposes no mutation operations. Apple Official Knowledge is informational
 * context and must remain isolated from PanicLab's deterministic Rule Pack.
 */
interface AppleOfficialKnowledgeRepository {
    suspend fun getSourceById(id: String): AppleKnowledgeSource?

    suspend fun getSourcesByIds(ids: List<String>): List<AppleKnowledgeSource>

    suspend fun getCardById(id: String): AppleKnowledgeCard?

    suspend fun getCardsForExactModel(exactModel: String): List<AppleKnowledgeCard>

    suspend fun getModelCapability(exactModel: String): AppleModelCapability?
}
