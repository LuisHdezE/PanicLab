package com.example.ui.knowledge_base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DeviceModel
import com.example.domain.model.DiagnosticRule
import com.example.domain.repository.KnowledgeBaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KnowledgeBaseViewModel(
    private val kbRepository: KnowledgeBaseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProfileFilter = MutableStateFlow<String?>(null)
    val selectedProfileFilter: StateFlow<String?> = _selectedProfileFilter.asStateFlow()

    val devices: StateFlow<List<DeviceModel>> = kbRepository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<DiagnosticRule>> = combine(
        kbRepository.getAllRules(),
        _searchQuery,
        _selectedProfileFilter
    ) { allRules, query, profile ->
        allRules.filter { rule ->
            val matchesQuery = query.isBlank() ||
                rule.title.contains(query, ignoreCase = true) ||
                rule.diagnosis.label.contains(query, ignoreCase = true) ||
                rule.sensorCodesExact.any { it.contains(query, ignoreCase = true) } ||
                rule.sensorTokens.any { it.contains(query, ignoreCase = true) } ||
                rule.diagnosis.suspectedComponents.any { it.name.contains(query, ignoreCase = true) }

            val matchesProfile = profile == null ||
                rule.deviceScope.diagnosticProfiles.isEmpty() ||
                rule.deviceScope.diagnosticProfiles.contains(profile)

            matchesQuery && matchesProfile
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentVersion: StateFlow<String> = flow {
        emit(kbRepository.getCurrentRulePackVersion())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.0.0")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProfileFilter(profile: String?) {
        _selectedProfileFilter.value = profile
    }

    fun importRulePack(jsonContent: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = kbRepository.importRulePackJson(jsonContent)
            onResult(result)
        }
    }
}

class KnowledgeBaseViewModelFactory(
    private val kbRepository: KnowledgeBaseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KnowledgeBaseViewModel::class.java)) {
            return KnowledgeBaseViewModel(kbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
