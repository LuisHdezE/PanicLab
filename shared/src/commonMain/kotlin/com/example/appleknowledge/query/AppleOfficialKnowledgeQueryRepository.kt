package com.example.appleknowledge.query

import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleModelCapability

/**
 * Read-only query boundary for Apple Official Knowledge v1.
 *
 * Unlike the strict normalized domain repository, this query boundary can preserve governed raw
 * source URLs whose mapping to AOK source ids is ambiguous or unavailable. It deliberately exposes
 * no mutation operation and no deterministic-diagnosis operation.
 */
interface AppleOfficialKnowledgeQueryRepository {
    suspend fun getSourceById(id: String): AppleKnowledgeSource?

    suspend fun getSourcesByIds(ids: List<String>): List<AppleKnowledgeSource>

    suspend fun getCardById(id: String): AppleKnowledgeCardView?

    suspend fun getCardsForExactModel(exactModel: String): List<AppleKnowledgeCardView>

    suspend fun getModelCapability(exactModel: String): AppleModelCapability?
}
