package com.example.domain.model

data class RulePackMetadata(
    val version: String,
    val title: String,
    val generatedAt: String,
    val schemaVersion: Int,
    val rulesCount: Int,
    val modelsCount: Int,
    val classifiersCount: Int,
    val sourcesCount: Int,
    val bitmaskCount: Int,
    val origin: RulePackOrigin,
    val sourceFilename: String?,
    val checksum: String,
    val isActive: Boolean,
    val isDefault: Boolean,
    val previousVersion: String?,
    val importedAt: Long
)
