package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "device_models")
data class DeviceModelEntity(
    @PrimaryKey val productCode: String,
    val marketingName: String,
    val family: String,
    val variant: String,
    val diagnosticProfile: String,
    val releaseYear: Int,
    val sourceIdsJson: String = "[]",
    val notes: String? = null
)

@Entity(tableName = "diagnostic_rules")
data class DiagnosticRuleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val active: Boolean = true,
    val priority: Int = 100,
    val diagnosticProfilesJson: String = "[]",
    val productCodesJson: String = "[]",
    val panicFamiliesJson: String = "[]",
    val sensorTokensJson: String = "[]",
    val sensorCodesExactJson: String = "[]",
    val requiredTermsJson: String = "[]",
    val rawTermsJson: String = "[]",
    val label: String,
    val subsystem: String,
    val suspectedComponentsJson: String = "[]",
    val interpretation: String,
    val confidence: String,
    val verificationStatus: String,
    val primaryEligible: Boolean = true,
    val exactCodeOnly: Boolean = true,
    val allowBitmaskDecomposition: Boolean = false,
    val firstChecksJson: String = "[]",
    val knownGoodTest: String? = null,
    val boardLevelNextStepsJson: String = "[]",
    val cautionsJson: String = "[]",
    val sourceIdsJson: String = "[]",
    val notes: String? = null,
    val version: String = "1.0.0"
)

@Entity(tableName = "diagnostic_sessions")
data class DiagnosticSessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val sourceFilename: String?,
    val deviceProductCode: String,
    val deviceName: String,
    val osVersion: String,
    val build: String,
    val panicFamiliesJson: String,
    val panicStringSummary: String,
    val primaryRuleId: String?,
    val primaryDiagnosis: String,
    val confidence: String,
    val verificationStatus: String,
    val knowledgeBaseVersion: String,
    val appliedRuleIdsJson: String = "[]",
    val repairFlowJson: String = "{}",
    val rawLog: String? = null,
    val rawLogSaved: Boolean = false,
    val reanalyzedAt: Long? = null,
    val previousDiagnosis: String? = null,
    val previousKnowledgeBaseVersion: String? = null
)

@Entity(
    tableName = "diagnostic_evidences",
    foreignKeys = [
        ForeignKey(
            entity = DiagnosticSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class DiagnosticEvidenceEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val type: String,
    val title: String,
    val rawValue: String,
    val normalizedValue: String,
    val excerpt: String,
    val lineNumber: Int = -1
)

@Entity(
    tableName = "diagnosis_candidates",
    foreignKeys = [
        ForeignKey(
            entity = DiagnosticSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class DiagnosisCandidateEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ruleId: String,
    val label: String,
    val subsystem: String,
    val suspectedComponentsJson: String,
    val interpretation: String,
    val confidence: String,
    val verificationStatus: String,
    val isPrimary: Boolean,
    val repairFlowJson: String = "{}"
)

@Entity(tableName = "rule_packs")
data class RulePackEntity(
    @PrimaryKey val version: String,
    val title: String,
    val generatedAt: String,
    val schemaVersion: Int,
    val rulesCount: Int,
    val modelsCount: Int,
    val classifiersCount: Int = 0,
    val sourcesCount: Int = 0,
    val bitmaskCount: Int = 0,
    val origin: String = "USER_IMPORTED", // "PANICLAB_OFFICIAL", "USER_IMPORTED", "BUNDLED"
    val sourceFilename: String? = null,
    val checksum: String = "",
    val isActive: Boolean = false,
    val isDefault: Boolean = false,
    val previousVersion: String? = null,
    val rawJson: String? = null,
    val importedAt: Long = System.currentTimeMillis()
)
