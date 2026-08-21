package com.example.util

import com.example.domain.model.DiagnosticReport
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

object UnknownCaseRedactor {

    // Regex patterns for private/sensitive identification data
    private val SERIAL_REGEX = Pattern.compile("\\b([A-Z0-9]{10,12})\\b")
    private val MAC_ADDRESS_REGEX = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
    private val UUID_REGEX = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val USER_PATH_REGEX = Pattern.compile("/(Users|private/var/mobile/Containers/Data/Application)/[a-zA-Z0-9_-]+")
    private val IMEI_REGEX = Pattern.compile("\\b\\d{15}\\b")
    private val PHONE_NUMBER_REGEX = Pattern.compile("\\+?\\d{10,14}")

    fun redactLog(rawText: String?): String {
        if (rawText.isNullOrBlank()) return ""
        var redacted = rawText

        redacted = UUID_REGEX.matcher(redacted).replaceAll("[REDACTED_UUID]")
        redacted = MAC_ADDRESS_REGEX.matcher(redacted).replaceAll("[REDACTED_MAC]")
        redacted = USER_PATH_REGEX.matcher(redacted).replaceAll("/[REDACTED_PATH]")
        redacted = IMEI_REGEX.matcher(redacted).replaceAll("[REDACTED_IMEI]")
        redacted = PHONE_NUMBER_REGEX.matcher(redacted).replaceAll("[REDACTED_PHONE]")

        return redacted
    }

    fun sanitizeRawLog(rawText: String?): String = redactLog(rawText)

    fun generateUnknownCaseJson(
        report: DiagnosticReport,
        contributorNotes: String? = null
    ): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val exportedAt = isoFormat.format(Date())

        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("type", "PANICLAB_UNKNOWN_CASE_REPORT")
        root.put("exportedAt", exportedAt)
        root.put("generator", "PanicLab Android v1.0.0")

        val deviceJson = JSONObject().apply {
            put("productCode", report.productCode)
            put("marketingName", report.deviceModel?.marketingName ?: "Desconocido")
            put("family", report.deviceModel?.family ?: "UNKNOWN")
            put("diagnosticProfile", report.deviceModel?.diagnosticProfile ?: "UNKNOWN")
            put("osVersion", report.osVersion)
            put("build", report.build)
        }
        root.put("device", deviceJson)

        val panicInfo = JSONObject().apply {
            put("families", JSONArray(report.panicFamilies.map { it.name }))
            put("panicStringSummary", report.panicStringSummary)
            put("confidenceAtAnalysis", report.confidence.name)
            put("verificationStatusAtAnalysis", report.verificationStatus.name)
            put("kbVersionAtAnalysis", report.knowledgeBaseVersion)
        }
        root.put("panicDetails", panicInfo)

        val evidencesArray = JSONArray()
        report.evidences.forEach { ev ->
            val evObj = JSONObject().apply {
                put("type", ev.type)
                put("title", ev.title)
                put("rawValue", ev.rawValue)
                put("normalizedValue", ev.normalizedValue)
                put("lineNumber", ev.lineNumber)
            }
            evidencesArray.put(evObj)
        }
        root.put("evidences", evidencesArray)

        // Sanitize raw log excerpt (max 3000 chars around key findings)
        val sanitizedRawLog = redactLog(report.rawLog)
        val logExcerpt = if (sanitizedRawLog.length > 4000) {
            sanitizedRawLog.take(4000) + "\n... [TRUNCADO PARA REPORTE COMUNITARIO] ..."
        } else {
            sanitizedRawLog
        }
        root.put("sanitizedLogExcerpt", logExcerpt)

        val technicianSection = JSONObject().apply {
            put("notes", contributorNotes ?: "Caso no catalogado exportado para ampliación de la base de conocimiento de PanicLab.")
            put("verifiedHardwareSwapAttempted", false)
        }
        root.put("technicianReport", technicianSection)

        return root.toString(2)
    }
}
