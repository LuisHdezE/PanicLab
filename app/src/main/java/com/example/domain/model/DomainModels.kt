package com.example.domain.model

enum class PanicFamily {
    THERMAL_MISSING_SENSOR,
    WATCHDOG_NO_CHECKIN,
    SMC_ASSERTION,
    SMC_BSC_FAILURE,
    I2C,
    AOP_NMI_POWER,
    AOP_BOSCH_CONTROL,
    AOP_OTHER,
    ANS2,
    APPLE_SOC_HOT,
    SEP_ROM_BOOT,
    UNDEFINED_KERNEL_INSTRUCTION,
    DCP_DISPLAY,
    BASEBAND,
    UNKNOWN
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

enum class VerificationStatus {
    VERIFIED,
    WELL_DOCUMENTED,
    COMMUNITY_SUPPORTED,
    CONFLICTING_SOURCE,
    EXPERIMENTAL,
    UNKNOWN
}

data class SuspectedComponent(
    val name: String,
    val role: String // "PRIMARY", "SECONDARY", "ALTERNATIVE", "CANDIDATE", "LOW_CONFIDENCE_CANDIDATE"
)

data class DiagnosisDefinition(
    val label: String,
    val subsystem: String,
    val suspectedComponents: List<SuspectedComponent>,
    val interpretation: String
)

data class RepairFlow(
    val firstChecks: List<String> = emptyList(),
    val knownGoodTest: String? = null,
    val boardLevelNextSteps: List<String> = emptyList(),
    val cautions: List<String> = emptyList()
)

data class DeviceScope(
    val diagnosticProfiles: List<String> = emptyList(),
    val productCodes: List<String> = emptyList()
)

data class DiagnosticRule(
    val id: String,
    val title: String,
    val active: Boolean = true,
    val priority: Int = 100,
    val deviceScope: DeviceScope = DeviceScope(),
    val panicFamilies: List<PanicFamily> = emptyList(),
    val sensorTokens: List<String> = emptyList(),
    val sensorCodesExact: List<String> = emptyList(),
    val requiredTermsAll: List<String> = emptyList(),
    val rawTermsAny: List<String> = emptyList(),
    val diagnosis: DiagnosisDefinition,
    val confidence: ConfidenceLevel,
    val verificationStatus: VerificationStatus,
    val primaryEligible: Boolean = true,
    val exactCodeOnly: Boolean = true,
    val allowBitmaskDecomposition: Boolean = false,
    val repairFlow: RepairFlow = RepairFlow(),
    val sourceIds: List<String> = emptyList(),
    val notes: String? = null,
    val version: String = "1.0.0"
)

data class DeviceModel(
    val productCode: String,
    val marketingName: String,
    val family: String,
    val variant: String,
    val diagnosticProfile: String,
    val releaseYear: Int,
    val sourceIds: List<String> = emptyList()
)

data class DiagnosticEvidence(
    val id: String,
    val type: String, // "SMC_CODE", "MISSING_SENSOR", "PANIC_STRING", "I2C_CHANNEL"
    val title: String,
    val rawValue: String,
    val normalizedValue: String,
    val excerpt: String,
    val lineNumber: Int = -1
)

data class DiagnosisCandidate(
    val ruleId: String,
    val label: String,
    val subsystem: String,
    val suspectedComponents: List<SuspectedComponent>,
    val interpretation: String,
    val confidence: ConfidenceLevel,
    val verificationStatus: VerificationStatus,
    val isPrimary: Boolean,
    val repairFlow: RepairFlow = RepairFlow()
)

data class ParsedMetadata(
    val product: String? = null,
    val osVersion: String? = null,
    val build: String? = null,
    val bugType: String? = null,
    val incidentId: String? = null,
    val crashReporterKey: String? = null,
    val panicInitiator: String? = null,
    val panicString: String? = null,
    val kernel: String? = null,
    val socId: String? = null,
    val timestamp: String? = null,
    val repairStatus: String? = null,
    val rootsInstalled: String? = null
)

data class DiagnosticReport(
    val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceFilename: String? = null,
    val deviceModel: DeviceModel?,
    val productCode: String,
    val osVersion: String,
    val build: String,
    val panicFamilies: List<PanicFamily>,
    val panicStringSummary: String,
    val evidences: List<DiagnosticEvidence>,
    val primaryCandidate: DiagnosisCandidate?,
    val alternativeCandidates: List<DiagnosisCandidate>,
    val confidence: ConfidenceLevel,
    val verificationStatus: VerificationStatus,
    val repairFlow: RepairFlow,
    val knowledgeBaseVersion: String,
    val rawLog: String? = null,
    val rawLogSaved: Boolean = false,
    val reanalyzedAt: Long? = null,
    val previousDiagnosis: String? = null,
    val previousKnowledgeBaseVersion: String? = null
)

enum class RulePackOrigin {
    PANICLAB_OFFICIAL,
    USER_IMPORTED,
    BUNDLED
}

data class RulePackSource(
    val id: String,
    val title: String,
    val publisher: String,
    val url: String,
    val checkedAt: String,
    val trustLevel: String,
    val supports: List<String> = emptyList()
)

data class PanicClassifier(
    val id: String,
    val family: String,
    val priority: Int = 100,
    val anyTerms: List<String> = emptyList(),
    val allTerms: List<String> = emptyList(),
    val regexAny: List<String> = emptyList(),
    val notRegex: List<String> = emptyList(),
    val notes: String? = null
)

data class BitmaskPolicy(
    val diagnosticProfile: String,
    val enabled: Boolean,
    val knownBits: List<String> = emptyList(),
    val exactRulesAlwaysWin: Boolean = true,
    val notes: String? = null
)

data class ParsedRulePack(
    val schemaVersion: Int,
    val knowledgeBaseVersion: String,
    val title: String,
    val generatedAt: String,
    val locale: String,
    val sources: List<RulePackSource>,
    val deviceModels: List<DeviceModel>,
    val panicClassifiers: List<PanicClassifier>,
    val bitmaskPolicies: List<BitmaskPolicy>,
    val diagnosticRules: List<DiagnosticRule>,
    val checksum: String = "",
    val origin: RulePackOrigin = RulePackOrigin.USER_IMPORTED,
    val rawJson: String = ""
)

data class ValidationIssue(
    val type: IssueType,
    val path: String,
    val message: String
) {
    enum class IssueType {
        ERROR,
        WARNING
    }
}

data class RulePackValidationResult(
    val isValid: Boolean,
    val schemaVersion: Int,
    val knowledgeBaseVersion: String,
    val rulesCount: Int,
    val modelsCount: Int,
    val classifiersCount: Int,
    val sourcesCount: Int,
    val bitmaskCount: Int,
    val errors: List<ValidationIssue>,
    val warnings: List<ValidationIssue>,
    val checksum: String
)

data class RuleDiffItem(
    val ruleId: String,
    val title: String,
    val changeType: ChangeType, // ADDED, MODIFIED, REMOVED, DEACTIVATED
    val previousSummary: String? = null,
    val newSummary: String? = null,
    val confidenceChange: String? = null,
    val statusChange: String? = null,
    val details: List<String> = emptyList()
) {
    enum class ChangeType {
        ADDED,
        MODIFIED,
        REMOVED,
        DEACTIVATED,
        UNCHANGED
    }
}

data class RulePackDiffSummary(
    val currentVersion: String,
    val incomingVersion: String,
    val addedRulesCount: Int,
    val modifiedRulesCount: Int,
    val deactivatedRulesCount: Int,
    val removedRulesCount: Int,
    val addedModelsCount: Int,
    val modifiedModelsCount: Int,
    val addedSourcesCount: Int,
    val addedClassifiersCount: Int,
    val ruleDiffs: List<RuleDiffItem>
)
