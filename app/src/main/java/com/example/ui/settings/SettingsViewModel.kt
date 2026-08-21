package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val kbRepository: KnowledgeBaseRepository
) : ViewModel() {

    val darkMode: StateFlow<String> = settingsRepository.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val saveRawLogs: StateFlow<Boolean> = settingsRepository.saveRawLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val redactIdentifiers: StateFlow<Boolean> = settingsRepository.redactIdentifiersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val kbVersion: StateFlow<String> = flow {
        emit(kbRepository.getCurrentRulePackVersion())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.0.0")

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(mode)
        }
    }

    fun setSaveRawLogs(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveRawLogs(save)
        }
    }

    fun setRedactIdentifiers(redact: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRedactIdentifiers(redact)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val kbRepository: KnowledgeBaseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsRepository, kbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
