package com.example.parser

import com.example.domain.model.ParsedMetadata
import org.json.JSONObject
import java.util.regex.Pattern

object MetadataExtractor {

    fun extract(normalizedLog: String): ParsedMetadata {
        if (normalizedLog.isBlank()) {
            return ParsedMetadata()
        }

        // 1. Try parsing as complete JSON
        val jsonResult = tryParseJson(normalizedLog)
        if (jsonResult != null && (jsonResult.product != null || jsonResult.panicString != null)) {
            return jsonResult
        }

        // 2. Fallback to Regex extraction across lines/text
        return extractWithRegex(normalizedLog)
    }

    private fun tryParseJson(text: String): ParsedMetadata? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }

        return try {
            val json = JSONObject(trimmed)
            val product = json.optString("product").takeIf { it.isNotBlank() }
                ?: json.optString("model").takeIf { it.isNotBlank() }
            val osVersion = json.optString("os_version").takeIf { it.isNotBlank() }
                ?: json.optString("osVersion").takeIf { it.isNotBlank() }
            val build = json.optString("build").takeIf { it.isNotBlank() }
            val bugType = json.optString("bug_type").takeIf { it.isNotBlank() }
            val incidentId = json.optString("incident_id").takeIf { it.isNotBlank() }
                ?: json.optString("incident").takeIf { it.isNotBlank() }
            val crashReporterKey = json.optString("crashReporterKey").takeIf { it.isNotBlank() }
            val panicInitiator = json.optString("panicInitiator").takeIf { it.isNotBlank() }
            val panicString = json.optString("panicString").takeIf { it.isNotBlank() }
            val kernel = json.optString("kernel").takeIf { it.isNotBlank() }
            val socId = json.optString("socId").takeIf { it.isNotBlank() }
            val timestamp = json.optString("timestamp").takeIf { it.isNotBlank() }
            val repairStatus = json.optString("repairStatus").takeIf { it.isNotBlank() }
            val rootsInstalled = json.optString("roots_installed").takeIf { it.isNotBlank() }

            ParsedMetadata(
                product = product,
                osVersion = osVersion,
                build = build,
                bugType = bugType,
                incidentId = incidentId,
                crashReporterKey = crashReporterKey,
                panicInitiator = panicInitiator,
                panicString = panicString,
                kernel = kernel,
                socId = socId,
                timestamp = timestamp,
                repairStatus = repairStatus,
                rootsInstalled = rootsInstalled
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractWithRegex(text: String): ParsedMetadata {
        val product = findRegexMatch(text, "(?:\"product\"\\s*:\\s*\"|product\\s*:\\s*|product\\s+|Product:\\s*)(iPhone\\d+,\\d+)")
            ?: findRegexMatch(text, "\\b(iPhone\\d+,\\d+)\\b")

        val osVersion = findRegexMatch(text, "(?:\"os_version\"\\s*:\\s*\"|os_version\\s*:\\s*|OS Version:\\s*)([^\"\\n,]+)")
        val build = findRegexMatch(text, "(?:\"build\"\\s*:\\s*\"|build\\s*:\\s*|Build:\\s*)([^\"\\n,]+)")
            ?: findRegexMatch(text, "\\(([0-9A-Z]{5,7})\\)")

        val bugType = findRegexMatch(text, "(?:\"bug_type\"\\s*:\\s*\"|bug_type\\s*:\\s*)([^\"\\n,]+)")
        val incidentId = findRegexMatch(text, "(?:\"incident_id\"\\s*:\\s*\"|Incident Identifier:\\s*)([^\"\\n,]+)")
        val crashReporterKey = findRegexMatch(text, "(?:\"crashReporterKey\"\\s*:\\s*\"|CrashReporter Key:\\s*)([^\"\\n,]+)")
        val panicInitiator = findRegexMatch(text, "(?:\"panicInitiator\"\\s*:\\s*\"|panicInitiator\\s*:\\s*)([^\"\\n,]+)")

        val panicString = extractPanicString(text)

        val kernel = findRegexMatch(text, "(?:\"kernel\"\\s*:\\s*\"|Kernel Version:\\s*|Darwin Kernel Version\\s*)([^\"\\n]+)")
        val socId = findRegexMatch(text, "(?:\"socId\"\\s*:\\s*\"|socId\\s*:\\s*)([^\"\\n,]+)")
        val timestamp = findRegexMatch(text, "(?:\"timestamp\"\\s*:\\s*\"|Date/Time:\\s*|timestamp\\s*:\\s*)([^\"\\n,]+)")
        val repairStatus = findRegexMatch(text, "(?:\"repairStatus\"\\s*:\\s*\"|repairStatus\\s*:\\s*)([^\"\\n,]+)")
        val rootsInstalled = findRegexMatch(text, "(?:\"roots_installed\"\\s*:\\s*\"|roots_installed\\s*:\\s*)([^\"\\n,]+)")

        return ParsedMetadata(
            product = product?.trim(),
            osVersion = osVersion?.trim(),
            build = build?.trim(),
            bugType = bugType?.trim(),
            incidentId = incidentId?.trim(),
            crashReporterKey = crashReporterKey?.trim(),
            panicInitiator = panicInitiator?.trim(),
            panicString = panicString?.trim(),
            kernel = kernel?.trim(),
            socId = socId?.trim(),
            timestamp = timestamp?.trim(),
            repairStatus = repairStatus?.trim(),
            rootsInstalled = rootsInstalled?.trim()
        )
    }

    private fun extractPanicString(text: String): String? {
        // Match JSON panicString value if partially present
        val jsonPanicPattern = Pattern.compile("\"panicString\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL)
        val jsonMatcher = jsonPanicPattern.matcher(text)
        if (jsonMatcher.find()) {
            val group = jsonMatcher.group(1)
            if (group != null) {
                return group.replace("\\n", "\n").replace("\\\"", "\"")
            }
        }

        // Match panic(...) or "panicString" or "panic(" header block
        val panicBlockPattern = Pattern.compile("(panic\\(.*?\\):.*?)(?:\\n\\n|Debugger message|Backtrace:|\$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val blockMatcher = panicBlockPattern.matcher(text)
        if (blockMatcher.find()) {
            val group = blockMatcher.group(1)
            if (group != null) {
                return group.trim()
            }
        }

        // Match "SMC PANIC" or "Missing sensor(s)" or "userspace watchdog timeout" line
        val lines = text.lines()
        val panicLines = lines.filter { line ->
            line.contains("panic", ignoreCase = true) ||
            line.contains("Missing sensor", ignoreCase = true) ||
            line.contains("SMC BSC", ignoreCase = true) ||
            line.contains("S.sensor array", ignoreCase = true) ||
            line.contains("watchdog timeout", ignoreCase = true) ||
            line.contains("AOP PANIC", ignoreCase = true) ||
            line.contains("ANS2", ignoreCase = true)
        }

        if (panicLines.isNotEmpty()) {
            return panicLines.take(6).joinToString("\n")
        }

        return null
    }

    private fun findRegexMatch(text: String, regexPattern: String): String? {
        return try {
            val pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                matcher.group(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
