package com.example.appleknowledge

import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleKnowledgeAuthority
import com.example.appleknowledge.model.AppleKnowledgeCardState
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.model.AppleKnowledgeDetailLevel
import com.example.appleknowledge.model.AppleKnowledgeRulePackEffect
import com.example.appleknowledge.model.AppleKnowledgeSource
import com.example.appleknowledge.model.AppleKnowledgeSourceStatus
import com.example.appleknowledge.model.AppleKnowledgeSourceType
import com.example.appleknowledge.model.AppleModelCapability
import com.example.appleknowledge.query.AppleKnowledgeCardView
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceRole
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceView
import com.example.appleknowledge.query.AppleOfficialKnowledgeQueryRepository
import com.example.appleknowledge.usecase.GetAppleOfficialKnowledgeCardUseCase
import com.example.appleknowledge.usecase.GetAppleOfficialKnowledgeForModelUseCase
import com.example.appleknowledge.usecase.GetAppleOfficialModelCapabilityUseCase
import com.example.appleknowledge.usecase.hasUnresolvedSourceReferences
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppleOfficialKnowledgeUseCasesTest {

    @Test
    fun modelUseCaseReturnsCapabilityAndDeterministicallyFilteredCards() {
        val battery = card(
            id = "AOKF-12-002",
            category = AppleKnowledgeCategory.BATTERY_CHARGING_POWER,
            primary = exactReference("AOK-001")
        )
        val sound = card(
            id = "AOKF-12-013",
            category = AppleKnowledgeCategory.SOUND,
            primary = exactReference("AOK-002")
        )
        val repository = FakeQueryRepository(cards = listOf(sound, battery))
        val useCase = GetAppleOfficialKnowledgeForModelUseCase(repository)

        val all = runSuspend { useCase.execute("  iPhone 12  ") }
        assertNotNull(all)
        assertEquals("iPhone 12", all.exactModel)
        assertEquals(listOf("AOKF-12-002", "AOKF-12-013"), all.cards.map { it.id })

        val soundOnly = runSuspend {
            useCase.execute("iPhone 12", AppleKnowledgeCategory.SOUND)
        }
        assertNotNull(soundOnly)
        assertEquals(listOf("AOKF-12-013"), soundOnly.cards.map { it.id })
    }

    @Test
    fun cardUseCaseResolvesOnlyExactIdsAndLeavesCandidatesUnresolved() {
        val primary = AppleKnowledgeSourceReferenceView(
            role = AppleKnowledgeSourceReferenceRole.PRIMARY,
            url = "https://support.apple.com/es-es/108044",
            resolution = AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS,
            exactSourceIds = listOf("AOK-001", "AOK-002")
        )
        val candidate = AppleKnowledgeSourceReferenceView(
            role = AppleKnowledgeSourceReferenceRole.SECONDARY,
            url = "https://support.apple.com/es-es/101965",
            resolution = AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE,
            candidateSourceIds = listOf("AOK-003")
        )
        val exactSecondary = AppleKnowledgeSourceReferenceView(
            role = AppleKnowledgeSourceReferenceRole.SECONDARY,
            url = "https://support.apple.com/es-es/100464",
            resolution = AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE,
            exactSourceIds = listOf("AOK-004")
        )
        val card = card(
            id = "AOKF-12-001",
            category = AppleKnowledgeCategory.BATTERY_CHARGING_POWER,
            primary = primary,
            secondary = listOf(candidate, exactSecondary)
        )
        val repository = FakeQueryRepository(cards = listOf(card))
        val useCase = GetAppleOfficialKnowledgeCardUseCase(repository)

        val detail = runSuspend { useCase.execute("AOKF-12-001") }
        assertNotNull(detail)
        assertEquals(listOf("AOK-001", "AOK-002", "AOK-004"), detail.resolvedExactSources.map { it.id })
        assertFalse(detail.resolvedExactSources.any { it.id == "AOK-003" })
        assertTrue(detail.hasUnresolvedSourceReferences())
    }

    @Test
    fun blankOrUnauditedModelReturnsNullWithoutInventingCoverage() {
        val repository = FakeQueryRepository(cards = emptyList())
        val modelUseCase = GetAppleOfficialKnowledgeForModelUseCase(repository)
        val capabilityUseCase = GetAppleOfficialModelCapabilityUseCase(repository)

        assertNull(runSuspend { modelUseCase.execute("   ") })
        assertNull(runSuspend { modelUseCase.execute("iPhone 4S") })
        assertNull(runSuspend { capabilityUseCase.execute("iPhone 4S") })
    }

    private fun exactReference(id: String) = AppleKnowledgeSourceReferenceView(
        role = AppleKnowledgeSourceReferenceRole.PRIMARY,
        url = "https://support.apple.com/es-es/${id.removePrefix("AOK-")}",
        resolution = AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE,
        exactSourceIds = listOf(id)
    )

    private fun card(
        id: String,
        category: AppleKnowledgeCategory,
        primary: AppleKnowledgeSourceReferenceView,
        secondary: List<AppleKnowledgeSourceReferenceView> = emptyList()
    ) = AppleKnowledgeCardView(
        id = id,
        modelScope = "iPhone 12",
        category = category,
        subcategory = "Test",
        symptoms = "Symptoms",
        quickChecks = "Checks",
        appleDiagnostics = "Diagnostics",
        inspectionOrDiscard = "Inspection",
        appleAction = "Action",
        primarySource = primary,
        secondarySources = secondary,
        sourceStatus = AppleKnowledgeSourceStatus.CURRENT,
        contextualSourceStatuses = emptyList(),
        detailLevel = AppleKnowledgeDetailLevel.DETAILED,
        panicLabCorrelation = "Context only",
        rulePackEffect = AppleKnowledgeRulePackEffect.NONE,
        state = AppleKnowledgeCardState.PILOT_READY,
        verifiedAt = "2026-09-21",
        applicabilityNotes = ""
    )

    private class FakeQueryRepository(
        private val cards: List<AppleKnowledgeCardView>
    ) : AppleOfficialKnowledgeQueryRepository {
        private val capability = AppleModelCapability(
            family = "iPhone 12",
            exactModels = listOf("iPhone 12", "iPhone 12 mini", "iPhone 12 Pro", "iPhone 12 Pro Max"),
            releaseYears = "2020",
            publicRepairManual = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            diagnosticsSsr = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            recoveryDiagnosticsMode = AppleCapability(AppleCapabilityAvailability.NOT_SUPPORTED),
            repairAssistant = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            partsServiceHistoryCapabilities = listOf("Battery", "Display"),
            troubleshooting = AppleCapability(AppleCapabilityAvailability.SUPPORTED),
            historicalProgramSourceIds = listOf("AOK-002"),
            primaryOfficialUrl = "https://support.apple.com/en-gb/100464",
            verifiedAt = "2026-09-21"
        )

        private val sources = (1..4).associate { index ->
            val id = "AOK-${index.toString().padStart(3, '0')}"
            id to AppleKnowledgeSource(
                id = id,
                sourceType = AppleKnowledgeSourceType.TROUBLESHOOTING,
                topic = "Test",
                applicableModels = listOf("iPhone 12"),
                title = "Source $id",
                officialUrl = "https://support.apple.com/es-es/$index",
                sourceStatus = AppleKnowledgeSourceStatus.CURRENT,
                authority = AppleKnowledgeAuthority.APPLE_OFFICIAL,
                verifiedAt = "2026-09-21"
            )
        }

        override suspend fun getSourceById(id: String): AppleKnowledgeSource? = sources[id]

        override suspend fun getSourcesByIds(ids: List<String>): List<AppleKnowledgeSource> =
            ids.distinct().mapNotNull(sources::get)

        override suspend fun getCardById(id: String): AppleKnowledgeCardView? =
            cards.firstOrNull { it.id == id }

        override suspend fun getCardsForExactModel(exactModel: String): List<AppleKnowledgeCardView> =
            if (capability.appliesToExactModel(exactModel)) cards else emptyList()

        override suspend fun getModelCapability(exactModel: String): AppleModelCapability? =
            capability.takeIf { it.appliesToExactModel(exactModel) }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completed = false
        var value: Any? = null
        var failure: Throwable? = null

        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                result.fold(
                    onSuccess = { value = it },
                    onFailure = { failure = it }
                )
                completed = true
            }
        })

        check(completed) { "Test coroutine unexpectedly suspended" }
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
