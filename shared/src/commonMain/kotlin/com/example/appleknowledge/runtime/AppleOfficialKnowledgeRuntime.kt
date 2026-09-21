package com.example.appleknowledge.runtime

import com.example.appleknowledge.data.AppleKnowledgeCardSeedRecord
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCapabilitiesValidator
import com.example.appleknowledge.data.AppleOfficialKnowledgeCardJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeCardValidator
import com.example.appleknowledge.data.AppleOfficialKnowledgeQueryMapper
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceJsonParser
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceReferenceAudit
import com.example.appleknowledge.data.AppleOfficialKnowledgeSourceValidator
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleModelCapability
import com.example.appleknowledge.query.AppleKnowledgeCardView
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceView
import com.example.appleknowledge.query.AppleOfficialKnowledgeCardDetail
import com.example.appleknowledge.query.AppleOfficialKnowledgeQueryRepository
import com.example.appleknowledge.usecase.GetAppleOfficialKnowledgeCardUseCase
import com.example.appleknowledge.usecase.GetAppleOfficialKnowledgeForModelUseCase
import com.example.appleknowledge.usecase.GetAppleOfficialModelCapabilityUseCase

/**
 * Runtime input for the frozen Apple Official Knowledge v1 dataset.
 *
 * Platform layers own resource I/O. COMMON owns parsing, validation, source-reference auditing,
 * model applicability and query semantics.
 */
data class AppleOfficialKnowledgeResourceBundle(
    val capabilitiesJson: String,
    val sourceJsonParts: List<String>,
    val cardJsonParts: List<String>
) {
    init {
        require(capabilitiesJson.isNotBlank()) { "capabilitiesJson must not be blank" }
        require(sourceJsonParts.isNotEmpty()) { "sourceJsonParts must not be empty" }
        require(cardJsonParts.isNotEmpty()) { "cardJsonParts must not be empty" }
    }
}

object AppleOfficialKnowledgeResourceNames {
    const val capabilities = "appleknowledge/apple_official_knowledge_capabilities_v1.json"

    val sources = listOf(
        "appleknowledge/apple_official_knowledge_sources_v1_part1.json",
        "appleknowledge/apple_official_knowledge_sources_v1_part2.json",
        "appleknowledge/apple_official_knowledge_sources_v1_part3.json"
    )

