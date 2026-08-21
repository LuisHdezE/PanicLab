package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paniclab_preferences")

class DataStoreManager(private val context: Context) {
    companion object {
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_SAVE_RAW_LOGS = booleanPreferencesKey("save_raw_logs")
        val KEY_REDACT_IDENTIFIERS = booleanPreferencesKey("redact_identifiers_in_reports")
        val KEY_LAST_RULE_PACK_VERSION = stringPreferencesKey("last_rule_pack_version")
    }

    val darkModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: "SYSTEM"
    }

    val saveRawLogsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVE_RAW_LOGS] ?: false
    }

    val redactIdentifiersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REDACT_IDENTIFIERS] ?: true
    }

    val lastRulePackVersionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_RULE_PACK_VERSION] ?: "1.0.0"
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = mode
        }
    }

    suspend fun setSaveRawLogs(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAVE_RAW_LOGS] = save
        }
    }

    suspend fun setRedactIdentifiers(redact: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REDACT_IDENTIFIERS] = redact
        }
    }

    suspend fun setLastRulePackVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_RULE_PACK_VERSION] = version
        }
    }
}
