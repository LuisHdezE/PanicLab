package com.example.appleknowledge.model

enum class AppleKnowledgeSourceStatus {
    CURRENT,
    HISTORICAL,
    ENDED,
    ARCHIVE_VERIFICATION_REQUIRED
}

enum class AppleKnowledgeAuthority {
    APPLE_OFFICIAL,
    APPLE_OFFICIAL_STATUS
}

enum class AppleKnowledgeRulePackEffect {
    NONE
}

enum class AppleKnowledgeDetailLevel {
    DETAILED
}

enum class AppleKnowledgeCardState {
    PILOT_READY
}

enum class AppleCapabilityAvailability {
    SUPPORTED,
    NOT_SUPPORTED,
    NOT_FOUND_PUBLICLY,
    MODEL_OR_REGION_DEPENDENT
}

enum class AppleKnowledgeSourceType {
    MANUAL_INDEX,
    MANUALS_INDEX,
    REPAIR_MANUAL,
    TROUBLESHOOTING,
    DEVICE_HISTORY,
    PARTS_SERVICE_HISTORY,
    DIAGNOSTICS_RECOVERY_ASSISTANT,
    DIAGNOSTICS,
    DIAGNOSTICS_SSR,
    POST_REPAIR,
    REPAIR_ASSISTANT,
    SERVICE_PROGRAM,
    SERVICE_PROGRAM_STATUS,
    DEVICE_PART_SUPPORT,
    BATTERY_RECALIBRATION,
    BATTERY_AUTHENTICITY,
    TECH_SPECS,
    MODEL_IDENTIFICATION,
    OTHER
}

enum class AppleKnowledgeCategory {
    BATTERY_CHARGING_POWER,
    CAMERA,
    DISPLAY,
    DISPLAY_SENSORS,
    MECHANICAL,
    SOUND,
    REPAIR_ECOSYSTEM,
    CONNECTIVITY_POWER,
    MODEL_GUARDRAILS,
    REPAIR_HISTORY,
    KNOWN_ISSUE,
    BIOMETRICS,
    CONNECTIVITY,
    HISTORICAL_SERVICE_PROGRAM,
    BUTTONS,
    OTHER
}

data class AppleModelScope(
    val displayScope: String,
    val exactModels: List<String>
) {
    init {
        require(displayScope.isNotBlank()) { "displayScope must not be blank" }
        require(exactModels.isNotEmpty()) { "exactModels must not be empty" }
        require(exactModels.all { it.isNotBlank() }) { "exactModels must not contain blanks" }
    }

    fun appliesToExactModel(exactModel: String): Boolean {
        val candidate = exactModel.trim()
        if (candidate.isEmpty()) return false
        return exactModels.any { it.equals(candidate, ignoreCase = true) }
    }
}

data class AppleKnowledgeSource(
    val id: String,
    val sourceType: AppleKnowledgeSourceType,
    val topic: String,
    val applicableModels: List<String>,
    val title: String,
    val officialUrl: String,
    val sourceStatus: AppleKnowledgeSourceStatus,
    val authority: AppleKnowledgeAuthority,
    val verifiedAt: String,
    val notes: String? = null
) {
    init {
        require(id.startsWith("AOK-")) { "Apple knowledge source id must start with AOK-" }
        require(topic.isNotBlank()) { "topic must not be blank" }
        require(applicableModels.isNotEmpty()) { "applicableModels must not be empty" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(officialUrl.startsWith("https://")) { "officialUrl must use https" }
        require(verifiedAt.isNotBlank()) { "verifiedAt must not be blank" }
    }
}

data class AppleKnowledgeCard(
    val id: String,
    val modelScope: AppleModelScope,
    val category: AppleKnowledgeCategory,
    val subcategory: String,
    val symptoms: String,
    val quickChecks: String,
    val appleDiagnostics: String,
    val inspectionOrDiscard: String,
    val appleAction: String,
    val primarySourceId: String,
    val secondarySourceIds: List<String> = emptyList(),
    val sourceStatus: AppleKnowledgeSourceStatus,
    val contextualSourceStatuses: List<AppleKnowledgeSourceStatus> = emptyList(),
    val detailLevel: AppleKnowledgeDetailLevel = AppleKnowledgeDetailLevel.DETAILED,
    val panicLabCorrelation: String,
    val rulePackEffect: AppleKnowledgeRulePackEffect = AppleKnowledgeRulePackEffect.NONE,
    val state: AppleKnowledgeCardState = AppleKnowledgeCardState.PILOT_READY,
    val verifiedAt: String,
    val applicabilityNotes: String? = null
) {
    init {
        require(id.startsWith("AOKF-")) { "Apple knowledge card id must start with AOKF-" }
        require(subcategory.isNotBlank()) { "subcategory must not be blank" }
        require(primarySourceId.startsWith("AOK-")) { "primarySourceId must reference an AOK source" }
        require(secondarySourceIds.all { it.startsWith("AOK-") }) {
            "secondarySourceIds must reference AOK sources"
        }
        require(verifiedAt.isNotBlank()) { "verifiedAt must not be blank" }
    }
}

data class AppleCapability(
    val availability: AppleCapabilityAvailability,
    val notes: String? = null
)

data class AppleModelCapability(
    val family: String,
    val exactModels: List<String>,
    val releaseYears: String,
    val publicRepairManual: AppleCapability,
    val diagnosticsSsr: AppleCapability,
    val recoveryDiagnosticsMode: AppleCapability,
    val repairAssistant: AppleCapability,
    val partsServiceHistoryCapabilities: List<String> = emptyList(),
    val troubleshooting: AppleCapability,
    val historicalProgramSourceIds: List<String> = emptyList(),
    val primaryOfficialUrl: String,
    val verifiedAt: String,
    val notes: String? = null
) {
    init {
        require(family.isNotBlank()) { "family must not be blank" }
        require(exactModels.isNotEmpty()) { "exactModels must not be empty" }
        require(exactModels.all { it.isNotBlank() }) { "exactModels must not contain blanks" }
        require(historicalProgramSourceIds.all { it.startsWith("AOK-") }) {
            "historicalProgramSourceIds must reference AOK sources"
        }
        require(primaryOfficialUrl.startsWith("https://")) { "primaryOfficialUrl must use https" }
        require(verifiedAt.isNotBlank()) { "verifiedAt must not be blank" }
    }

    fun appliesToExactModel(exactModel: String): Boolean {
        val candidate = exactModel.trim()
        if (candidate.isEmpty()) return false
        return exactModels.any { it.equals(candidate, ignoreCase = true) }
    }
}
