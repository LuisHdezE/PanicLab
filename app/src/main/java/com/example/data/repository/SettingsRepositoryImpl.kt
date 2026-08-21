package com.example.data.repository

import com.example.data.local.DataStoreManager
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : SettingsRepository {

    override val darkModeFlow: Flow<String> = dataStoreManager.darkModeFlow
    override val saveRawLogsFlow: Flow<Boolean> = dataStoreManager.saveRawLogsFlow
    override val redactIdentifiersFlow: Flow<Boolean> = dataStoreManager.redactIdentifiersFlow
    override val lastRulePackVersionFlow: Flow<String> = dataStoreManager.lastRulePackVersionFlow

    override suspend fun setDarkMode(mode: String) {
        dataStoreManager.setDarkMode(mode)
    }

    override suspend fun setSaveRawLogs(save: Boolean) {
        dataStoreManager.setSaveRawLogs(save)
    }

    override suspend fun setRedactIdentifiers(redact: Boolean) {
        dataStoreManager.setRedactIdentifiers(redact)
    }
}
