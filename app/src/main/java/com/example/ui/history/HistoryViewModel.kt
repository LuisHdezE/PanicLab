package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiagnosticReport
import com.example.domain.repository.DiagnosticRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedConfidenceFilter = MutableStateFlow<ConfidenceLevel?>(null)
    val selectedConfidenceFilter: StateFlow<ConfidenceLevel?> = _selectedConfidenceFilter.asStateFlow()

    val sessions: StateFlow<List<DiagnosticReport>> = combine(
        diagnosticRepository.getSessionHistory(),
        _searchQuery,
        _selectedConfidenceFilter
    ) { list, query, filterConfidence ->
        list.filter { session ->
            val matchesQuery = query.isBlank() ||
                session.deviceModel?.marketingName?.contains(query, ignoreCase = true) == true ||
                session.productCode.contains(query, ignoreCase = true) ||
                session.primaryCandidate?.label?.contains(query, ignoreCase = true) == true ||
                session.sourceFilename?.contains(query, ignoreCase = true) == true

            val matchesConfidence = filterConfidence == null || session.confidence == filterConfidence

            matchesQuery && matchesConfidence
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setConfidenceFilter(confidence: ConfidenceLevel?) {
        _selectedConfidenceFilter.value = confidence
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            diagnosticRepository.deleteSession(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            diagnosticRepository.clearHistory()
        }
    }

    fun reanalyzeSession(id: String, onResult: (Result<DiagnosticReport>) -> Unit) {
        viewModelScope.launch {
            val res = diagnosticRepository.reanalyzeSession(id)
            onResult(res)
        }
    }
}

class HistoryViewModelFactory(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(diagnosticRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
