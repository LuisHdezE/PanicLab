package com.example.domain.model

data class SearchGroundingSource(
    val title: String,
    val url: String,
    val snippet: String? = null
)

data class GroundedRepairSuggestion(
    val summary: String,
    val detailedSteps: List<String> = emptyList(),
    val suspectedComponents: List<String> = emptyList(),
    val diodeModeReferenceTips: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val searchSources: List<SearchGroundingSource> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val retrievedAt: Long = System.currentTimeMillis(),
    val isRealTimeGrounded: Boolean = true
)

sealed class RepairSuggestionUiState {
    object Idle : RepairSuggestionUiState()
    object Loading : RepairSuggestionUiState()
    data class Success(val suggestion: GroundedRepairSuggestion) : RepairSuggestionUiState()
    data class Error(
        val message: String,
        val fallbackSuggestion: GroundedRepairSuggestion? = null
    ) : RepairSuggestionUiState()
}
