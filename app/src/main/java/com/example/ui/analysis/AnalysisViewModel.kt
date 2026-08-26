package com.example.ui.analysis

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DiagnosticReport
import com.example.domain.repository.DiagnosticRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Analyzing : AnalysisUiState()
    data class Success(val report: DiagnosticReport) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

data class SampleLog(
    val title: String,
    val description: String,
    val device: String,
    val expectedOutcome: String,
    val rawText: String
)

class AnalysisViewModel(
    private val diagnosticRepository: DiagnosticRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _logInputText = MutableStateFlow("")
    val logInputText: StateFlow<String> = _logInputText.asStateFlow()

    private val _selectedFilename = MutableStateFlow<String?>(null)
    val selectedFilename: StateFlow<String?> = _selectedFilename.asStateFlow()

    private val _repairSuggestionState = MutableStateFlow<com.example.domain.model.RepairSuggestionUiState>(com.example.domain.model.RepairSuggestionUiState.Idle)
    val repairSuggestionState: StateFlow<com.example.domain.model.RepairSuggestionUiState> = _repairSuggestionState.asStateFlow()

    val sampleLogs: List<SampleLog> = listOf(
        SampleLog(
            title = "iPhone 13 mini — 0x1000",
            description = "Fallo sensor array SMC en puerto de carga",
            device = "iPhone 13 mini (iPhone14,4)",
            expectedOutcome = "Micrófono Inferior / Dock Flex (0x1000)",
            rawText = """
                {"bug_type":"210","timestamp":"2024-03-15 14:22:01.00 +0100","os_version":"iPhone OS 17.4 (21E236)","incident_id":"E8741B6C-32A1-4F62-8F51-1A4BC187E601","product":"iPhone14,4","build":"21E236"}
                Incident Identifier: E8741B6C-32A1-4F62-8F51-1A4BC187E601
                CrashReporter Key:   a1b2c3d4e5f678901234567890abcdef12345678
                Date/Time:           2024-03-15 14:22:01.00 +0100
                OS Version:          iPhone OS 17.4 (21E236)
                
                panic(cpu 0 caller 0xfffffff01bb1c6e4): "SMC PANIC - BSC failure at address 0x1000 - S.sensor array 0 - 6 is 0x0, 0x1000, 0x0, 0x0, 0x0, 0x0, 0x0"
                Debugger message: panic
                Memory ID: 0x6
                OS release type: User
                OS version: 21E236
                Kernel version: Darwin Kernel Version 23.4.0: Fri Feb  9 21:55:34 PST 2024; root:xnu-10063.101.3~2/RELEASE_ARM64_T8110
                roots_installed: 0
            """.trimIndent()
        ),
        SampleLog(
            title = "iPhone 14 — 0x500000",
            description = "Fallo de comunicación Gas Gauge / Batería",
            device = "iPhone 14 (iPhone14,7)",
            expectedOutcome = "Batería / Línea I2C Gas Gauge (0x500000)",
            rawText = """
                {"bug_type":"210","timestamp":"2024-04-10 18:30:12.00 +0200","os_version":"iPhone OS 17.3.1 (21D61)","product":"iPhone14,7","build":"21D61"}
                Incident Identifier: B391AA7F-4B52-44FE-B31F-2B5C1970221A
                OS Version:          iPhone OS 17.3.1 (21D61)
                
                panic(cpu 1 caller 0xfffffff0201a4e10): "SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0, 0x0, 0x0"
                Debugger message: panic
                Kernel version: Darwin Kernel Version 23.3.0: Wed Jan 10 22:20:15 PST 2024; root:xnu-10063.82.9~2/RELEASE_ARM64_T8110
            """.trimIndent()
        ),
        SampleLog(
            title = "iPhone 16 Pro — 3145728 (0x300000)",
            description = "Sensor array decimal en iPhone 16 Pro",
            device = "iPhone 16 Pro (iPhone17,1)",
            expectedOutcome = "Batería + Sensor Proximidad (0x300000)",
            rawText = """
                {"bug_type":"210","timestamp":"2024-10-02 09:15:44.00 +0200","os_version":"iPhone OS 18.0.1 (22A3370)","product":"iPhone17,1","build":"22A3370"}
                Date/Time:           2024-10-02 09:15:44.00 +0200
                OS Version:          iPhone OS 18.0.1 (22A3370)
                
                panic(cpu 2 caller 0xfffffff031021bc0): "SMC PANIC - BSC failure: S.sensor array 0 - 7 is 0, 3145728, 0, 0, 0, 0, 0"
                Debugger message: panic
                Kernel version: Darwin Kernel Version 24.0.0: Mon Sep  2 21:05:12 PDT 2024; root:xnu-11215.1.10~2/RELEASE_ARM64_T8130
            """.trimIndent()
        ),
        SampleLog(
            title = "iPhone X — Missing sensor PRS0",
            description = "Pánico térmico por sensor de presión barométrica",
            device = "iPhone X (iPhone10,3)",
            expectedOutcome = "Flex de Carga / Barómetro (PRS0)",
            rawText = """
                {"bug_type":"210","timestamp":"2023-11-20 11:04:19.00 +0100","os_version":"iPhone OS 16.7.2 (20H115)","product":"iPhone10,3","build":"20H115"}
                Incident Identifier: 1A2B3C4D-5E6F-7A8B-9C0D-1E2F3A4B5C6D
                OS Version:          iPhone OS 16.7.2 (20H115)
                
                panic(cpu 0 caller 0xfffffff008912e80): "thermalmonitord: Missing sensor(s): PRS0, shutting down device due to thermal runaway safeguard"
                Debugger message: panic
                Kernel version: Darwin Kernel Version 22.6.0: Fri Sep 15 16:41:27 PDT 2023; root:xnu-8796.142.1~1/RELEASE_ARM64_T8015
            """.trimIndent()
        ),
        SampleLog(
            title = "Código SMC No Documentado (Fallback)",
            description = "Código de sensor no catalogado en la base local",
            device = "iPhone 14 Pro (iPhone15,2)",
            expectedOutcome = "Diagnóstico no concluyente (Sin inventar)",
            rawText = """
                {"bug_type":"210","timestamp":"2024-05-18 16:40:00.00 +0200","os_version":"iPhone OS 17.5 (21F79)","product":"iPhone15,2","build":"21F79"}
                Date/Time:           2024-05-18 16:40:00.00 +0200
                OS Version:          iPhone OS 17.5 (21F79)
                
                panic(cpu 0 caller 0xfffffff01048a120): "SMC PANIC - BSC failure at address 0x987654 - S.sensor array 0 - 6 is 0x0, 0x987654, 0x0, 0x0"
                Debugger message: panic
            """.trimIndent()
        )
    )

    fun updateLogInputText(text: String) {
        _logInputText.value = text
    }

    fun loadSample(sample: SampleLog) {
        _logInputText.value = sample.rawText
        _selectedFilename.value = "Muestra: ${sample.title}"
    }

    fun loadFromUri(context: Context, uri: Uri, filename: String?) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Analyzing
            _selectedFilename.value = filename
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                _logInputText.value = content
                analyzeText(content, filename)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error("Error al leer el archivo: ${e.localizedMessage}")
            }
        }
    }

    fun analyzeRawLog(rawText: String, filename: String? = null) {
        val text = rawText.trim()
        if (text.isBlank()) {
            _uiState.value = AnalysisUiState.Error("El contenido del log no puede estar vacío.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Analyzing
            _selectedFilename.value = filename
            analyzeText(text, filename)
        }
    }

    fun analyzeCurrentInput(onSuccess: (String) -> Unit) {
        val text = _logInputText.value.trim()
        if (text.isBlank()) {
            _uiState.value = AnalysisUiState.Error("El contenido del log no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Analyzing
            try {
                val saveRawLogs = settingsRepository.saveRawLogsFlow.first()
                val report = diagnosticRepository.analyzeLog(
                    rawLogContent = text,
                    sourceFilename = _selectedFilename.value,
                    saveRawLog = saveRawLogs
                )
                _uiState.value = AnalysisUiState.Success(report)
                onSuccess(report.id)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AnalysisUiState.Error("Fallo en el motor de diagnóstico: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun analyzeText(text: String, filename: String?) {
        try {
            val saveRawLogs = settingsRepository.saveRawLogsFlow.first()
            val report = diagnosticRepository.analyzeLog(
                rawLogContent = text,
                sourceFilename = filename,
                saveRawLog = saveRawLogs
            )
            _uiState.value = AnalysisUiState.Success(report)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = AnalysisUiState.Error("Fallo en el motor de diagnóstico: ${e.localizedMessage}")
        }
    }

    fun fetchRepairSuggestions(report: DiagnosticReport) {
        viewModelScope.launch {
            _repairSuggestionState.value = com.example.domain.model.RepairSuggestionUiState.Loading
            try {
                val result = diagnosticRepository.fetchRealTimeRepairSuggestions(report)
                if (result.isSuccess) {
                    _repairSuggestionState.value = com.example.domain.model.RepairSuggestionUiState.Success(result.getOrThrow())
                } else {
                    _repairSuggestionState.value = com.example.domain.model.RepairSuggestionUiState.Error(
                        result.exceptionOrNull()?.localizedMessage ?: "Error al consultar sugerencias de reparación."
                    )
                }
            } catch (e: Exception) {
                _repairSuggestionState.value = com.example.domain.model.RepairSuggestionUiState.Error(e.localizedMessage ?: "Error inesperado")
            }
        }
    }

    fun saveTechnicianNotes(sessionId: String, notes: String) {
        viewModelScope.launch {
            diagnosticRepository.saveTechnicianNotes(sessionId, notes)
        }
    }

    fun resetState() {
        _uiState.value = AnalysisUiState.Idle
        _repairSuggestionState.value = com.example.domain.model.RepairSuggestionUiState.Idle
    }
}

class AnalysisViewModelFactory(
    private val diagnosticRepository: DiagnosticRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            return AnalysisViewModel(diagnosticRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
