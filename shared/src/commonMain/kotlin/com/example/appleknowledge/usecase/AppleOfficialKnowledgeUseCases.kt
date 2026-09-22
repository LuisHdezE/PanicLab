package com.example.appleknowledge.usecase

import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleModelCapability
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import com.example.appleknowledge.query.AppleOfficialKnowledgeCardDetail
import com.example.appleknowledge.query.AppleOfficialKnowledgeModelView
import com.example.appleknowledge.query.AppleOfficialKnowledgeQueryRepository

class GetAppleOfficialKnowledgeForModelUseCase(
    private val repository: AppleOfficialKnowledgeQueryRepository
) {
    suspend fun execute(
        exactModel: String,
        category: AppleKnowledgeCategory? = null
    ): AppleOfficialKnowledgeModelView? {
        val normalizedModel = exactModel.trim()
        if (normalizedModel.isEmpty()) return null

        val capability = repository.getModelCapability(normalizedModel) ?: return null
        val cards = repository.getCardsForExactModel(normalizedModel)
            .asSequence()
            .filter { category == null || it.category == category }
            .sortedWith(compareBy({ it.category.name }, { it.id }))
            .toList()

        return AppleOfficialKnowledgeModelView(
            exactModel = normalizedModel,
            capability = capability,
            cards = cards
        )
    }
}

class GetAppleOfficialKnowledgeCardUseCase(
    private val repository: AppleOfficialKnowledgeQueryRepository
) {
    suspend fun execute(cardId: String): AppleOfficialKnowledgeCardDetail? {
        val normalizedId = cardId.trim()
        if (normalizedId.isEmpty()) return null

        val card = repository.getCardById(normalizedId) ?: return null
        val exactIds = buildList {
            addAll(card.primarySource.exactSourceIds)
            card.secondarySources.forEach { reference ->
                addAll(reference.exactSourceIds)
            }
        }.distinct()

        val resolvedSources = repository.getSourcesByIds(exactIds)
            .sortedBy { it.id }

        return AppleOfficialKnowledgeCardDetail(
            card = card,
            resolvedExactSources = resolvedSources
        )
    }
}

class GetAppleOfficialModelCapabilityUseCase(
    private val repository: AppleOfficialKnowledgeQueryRepository
) {
    suspend fun execute(exactModel: String): AppleModelCapability? {
        val normalizedModel = exactModel.trim()
        if (normalizedModel.isEmpty()) return null
        return repository.getModelCapability(normalizedModel)
    }
}

/**
 * Returns true when a card still contains at least one source reference that must be presented as
 * unresolved context rather than silently normalized to an AOK id.
 */
fun AppleOfficialKnowledgeCardDetail.hasUnresolvedSourceReferences(): Boolean {
    val references = listOf(card.primarySource) + card.secondarySources
    return references.any { reference ->
        reference.resolution == AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS ||
            reference.resolution == AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE ||
            reference.resolution == AppleKnowledgeSourceReferenceResolution.UNMAPPED
    }
}
