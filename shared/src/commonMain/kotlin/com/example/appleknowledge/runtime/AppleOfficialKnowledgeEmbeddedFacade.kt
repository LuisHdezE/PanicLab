package com.example.appleknowledge.runtime

/**
 * Creates the validated AOK runtime from build-generated Kotlin literals whose only input is the
 * governed `shared/src/commonMain/resources/appleknowledge` dataset.
 */
object AppleOfficialKnowledgeRuntimeFactory {
    fun embedded(): AppleOfficialKnowledgeRuntime =
        AppleOfficialKnowledgeRuntime.create(AppleOfficialKnowledgeEmbeddedResources.bundle())
}

/**
 * Swift-friendly, no-I/O facade over the embedded AOK dataset.
 *
 * The generated literals live under build/ and are never a second source of truth. This facade is
 * presentation-only and cannot mutate or influence deterministic diagnosis.
 */
class NativeEmbeddedAppleOfficialKnowledgeFacade {
    private val delegate = AppleOfficialKnowledgeEmbeddedResources.bundle().let { bundle ->
        NativeAppleOfficialKnowledgeFacade(
            capabilitiesJson = bundle.capabilitiesJson,
            sourceJsonParts = bundle.sourceJsonParts,
            cardJsonParts = bundle.cardJsonParts
        )
    }

    fun exactModels(): List<String> = delegate.exactModels()

    fun modelSummary(exactModel: String): NativeAppleKnowledgeModelSummary? =
        delegate.modelSummary(exactModel)

    fun cards(
        exactModel: String,
        categoryName: String? = null
    ): List<NativeAppleKnowledgeCardSummary> = delegate.cards(exactModel, categoryName)

    fun cardDetail(cardId: String): NativeAppleKnowledgeCardDetail? =
        delegate.cardDetail(cardId)
}