    val cards = listOf(
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

/**
 * Immutable, validated in-memory catalog backing both the I3 repository and native UI adapters.
 */
class AppleOfficialKnowledgeCatalog private constructor(
    val datasetVersion: String,
    val capabilities: List<AppleModelCapability>,
    private val sourcesById: Map<String, AppleKnowledgeSource>,
    private val cardsById: Map<String, AppleKnowledgeCardView>,
    private val cardsByExactModel: Map<String, List<AppleKnowledgeCardView>>
) {
    val exactModels: List<String> = capabilities.flatMap { it.exactModels }

    fun getSourceById(id: String): AppleKnowledgeSource? = sourcesById[id.trim()]

    fun getSourcesByIds(ids: List<String>): List<AppleKnowledgeSource> =
        ids.distinct().mapNotNull(sourcesById::get)

    fun getCardById(id: String): AppleKnowledgeCardView? = cardsById[id.trim()]

    fun getCardsForExactModel(exactModel: String): List<AppleKnowledgeCardView> =
        cardsByExactModel[normalizeKey(exactModel)].orEmpty()

    fun getModelCapability(exactModel: String): AppleModelCapability? =
        capabilities.firstOrNull { it.appliesToExactModel(exactModel) }

    fun getCardDetail(cardId: String): AppleOfficialKnowledgeCardDetail? {
        val card = getCardById(cardId) ?: return null
        val exactIds = buildList {
            addAll(card.primarySource.exactSourceIds)
            card.secondarySources.forEach { addAll(it.exactSourceIds) }
        }.distinct()

        return AppleOfficialKnowledgeCardDetail(
            card = card,
            resolvedExactSources = getSourcesByIds(exactIds).sortedBy { it.id }
        )
    }

    companion object {
        fun create(bundle: AppleOfficialKnowledgeResourceBundle): AppleOfficialKnowledgeCatalog {
            val capabilitySeed = AppleOfficialKnowledgeCapabilitiesJsonParser.parse(bundle.capabilitiesJson)
            val sourceParts = bundle.sourceJsonParts.map(AppleOfficialKnowledgeSourceJsonParser::parse)
            val cardParts = bundle.cardJsonParts.map(AppleOfficialKnowledgeCardJsonParser::parse)

            requireValid("capabilities", AppleOfficialKnowledgeCapabilitiesValidator.validate(capabilitySeed))
            requireValid("sources", AppleOfficialKnowledgeSourceValidator.validate(sourceParts))
            requireValid("cards", AppleOfficialKnowledgeCardValidator.validate(cardParts))

            val sourceRecords = sourceParts.flatMap { it.records }
            val cardRecords = cardParts.flatMap { it.records }

            require(sourceRecords.size == capabilitySeed.manifest.expectedCounts.sources) {
                "AOK source count does not match manifest"
            }
            require(cardRecords.size == capabilitySeed.manifest.expectedCounts.cards) {
                "AOK card count does not match manifest"
            }

            val audit = AppleOfficialKnowledgeSourceReferenceAudit.audit(sourceRecords, cardRecords)
            val projections = AppleOfficialKnowledgeQueryMapper.mapAll(cardRecords, audit)
            val sourceMap = sourceRecords.associate { it.source.id to it.source }
            val cardMap = projections.associateBy { it.id }

            val cardsByModel = capabilitySeed.modelCapabilities
                .flatMap { it.exactModels }
                .associate { exactModel ->
                    normalizeKey(exactModel) to cardRecords.indices
                        .asSequence()
                        .filter { index ->
                            AppleOfficialKnowledgeApplicabilityResolver.appliesToExactModel(
                                card = cardRecords[index],
                                exactModel = exactModel,
                                capabilities = capabilitySeed.modelCapabilities
                            )
                        }
                        .map { projections[it] }
                        .sortedWith(compareBy({ it.category.name }, { it.id }))
                        .toList()
                }

            return AppleOfficialKnowledgeCatalog(
                datasetVersion = capabilitySeed.manifest.datasetVersion,
                capabilities = capabilitySeed.modelCapabilities,
                sourcesById = sourceMap,
                cardsById = cardMap,
                cardsByExactModel = cardsByModel
            )
        }

        private fun requireValid(label: String, validation: com.example.appleknowledge.data.AppleKnowledgeDatasetValidationResult) {
            require(validation.isValid) {
                val messages = validation.errors.joinToString("; ") { "${it.path}: ${it.message}" }
                "Invalid AOK $label dataset: $messages"
            }
        }

        private fun normalizeKey(value: String): String =
            value.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}

/**
 * Converts the governed display scope into exact-model applicability without broadening special
 * cases. Explicit slash/comma lists only select named variants; scopes containing `family` expand
 * to the corresponding audited family. The iPhone 5c storage variant is the only storage-only
 * alias represented as a separate exact model in v1.
 */
object AppleOfficialKnowledgeApplicabilityResolver {
    private val splitRegex = Regex("\\s*(?:/|\\+|;|,|\\bvs\\b)\\s*", RegexOption.IGNORE_CASE)

    fun appliesToExactModel(
        card: AppleKnowledgeCardSeedRecord,
        exactModel: String,
        capabilities: List<AppleModelCapability>
    ): Boolean = resolveExactModels(card.modelScopeRaw, capabilities)
        .any { it.equals(exactModel.trim(), ignoreCase = true) }

    fun resolveExactModels(
        modelScopeRaw: String,
        capabilities: List<AppleModelCapability>
    ): List<String> {
        val normalizedScope = normalize(modelScopeRaw)
        val allModels = capabilities.flatMap { it.exactModels }.distinct()
        val resolved = linkedSetOf<String>()

        modelScopeRaw.split(splitRegex)
            .map(::normalize)
            .filter { it.isNotEmpty() }
            .forEach { segment ->
                val matches = allModels.mapNotNull { model ->
                    aliasesFor(model)
                        .filter { alias -> segment == alias || segment.startsWith("$alias ") }
                        .maxByOrNull(String::length)
                        ?.let { alias -> Triple(alias.length, model, alias) }
                }
                val longest = matches.maxOfOrNull { it.first }
                if (longest != null) {
                    matches.filter { it.first == longest }.forEach { resolved += it.second }
                }
            }

        capabilities.forEach { capability ->
            val familyToken = normalize(capability.family).removePrefix("iphone ")
            val isBroadFamilyScope = normalizedScope.contains("$familyToken family") ||
                normalizedScope.contains("$familyToken familia")
            if (isBroadFamilyScope) {
                resolved += capability.exactModels
            }
        }

        capabilities.firstOrNull { capability ->
            normalize(capability.family) == normalizedScope &&
                capability.exactModels.drop(1).all { model -> normalize(model).contains(" gb") }
        }?.let { resolved += it.exactModels }

        return allModels.filter { model -> resolved.any { it.equals(model, ignoreCase = true) } }
    }

    private fun aliasesFor(exactModel: String): List<String> {
        val full = normalize(exactModel)
        val suffix = full.removePrefix("iphone ")
        return listOf(full, suffix).distinct().sortedByDescending(String::length)
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}

class InMemoryAppleOfficialKnowledgeQueryRepository(
    private val catalog: AppleOfficialKnowledgeCatalog
) : AppleOfficialKnowledgeQueryRepository {
    override suspend fun getSourceById(id: String): AppleKnowledgeSource? = catalog.getSourceById(id)

    override suspend fun getSourcesByIds(ids: List<String>): List<AppleKnowledgeSource> =
        catalog.getSourcesByIds(ids)

    override suspend fun getCardById(id: String): AppleKnowledgeCardView? = catalog.getCardById(id)

    override suspend fun getCardsForExactModel(exactModel: String): List<AppleKnowledgeCardView> =
        catalog.getCardsForExactModel(exactModel)

    override suspend fun getModelCapability(exactModel: String): AppleModelCapability? =
        catalog.getModelCapability(exactModel)
}

class AppleOfficialKnowledgeRuntime private constructor(
    val catalog: AppleOfficialKnowledgeCatalog,
    val repository: AppleOfficialKnowledgeQueryRepository,
    val getForModel: GetAppleOfficialKnowledgeForModelUseCase,
    val getCard: GetAppleOfficialKnowledgeCardUseCase,
    val getCapability: GetAppleOfficialModelCapabilityUseCase
) {
    companion object {
        fun create(bundle: AppleOfficialKnowledgeResourceBundle): AppleOfficialKnowledgeRuntime {
            val catalog = AppleOfficialKnowledgeCatalog.create(bundle)
            val repository = InMemoryAppleOfficialKnowledgeQueryRepository(catalog)
            return AppleOfficialKnowledgeRuntime(
                catalog = catalog,
                repository = repository,
                getForModel = GetAppleOfficialKnowledgeForModelUseCase(repository),
                getCard = GetAppleOfficialKnowledgeCardUseCase(repository),
                getCapability = GetAppleOfficialModelCapabilityUseCase(repository)
            )
        }
    }
}

data class NativeAppleKnowledgeModelSummary(
    val exactModel: String,
    val family: String,
    val releaseYears: String,
    val publicRepairManual: String,
    val diagnosticsSsr: String,
    val recoveryDiagnosticsMode: String,
    val repairAssistant: String,
    val partsServiceHistory: String,
    val troubleshooting: String,
    val primaryOfficialUrl: String,
    val verifiedAt: String,
    val notes: String
)

data class NativeAppleKnowledgeSourceReference(
    val role: String,
    val url: String,
    val resolution: String,
    val exactSourceIds: String,
    val candidateSourceIds: String
)

data class NativeAppleKnowledgeCardSummary(
    val id: String,
    val category: String,
    val subcategory: String,
    val modelScope: String,
    val symptoms: String,
    val sourceStatus: String,
    val hasUnresolvedSources: Boolean
)

data class NativeAppleKnowledgeCardDetail(
    val id: String,
    val category: String,
    val subcategory: String,
    val modelScope: String,
    val symptoms: String,
    val quickChecks: String,
    val appleDiagnostics: String,
    val inspectionOrDiscard: String,
    val appleAction: String,
    val sourceStatus: String,
    val contextualSourceStatuses: String,
    val panicLabCorrelation: String,
    val applicabilityNotes: String,
    val verifiedAt: String,
    val sourceReferences: List<NativeAppleKnowledgeSourceReference>
)

/**
 * Synchronous Swift-friendly adapter over the same immutable catalog used by the I3 repository.
 * It is presentation-only and has no diagnostic or Rule Pack mutation surface.
 */
class NativeAppleOfficialKnowledgeFacade(
    capabilitiesJson: String,
    sourceJsonParts: List<String>,
    cardJsonParts: List<String>
) {
    private val catalog = AppleOfficialKnowledgeCatalog.create(
        AppleOfficialKnowledgeResourceBundle(
            capabilitiesJson = capabilitiesJson,
            sourceJsonParts = sourceJsonParts,
            cardJsonParts = cardJsonParts
        )
    )

    fun exactModels(): List<String> = catalog.exactModels

    fun modelSummary(exactModel: String): NativeAppleKnowledgeModelSummary? =
        catalog.getModelCapability(exactModel)?.let { capability ->
            NativeAppleKnowledgeModelSummary(
                exactModel = exactModel.trim(),
                family = capability.family,
                releaseYears = capability.releaseYears,
                publicRepairManual = capability.publicRepairManual.availability.name,
                diagnosticsSsr = capability.diagnosticsSsr.availability.name,
                recoveryDiagnosticsMode = capability.recoveryDiagnosticsMode.availability.name,
                repairAssistant = capability.repairAssistant.availability.name,
                partsServiceHistory = capability.partsServiceHistoryCapabilities.joinToString(" · ").ifBlank { "NO" },
                troubleshooting = capability.troubleshooting.availability.name,
                primaryOfficialUrl = capability.primaryOfficialUrl,
                verifiedAt = capability.verifiedAt,
                notes = capability.notes.orEmpty()
            )
        }

    fun cards(exactModel: String, categoryName: String? = null): List<NativeAppleKnowledgeCardSummary> {
        val category = categoryName?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { AppleKnowledgeCategory.valueOf(raw) }.getOrNull()
        }
        return catalog.getCardsForExactModel(exactModel)
            .asSequence()
            .filter { category == null || it.category == category }
            .map { card ->
                NativeAppleKnowledgeCardSummary(
                    id = card.id,
                    category = card.category.name,
                    subcategory = card.subcategory,
                    modelScope = card.modelScope,
                    symptoms = card.symptoms,
                    sourceStatus = card.sourceStatus.name,
                    hasUnresolvedSources = card.allReferences().any { it.resolution.name != "EXACT_SINGLE" }
                )
            }
            .toList()
    }

    fun cardDetail(cardId: String): NativeAppleKnowledgeCardDetail? =
        catalog.getCardDetail(cardId)?.card?.let { card ->
            NativeAppleKnowledgeCardDetail(
                id = card.id,
                category = card.category.name,
                subcategory = card.subcategory,
                modelScope = card.modelScope,
                symptoms = card.symptoms,
                quickChecks = card.quickChecks,
                appleDiagnostics = card.appleDiagnostics,
                inspectionOrDiscard = card.inspectionOrDiscard,
                appleAction = card.appleAction,
                sourceStatus = card.sourceStatus.name,
                contextualSourceStatuses = card.contextualSourceStatuses.joinToString(" · ") { it.name },
                panicLabCorrelation = card.panicLabCorrelation,
                applicabilityNotes = card.applicabilityNotes,
                verifiedAt = card.verifiedAt,
                sourceReferences = card.allReferences().map(::toNativeReference)
            )
        }

    private fun toNativeReference(reference: AppleKnowledgeSourceReferenceView) =
        NativeAppleKnowledgeSourceReference(
            role = reference.role.name,
            url = reference.url,
            resolution = reference.resolution.name,
            exactSourceIds = reference.exactSourceIds.joinToString(", "),
            candidateSourceIds = reference.candidateSourceIds.joinToString(", ")
        )

    private fun AppleKnowledgeCardView.allReferences(): List<AppleKnowledgeSourceReferenceView> =
        listOf(primarySource) + secondarySources
}
