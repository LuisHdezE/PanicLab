package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class PanicCodeStat(
    val code: String,
    val label: String,
    val count: Int,
    val percentage: Float,
    val ruleId: String?,
    val subsystem: String,
    val affectedModels: List<String>,
    val diodeModeHint: String? = null,
    val severityLevel: String = "HIGH"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("code", code)
            put("label", label)
            put("count", count)
            put("percentage", percentage)
            put("ruleId", ruleId ?: "")
            put("subsystem", subsystem)
            put("affectedModels", JSONArray(affectedModels))
            put("diodeModeHint", diodeModeHint ?: "")
            put("severityLevel", severityLevel)
        }
    }
}

data class SubsystemFailureStat(
    val subsystemId: String,
    val label: String,
    val count: Int,
    val percentage: Float,
    val colorHex: String,
    val topFailingPart: String,
    val commonSymptoms: List<String>
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("subsystemId", subsystemId)
            put("label", label)
            put("count", count)
            put("percentage", percentage)
            put("colorHex", colorHex)
            put("topFailingPart", topFailingPart)
            put("commonSymptoms", JSONArray(commonSymptoms))
        }
    }
}

data class ModelFailureCorrelation(
    val productCode: String,
    val marketingName: String,
    val totalPanics: Int,
    val topPanicCode: String,
    val topPanicLabel: String,
    val panicDistribution: Map<String, Int>
) {
    fun toJson(): JSONObject {
        val distObj = JSONObject()
        panicDistribution.forEach { (code, count) -> distObj.put(code, count) }
        return JSONObject().apply {
            put("productCode", productCode)
            put("marketingName", marketingName)
            put("totalPanics", totalPanics)
            put("topPanicCode", topPanicCode)
            put("topPanicLabel", topPanicLabel)
            put("panicDistribution", distObj)
        }
    }
}

data class TimelineTrendPoint(
    val timestamp: Long,
    val dateFormatted: String,
    val count: Int,
    val topCode: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("timestamp", timestamp)
            put("dateFormatted", dateFormatted)
            put("count", count)
            put("topCode", topCode)
        }
    }
}

data class WorkshopHardwareInsight(
    val title: String,
    val description: String,
    val recommendation: String,
    val priority: String, // "CRITICAL", "WARNING", "INFO"
    val relatedCode: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("description", description)
            put("recommendation", recommendation)
            put("priority", priority)
            put("relatedCode", relatedCode ?: "")
        }
    }
}

data class TechnicianTrendDashboardData(
    val totalCases: Int,
    val totalUniqueCodes: Int,
    val mostFrequentCode: PanicCodeStat?,
    val mostFailingSubsystem: SubsystemFailureStat?,
    val averageConfidencePercent: Int,
    val panicCodeStats: List<PanicCodeStat>,
    val subsystemStats: List<SubsystemFailureStat>,
    val modelCorrelations: List<ModelFailureCorrelation>,
    val timelinePoints: List<TimelineTrendPoint>,
    val hardwareInsights: List<WorkshopHardwareInsight>
) {
    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("totalCases", totalCases)
            put("totalUniqueCodes", totalUniqueCodes)
            put("averageConfidencePercent", averageConfidencePercent)
            put("mostFrequentCode", mostFrequentCode?.toJson() ?: JSONObject.NULL)
            put("mostFailingSubsystem", mostFailingSubsystem?.toJson() ?: JSONObject.NULL)
            
            val codesArray = JSONArray()
            panicCodeStats.forEach { codesArray.put(it.toJson()) }
            put("panicCodeStats", codesArray)

            val subsArray = JSONArray()
            subsystemStats.forEach { subsArray.put(it.toJson()) }
            put("subsystemStats", subsArray)

            val modelsArray = JSONArray()
            modelCorrelations.forEach { modelsArray.put(it.toJson()) }
            put("modelCorrelations", modelsArray)

            val timelineArray = JSONArray()
            timelinePoints.forEach { timelineArray.put(it.toJson()) }
            put("timelinePoints", timelineArray)

            val insightsArray = JSONArray()
            hardwareInsights.forEach { insightsArray.put(it.toJson()) }
            put("hardwareInsights", insightsArray)
        }
        return root.toString()
    }
}
