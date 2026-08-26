package com.example.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.DiagnosticRepository
import com.example.domain.repository.KnowledgeBaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrendDashboardViewModel(
    private val diagnosticRepository: DiagnosticRepository,
    private val kbRepository: KnowledgeBaseRepository
) : ViewModel() {

    private val _dashboardData = MutableStateFlow<TechnicianTrendDashboardData?>(null)
    val dashboardData: StateFlow<TechnicianTrendDashboardData?> = _dashboardData.asStateFlow()

    private val _selectedModelFilter = MutableStateFlow("ALL")
    val selectedModelFilter: StateFlow<String> = _selectedModelFilter.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow("ALL")
    val selectedTimeFilter: StateFlow<String> = _selectedTimeFilter.asStateFlow()

    private val _currentViewMode = MutableStateFlow("codes")
    val currentViewMode: StateFlow<String> = _currentViewMode.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTrendMetrics()
        // Listen to Room session history updates
        viewModelScope.launch {
            diagnosticRepository.getSessionHistory().collect {
                computeMetrics()
            }
        }
    }

    fun loadTrendMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            computeMetrics()
            _isLoading.value = false
        }
    }

    fun setModelFilter(model: String) {
        _selectedModelFilter.value = model
        computeMetrics()
    }

    fun setTimeFilter(timeDays: String) {
        _selectedTimeFilter.value = timeDays
        computeMetrics()
    }

    fun setViewMode(mode: String) {
        _currentViewMode.value = mode
    }

    private fun computeMetrics() {
        viewModelScope.launch(Dispatchers.Default) {
            val sessions = mutableListOf<DiagnosticReport>()
            // Collect existing sessions from Room
            diagnosticRepository.getSessionHistory().collect { list ->
                sessions.clear()
                sessions.addAll(list)
                
                val filteredSessions = applyTimeFilter(sessions, _selectedTimeFilter.value)
                val metrics = processReportsIntoDashboardData(filteredSessions)
                _dashboardData.value = metrics
                return@collect
            }
        }
    }

    private fun applyTimeFilter(reports: List<DiagnosticReport>, timeFilter: String): List<DiagnosticReport> {
        if (timeFilter == "ALL") return reports
        val days = timeFilter.toLongOrNull() ?: return reports
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return reports.filter { it.createdAt >= cutoff }
    }

    private fun processReportsIntoDashboardData(reports: List<DiagnosticReport>): TechnicianTrendDashboardData {
        if (reports.isEmpty()) {
            return TechnicianTrendDashboardData(
                totalCases = 0,
                totalUniqueCodes = 0,
                mostFrequentCode = null,
                mostFailingSubsystem = null,
                averageConfidencePercent = 0,
                panicCodeStats = emptyList(),
                subsystemStats = emptyList(),
                modelCorrelations = emptyList(),
                timelinePoints = emptyList(),
                hardwareInsights = emptyList()
            )
        }

        val totalCases = reports.size

        // 1. Group by Panic Codes / Sensor Codes
        val codeCountMap = mutableMapOf<String, CodeAggregate>()
        val subsystemCountMap = mutableMapOf<String, SubsystemAggregate>()
        val modelPanicMap = mutableMapOf<String, ModelAggregate>()
        val dateGroupMap = TreeMap<String, MutableList<DiagnosticReport>>()

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        reports.forEach { report ->
            // Date group
            val dateKey = dateFormat.format(Date(report.createdAt))
            dateGroupMap.getOrPut(dateKey) { mutableListOf() }.add(report)

            // Device Model
            val deviceModelName = report.deviceModel?.marketingName ?: report.productCode
            val modelAgg = modelPanicMap.getOrPut(report.productCode) {
                ModelAggregate(
                    productCode = report.productCode,
                    marketingName = deviceModelName,
                    total = 0,
                    panicCounts = mutableMapOf()
                )
            }
            modelAgg.total++

            // Determine Panic Code
            val candidate = report.primaryCandidate
            val ruleId = candidate?.ruleId ?: ""
            val codeStr = extractPanicCodeDisplay(report)
            val label = candidate?.label ?: report.panicStringSummary ?: "Falla de Sistema"
            val subsystem = determineSubsystem(report)

            // Update model panic counts
            modelAgg.panicCounts[codeStr] = (modelAgg.panicCounts[codeStr] ?: 0) + 1

            // Update Code Aggregate
            val codeAgg = codeCountMap.getOrPut(codeStr) {
                CodeAggregate(
                    code = codeStr,
                    label = label,
                    ruleId = ruleId,
                    subsystem = subsystem,
                    diodeHint = extractDiodeHint(codeStr),
                    count = 0,
                    models = mutableSetOf()
                )
            }
            codeAgg.count++
            codeAgg.models.add(report.productCode)

            // Update Subsystem Aggregate
            val subAgg = subsystemCountMap.getOrPut(subsystem) {
                SubsystemAggregate(
                    subsystemId = subsystem,
                    label = getSubsystemFriendlyName(subsystem),
                    colorHex = getSubsystemColor(subsystem),
                    topFailingPart = getSubsystemTopPart(subsystem),
                    commonSymptoms = getSubsystemSymptoms(subsystem),
                    count = 0
                )
            }
            subAgg.count++
        }

        // Convert to PanicCodeStat list
        val panicCodeStats = codeCountMap.values.sortedByDescending { it.count }.map {
            PanicCodeStat(
                code = it.code,
                label = it.label,
                count = it.count,
                percentage = (it.count.toFloat() / totalCases) * 100f,
                ruleId = it.ruleId,
                subsystem = it.subsystem,
                affectedModels = it.models.toList(),
                diodeModeHint = it.diodeHint
            )
        }

        // Convert to SubsystemFailureStat list
        val subsystemStats = subsystemCountMap.values.sortedByDescending { it.count }.map {
            SubsystemFailureStat(
                subsystemId = it.subsystemId,
                label = it.label,
                count = it.count,
                percentage = (it.count.toFloat() / totalCases) * 100f,
                colorHex = it.colorHex,
                topFailingPart = it.topFailingPart,
                commonSymptoms = it.commonSymptoms
            )
        }

        // Convert to ModelFailureCorrelation list
        val modelCorrelations = modelPanicMap.values.sortedByDescending { it.total }.map {
            val topCodeEntry = it.panicCounts.maxByOrNull { entry -> entry.value }
            val topCode = topCodeEntry?.key ?: "0x1000"
            val topLabel = codeCountMap[topCode]?.label ?: "Falla general"

            ModelFailureCorrelation(
                productCode = it.productCode,
                marketingName = it.marketingName,
                totalPanics = it.total,
                topPanicCode = topCode,
                topPanicLabel = topLabel,
                panicDistribution = it.panicCounts
            )
        }

        // Convert Timeline Points
        val timelinePoints = dateGroupMap.map { (dateStr, group) ->
            val topCodeInGroup = group.groupBy { extractPanicCodeDisplay(it) }.maxByOrNull { it.value.size }?.key ?: "0x1000"
            TimelineTrendPoint(
                timestamp = group.first().createdAt,
                dateFormatted = dateStr,
                count = group.size,
                topCode = topCodeInGroup
            )
        }

        // Average Confidence
        val highConfidenceCount = reports.count { it.confidence == ConfidenceLevel.HIGH }
        val avgConfidence = if (totalCases > 0) ((highConfidenceCount.toFloat() / totalCases) * 100).toInt() else 0

        // Hardware Insights Engine
        val hardwareInsights = generateHardwareInsights(panicCodeStats, subsystemStats, modelCorrelations, totalCases)

        return TechnicianTrendDashboardData(
            totalCases = totalCases,
            totalUniqueCodes = panicCodeStats.size,
            mostFrequentCode = panicCodeStats.firstOrNull(),
            mostFailingSubsystem = subsystemStats.firstOrNull(),
            averageConfidencePercent = avgConfidence,
            panicCodeStats = panicCodeStats,
            subsystemStats = subsystemStats,
            modelCorrelations = modelCorrelations,
            timelinePoints = timelinePoints,
            hardwareInsights = hardwareInsights
        )
    }

    private fun extractPanicCodeDisplay(report: DiagnosticReport): String {
        // Look for hex SMC codes in primary diagnosis label or rule id
        val primaryLabel = report.primaryCandidate?.label ?: ""
        val ruleId = report.primaryCandidate?.ruleId ?: ""
        val panicSummary = report.panicStringSummary ?: ""

        val hexRegex = Regex("(0x[0-9a-fA-F]{3,8})")
        val match = hexRegex.find(primaryLabel) ?: hexRegex.find(ruleId) ?: hexRegex.find(panicSummary)
        if (match != null) {
            val hex = match.value
            return when {
                hex.equals("0x1000", ignoreCase = true) -> "0x1000 (Mic2)"
                hex.equals("0x80000", ignoreCase = true) -> "0x80000 (Prs0)"
                hex.equals("0x40000", ignoreCase = true) -> "0x40000 (TG0B)"
                hex.equals("0x400000", ignoreCase = true) -> "0x400000 (NTC)"
                hex.equals("0x20000", ignoreCase = true) -> "0x20000 (Prox)"
                hex.equals("0x8000", ignoreCase = true) -> "0x8000 (NAND)"
                else -> hex
            }
        }

        // Check Panic Family names
        if (report.panicFamilies.contains(PanicFamily.WATCHDOG_NO_CHECKIN)) return "Watchdog Timeout"
        if (report.panicFamilies.contains(PanicFamily.I2C)) return "I2C Bus Error"
        if (report.panicFamilies.contains(PanicFamily.AOP_OTHER) || report.panicFamilies.contains(PanicFamily.AOP_NMI_POWER) || report.panicFamilies.contains(PanicFamily.AOP_BOSCH_CONTROL)) return "AOP Sensor Timeout"
        if (report.panicFamilies.contains(PanicFamily.SEP_ROM_BOOT)) return "SEP Cryptographic Panic"
        if (report.panicFamilies.contains(PanicFamily.ANS2)) return "NVMe / Storage Panic"

        return if (primaryLabel.isNotBlank()) primaryLabel.take(20) else "SMC 0x1000"
    }

    private fun determineSubsystem(report: DiagnosticReport): String {
        val label = (report.primaryCandidate?.label ?: "").lowercase()
        val summary = (report.panicStringSummary ?: "").lowercase()

        return when {
            label.contains("micrófono") || label.contains("carga") || label.contains("0x1000") || label.contains("dock") -> "AUDIO_CHARGING"
            label.contains("térmico") || label.contains("batería") || label.contains("ntc") || label.contains("0x400000") || label.contains("tg0b") -> "POWER_BATTERY"
            label.contains("barómetro") || label.contains("0x80000") || label.contains("prs0") || label.contains("proximidad") || label.contains("0x20000") -> "ENVIRONMENTAL_SENSORS"
            label.contains("i2c") || label.contains("pmu") || label.contains("smc") || summary.contains("i2c") -> "LOGIC_BUS"
            label.contains("watchdog") || label.contains("kernel") || label.contains("aop") || label.contains("nand") -> "KERNEL_STORAGE"
            else -> "PERIPHERALS"
        }
    }

    private fun getSubsystemFriendlyName(subsystem: String): String {
        return when (subsystem) {
            "AUDIO_CHARGING" -> "Flex de Carga & Micrófonos"
            "POWER_BATTERY" -> "Gestión Térmica & Batería NTC"
            "ENVIRONMENTAL_SENSORS" -> "Sensores Barómetro / ALS"
            "LOGIC_BUS" -> "Bus I2C & Comunicaciones PMU"
            "KERNEL_STORAGE" -> "Kernel, Watchdog & Almacenamiento"
            else -> "Periféricos & Placa Base"
        }
    }

    private fun getSubsystemColor(subsystem: String): String {
        return when (subsystem) {
            "AUDIO_CHARGING" -> "#00F5FF"
            "POWER_BATTERY" -> "#EF4444"
            "ENVIRONMENTAL_SENSORS" -> "#10B981"
            "LOGIC_BUS" -> "#F59E0B"
            "KERNEL_STORAGE" -> "#A855F7"
            else -> "#3B82F6"
        }
    }

    private fun getSubsystemTopPart(subsystem: String): String {
        return when (subsystem) {
            "AUDIO_CHARGING" -> "Flex de Carga Lightning/USB-C (Mic2)"
            "POWER_BATTERY" -> "Sensor Térmico NTC / Línea Gas Gauge"
            "ENVIRONMENTAL_SENSORS" -> "Sensor Barométrico Prs0 / Flex Auricular"
            "LOGIC_BUS" -> "Líneas I2C0 / I2C1 Pull-up 2.2kΩ"
            "KERNEL_STORAGE" -> "NAND Flash / PMIC Principal"
            else -> "Conector FPC"
        }
    }

    private fun getSubsystemSymptoms(subsystem: String): List<String> {
        return when (subsystem) {
            "AUDIO_CHARGING" -> listOf("Reinicio cada 3 minutos", "Sin audio en llamadas", "Carga intermitente")
            "POWER_BATTERY" -> listOf("Sin lectura de % batería", "Reinicio repentino bajo carga", "Indicador 1% fijo")
            "ENVIRONMENTAL_SENSORS" -> listOf("Reinicio cada 180s", "Falla de brillo automático", "TrueTone desactivado")
            "LOGIC_BUS" -> listOf("Consumo anormal en fuente", "I2C timeout en bus SMC", "Pérdida de periféricos")
            "KERNEL_STORAGE" -> listOf("Bucle de manzana constante", "Pantalla rosa o morada", "Error de lectura de partición")
            else -> listOf("Reinicio esporádico")
        }
    }

    private fun extractDiodeHint(code: String): String? {
        return when {
            code.contains("0x1000") -> "Línea I2C_SMC_BI_AP: 0.420V - 0.480V en FPC de carga"
            code.contains("0x80000") -> "Línea I2C_PRS0_SDA: 0.435V en conector de flex de carga"
            code.contains("0x40000") -> "Líneas TG0B / NTC: 0.510V hacia SoC / PMU"
            code.contains("0x400000") -> "Líneas SWI / BATT_NTC: 0.620V en conector de batería"
            code.contains("0x20000") -> "Líneas ALS_INT / I2C: 0.410V en FPC sensor superior"
            else -> "Verificar líneas de alimentación 1.8V_ALWAYS y 3.0V_SMC"
        }
    }

    private fun generateHardwareInsights(
        codeStats: List<PanicCodeStat>,
        subsystemStats: List<SubsystemFailureStat>,
        modelCorrelations: List<ModelFailureCorrelation>,
        totalCases: Int
    ): List<WorkshopHardwareInsight> {
        val insights = mutableListOf<WorkshopHardwareInsight>()

        // Insight 1: Top Panic Code Alert
        codeStats.firstOrNull()?.let { top ->
            if (top.percentage >= 25f) {
                insights.add(
                    WorkshopHardwareInsight(
                        title = "Patrón Crítico: Alta Incidencia de ${top.code}",
                        description = "El ${Math.round(top.percentage)}% de todos los pánicos analizados en tu taller corresponden a ${top.label}.",
                        recommendation = "Mantén stock de repuestos OEM para el flex de carga y verifica las líneas con multímetro en modo diodo (${top.diodeModeHint ?: "0.450V"}).",
                        priority = "CRITICAL",
                        relatedCode = top.code
                    )
                )
            }
        }

        // Insight 2: Model specific vulnerability
        modelCorrelations.firstOrNull { it.totalPanics >= 2 }?.let { model ->
            insights.add(
                WorkshopHardwareInsight(
                    title = "Tendencia de Modelo: ${model.marketingName}",
                    description = "Falla recurrente observada con código ${model.topPanicCode} (${model.topPanicLabel}) en ${model.marketingName}.",
                    recommendation = "Al recibir este modelo con reinicios de 3 minutos, prueba primero un flex de carga de prueba antes de manipular la placa madre.",
                    priority = "WARNING",
                    relatedCode = model.topPanicCode
                )
            )
        }

        // Insight 3: Thermal / Battery warning
        val thermalSub = subsystemStats.find { it.subsystemId == "POWER_BATTERY" }
        if (thermalSub != null && thermalSub.count >= 1) {
            insights.add(
                WorkshopHardwareInsight(
                    title = "Precaución en Líneas NTC / Batería",
                    description = "Se registraron fallas relacionadas con termistores y sensores de gas gauge.",
                    recommendation = "Revisa los pines del conector de batería (FPC) por pines doblados o soldadura fría antes de reemplazar la batería.",
                    priority = "INFO",
                    relatedCode = "0x400000"
                )
            )
        }

        return insights
    }

    /**
     * Seeds realistic demo cases in Room if technician wants to preview trends immediately
     */
    fun seedSampleWorkshopCases() {
        viewModelScope.launch(Dispatchers.IO) {
            val sampleLogs = listOf(
                // 1. iPhone 13 mini - 0x1000 Mic2
                """
                {"bug_type":"210","timestamp":"2026-08-10 14:22:01.00 -0400","os_version":"iPhone OS 17.5.1 (21F90)","incident_id":"SAMPLE-01"}
                {
                  "build" : "iPhone OS 17.5.1 (21F90)",
                  "product" : "iPhone14,4",
                  "socId" : "0x8110",
                  "panicString" : "panic(cpu 0 caller 0xfffffff0111a84f0): userspace watchdog timeout: no successful checkins from thermalmonitord in 180 seconds\nMissing sensor(s): Mic2\nSMC BSC failure: 0x1000"
                }
                """.trimIndent() to "iphone13mini_mic2.ips",

                // 2. iPhone 13 mini - 0x1000 Mic2
                """
                {"bug_type":"210","timestamp":"2026-08-12 11:05:00.00 -0400","os_version":"iPhone OS 17.4 (21E219)","incident_id":"SAMPLE-02"}
                {
                  "build" : "iPhone OS 17.4 (21E219)",
                  "product" : "iPhone14,4",
                  "panicString" : "userspace watchdog timeout: no successful checkins from thermalmonitord in 180 seconds\nMissing sensor(s): Mic2\nSMC BSC failure: 0x1000"
                }
                """.trimIndent() to "iphone13mini_mic2_case2.ips",

                // 3. iPhone 11 - 0x80000 Prs0 Barometer
                """
                {"bug_type":"210","timestamp":"2026-08-15 16:40:22.00 -0400","os_version":"iPhone OS 17.5 (21F79)","incident_id":"SAMPLE-03"}
                {
                  "build" : "iPhone OS 17.5 (21F79)",
                  "product" : "iPhone12,1",
                  "panicString" : "panic(cpu 1 caller 0xfffffff0123a10): thermalmonitord failed: Missing sensor(s): Prs0\nSMC BSC failure: 0x80000"
                }
                """.trimIndent() to "iphone11_barometer.ips",

                // 4. iPhone 12 Pro Max - 0x40000 TG0B
                """
                {"bug_type":"210","timestamp":"2026-08-18 09:15:33.00 -0400","os_version":"iPhone OS 17.3 (21D50)","incident_id":"SAMPLE-04"}
                {
                  "build" : "iPhone OS 17.3 (21D50)",
                  "product" : "iPhone13,4",
                  "panicString" : "panic(cpu 2 caller 0xfffffff0190a): userspace watchdog timeout in 180 seconds\nMissing sensor(s): TG0B\nSMC BSC failure: 0x40000"
                }
                """.trimIndent() to "iphone12promax_tg0b.ips",

                // 5. iPhone 14 Pro - 0x400000 NTC BATT
                """
                {"bug_type":"210","timestamp":"2026-08-20 18:30:10.00 -0400","os_version":"iPhone OS 17.6 (21G80)","incident_id":"SAMPLE-05"}
                {
                  "build" : "iPhone OS 17.6 (21G80)",
                  "product" : "iPhone15,2",
                  "panicString" : "thermalmonitord failed in 180 seconds\nMissing sensor(s): NTC BATT\nSMC BSC failure: 0x400000"
                }
                """.trimIndent() to "iphone14pro_batt_ntc.ips",

                // 6. iPhone 13 mini - 0x1000 Mic2
                """
                {"bug_type":"210","timestamp":"2026-08-24 10:10:00.00 -0400","os_version":"iPhone OS 17.5.1 (21F90)","incident_id":"SAMPLE-06"}
                {
                  "build" : "iPhone OS 17.5.1 (21F90)",
                  "product" : "iPhone14,4",
                  "panicString" : "Missing sensor(s): Mic2\nSMC BSC failure: 0x1000"
                }
                """.trimIndent() to "iphone13mini_mic2_case3.ips"
            )

            sampleLogs.forEach { (content, filename) ->
                try {
                    diagnosticRepository.analyzeLog(
                        rawLogContent = content,
                        sourceFilename = filename,
                        saveRawLog = true
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            computeMetrics()
        }
    }

    private data class CodeAggregate(
        val code: String,
        val label: String,
        val ruleId: String?,
        val subsystem: String,
        val diodeHint: String?,
        var count: Int,
        val models: MutableSet<String>
    )

    private data class SubsystemAggregate(
        val subsystemId: String,
        val label: String,
        val colorHex: String,
        val topFailingPart: String,
        val commonSymptoms: List<String>,
        var count: Int
    )

    private data class ModelAggregate(
        val productCode: String,
        val marketingName: String,
        var total: Int,
        val panicCounts: MutableMap<String, Int>
    )
}

class TrendDashboardViewModelFactory(
    private val diagnosticRepository: DiagnosticRepository,
    private val kbRepository: KnowledgeBaseRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrendDashboardViewModel::class.java)) {
            return TrendDashboardViewModel(diagnosticRepository, kbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

