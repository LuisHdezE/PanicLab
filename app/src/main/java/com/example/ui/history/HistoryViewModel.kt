package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.RepairSuggestionUiState
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

    private val _repairSuggestionState = MutableStateFlow<RepairSuggestionUiState>(RepairSuggestionUiState.Idle)
    val repairSuggestionState: StateFlow<RepairSuggestionUiState> = _repairSuggestionState.asStateFlow()

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
                session.sourceFilename?.contains(query, ignoreCase = true) == true ||
                session.technicianNotes?.contains(query, ignoreCase = true) == true ||
                session.customerName?.contains(query, ignoreCase = true) == true

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

    fun saveTechnicianNotes(id: String, notes: String) {
        viewModelScope.launch {
            diagnosticRepository.saveTechnicianNotes(id, notes)
        }
    }

    fun saveCustomerInfo(id: String, customerName: String, notes: String) {
        viewModelScope.launch {
            diagnosticRepository.saveCustomerInfo(id, customerName, notes)
        }
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

    fun fetchRepairSuggestions(report: DiagnosticReport) {
        viewModelScope.launch {
            _repairSuggestionState.value = RepairSuggestionUiState.Loading
            try {
                val res = diagnosticRepository.fetchRealTimeRepairSuggestions(report)
                if (res.isSuccess) {
                    _repairSuggestionState.value = RepairSuggestionUiState.Success(res.getOrThrow())
                } else {
                    _repairSuggestionState.value = RepairSuggestionUiState.Error(
                        res.exceptionOrNull()?.localizedMessage ?: "Error al consultar sugerencias."
                    )
                }
            } catch (e: Exception) {
                _repairSuggestionState.value = RepairSuggestionUiState.Error(e.localizedMessage ?: "Error")
            }
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
