package com.example.domain.model

/**
 * Android/product-edge models that are intentionally not part of the deterministic
 * shared diagnostic core. The portable diagnostic/rule models live in :shared.
 */
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
