package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface KnowledgeBaseRepository {
    fun getAllRules(): Flow<List<DiagnosticRule>>
    suspend fun getAllRulesDirect(): List<DiagnosticRule>
    fun searchRules(query: String): Flow<List<DiagnosticRule>>
    suspend fun getRuleById(id: String): DiagnosticRule?
    fun getAllDevices(): Flow<List<DeviceModel>>
    suspend fun findDevice(query: String): DeviceModel?
    suspend fun initializeDefaultRulePackIfNeeded(): Boolean
    suspend fun importRulePackJson(jsonContent: String): Result<String>
    suspend fun getCurrentRulePackVersion(): String
}

interface DiagnosticRepository {
    suspend fun analyzeLog(
        rawLogContent: String,
        sourceFilename: String?,
        saveRawLog: Boolean
    ): DiagnosticReport

    fun getSessionHistory(): Flow<List<DiagnosticReport>>
    suspend fun getSessionById(sessionId: String): DiagnosticReport?
    suspend fun deleteSession(sessionId: String)
    suspend fun clearHistory()
}

interface SettingsRepository {
    val darkModeFlow: Flow<String>
    val saveRawLogsFlow: Flow<Boolean>
    val redactIdentifiersFlow: Flow<Boolean>
    val lastRulePackVersionFlow: Flow<String>

    suspend fun setDarkMode(mode: String)
    suspend fun setSaveRawLogs(save: Boolean)
    suspend fun setRedactIdentifiers(redact: Boolean)
}
