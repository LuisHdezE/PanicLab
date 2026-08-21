package com.example.ui.knowledge_base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.RulePackEntity
import com.example.domain.model.*
import com.example.domain.repository.KnowledgeBaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface RulePackPreviewUiState {
    object Idle : RulePackPreviewUiState
    object Validating : RulePackPreviewUiState
    data class ValidatedDiff(
        val validationResult: RulePackValidationResult,
        val parsedPack: ParsedRulePack,
        val diffSummary: RulePackDiffSummary,
        val filename: String?
    ) : RulePackPreviewUiState
    data class ValidationError(
        val validationResult: RulePackValidationResult,
        val filename: String?
    ) : RulePackPreviewUiState
    object Installing : RulePackPreviewUiState
}

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

    val allRulePacks: StateFlow<List<RulePackEntity>> = kbRepository.getAllRulePackEntities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRulePack: StateFlow<RulePackEntity?> = kbRepository.getActiveRulePackEntity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentVersion: StateFlow<String> = activeRulePack.map { it?.version ?: "1.0.0" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.0.0")

    private val _previewState = MutableStateFlow<RulePackPreviewUiState>(RulePackPreviewUiState.Idle)
    val previewState: StateFlow<RulePackPreviewUiState> = _previewState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProfileFilter(profile: String?) {
        _selectedProfileFilter.value = profile
    }

    fun validateAndPreviewRulePack(jsonContent: String, filename: String? = null, origin: RulePackOrigin = RulePackOrigin.USER_IMPORTED) {
        viewModelScope.launch {
            _previewState.value = RulePackPreviewUiState.Validating
            try {
                val (validationResult, parsedPack) = kbRepository.validateRulePack(jsonContent, origin, filename)
                if (!validationResult.isValid || parsedPack == null) {
                    _previewState.value = RulePackPreviewUiState.ValidationError(
                        validationResult = validationResult,
                        filename = filename
                    )
                } else {
                    val diffSummary = kbRepository.computeDiffWithCurrent(parsedPack)
                    _previewState.value = RulePackPreviewUiState.ValidatedDiff(
                        validationResult = validationResult,
                        parsedPack = parsedPack,
                        diffSummary = diffSummary,
                        filename = filename
                    )
                }
            } catch (e: Exception) {
                _previewState.value = RulePackPreviewUiState.ValidationError(
                    validationResult = RulePackValidationResult(
                        isValid = false,
                        schemaVersion = 0,
                        knowledgeBaseVersion = "0.0.0",
                        rulesCount = 0,
                        modelsCount = 0,
                        classifiersCount = 0,
                        sourcesCount = 0,
                        bitmaskCount = 0,
                        errors = listOf(ValidationIssue(ValidationIssue.IssueType.ERROR, "root", "Excepción al procesar archivo: ${e.message}")),
                        warnings = emptyList(),
                        checksum = ""
                    ),
                    filename = filename
                )
            }
        }
    }

    fun installValidatedPack(
        parsedPack: ParsedRulePack,
        filename: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _previewState.value = RulePackPreviewUiState.Installing
            val result = kbRepository.installRulePackAtomic(parsedPack, filename)
            _previewState.value = RulePackPreviewUiState.Idle
            if (result.isSuccess) {
                onSuccess(result.getOrDefault(parsedPack.knowledgeBaseVersion))
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error desconocido al instalar")
            }
        }
    }

    fun restoreBundledDefault(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = kbRepository.restoreBundledDefault()
            if (result.isSuccess) {
                onSuccess(result.getOrDefault("1.0.0"))
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error al restaurar valores de fábrica")
            }
        }
    }

    fun restoreVersion(version: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = kbRepository.restoreRulePackVersion(version)
            if (result.isSuccess) {
                onSuccess(version)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error al restaurar versión $version")
            }
        }
    }

    fun clearPreview() {
        _previewState.value = RulePackPreviewUiState.Idle
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
