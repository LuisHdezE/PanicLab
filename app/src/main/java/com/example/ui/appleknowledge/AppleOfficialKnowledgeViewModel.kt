package com.example.ui.appleknowledge

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.query.AppleOfficialKnowledgeModelView
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntime
import com.example.data.appleknowledge.AndroidAppleOfficialKnowledgeLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppleOfficialKnowledgeUiState(
    val exactModels: List<String> = emptyList(),
    val selectedModel: String? = null,
    val selectedCategory: AppleKnowledgeCategory? = null,
    val modelView: AppleOfficialKnowledgeModelView? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AppleOfficialKnowledgeViewModel(
    private val runtime: AppleOfficialKnowledgeRuntime
) : ViewModel() {
    private val _state = MutableStateFlow(
        AppleOfficialKnowledgeUiState(exactModels = runtime.catalog.exactModels)
    )
    val state: StateFlow<AppleOfficialKnowledgeUiState> = _state.asStateFlow()

    init {
        runtime.catalog.exactModels.lastOrNull()?.let(::selectModel)
    }

    fun selectModel(exactModel: String) {
        _state.value = _state.value.copy(
            selectedModel = exactModel,
            selectedCategory = null
        )
        refresh()
    }

    fun selectCategory(category: AppleKnowledgeCategory?) {
        _state.value = _state.value.copy(selectedCategory = category)
        refresh()
    }

    fun retry() = refresh()

    private fun refresh() {
        val model = _state.value.selectedModel ?: return
        val category = _state.value.selectedCategory
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { runtime.getForModel.execute(model, category) }
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        modelView = result,
                        isLoading = false,
                        error = if (result == null) "Modelo fuera del alcance auditado." else null
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        modelView = null,
                        isLoading = false,
                        error = throwable.message ?: "No se pudo cargar Apple Official Knowledge."
                    )
                }
        }
    }
}

class AppleOfficialKnowledgeViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppleOfficialKnowledgeViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return AppleOfficialKnowledgeViewModel(
            AndroidAppleOfficialKnowledgeLoader.load(appContext)
        ) as T
    }
}
