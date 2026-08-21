package com.example.diagnostic

import com.example.util.HexUtils
import java.util.regex.Pattern

data class ExtractedSensors(
    val missingSensorTokens: List<String> = emptyList(),
    val smcSensorCodes: List<String> = emptyList(),
    val rawSensorArrayLines: List<String> = emptyList()
)

object SensorExtractor {

    fun extract(logText: String): ExtractedSensors {
        val missingSensors = mutableListOf<String>()
        val smcCodes = mutableListOf<String>()
        val rawArrayLines = mutableListOf<String>()

        val lines = logText.lines()

        for (line in lines) {
            // 1. Missing sensor(s): PRS0, Mic1, etc.
            if (line.contains("Missing sensor", ignoreCase = true)) {
                val matcher = Pattern.compile("Missing sensor\\(s\\)\\s*:\\s*([A-Za-z0-9_,\\s]+)", Pattern.CASE_INSENSITIVE).matcher(line)
                if (matcher.find()) {
                    val sensorsPart = matcher.group(1)
                    val tokens = sensorsPart.split(",", " ", "\t")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.length in 2..10 }
                    missingSensors.addAll(tokens)
                }
            }

            // 2. Sensor array lines: e.g. "S.sensor array 0 - 6 is 0x0, 0x1000, 0x0..."
            if (line.contains("sensor array", ignoreCase = true) || line.contains("sensor_array", ignoreCase = true)) {
                rawArrayLines.add(line.trim())
                // Extract all hex and decimal tokens
                val tokenMatcher = Pattern.compile("(0x[0-9a-fA-F]+|\\b[0-9]{2,10}\\b)").matcher(line)
                while (tokenMatcher.find()) {
                    val rawToken = tokenMatcher.group(1)
                    val longVal = HexUtils.parseCodeToLong(rawToken)
                    if (longVal != null && longVal > 0L) {
                        smcCodes.add(rawToken)
                    }
                }
            }
        }

        // Additional scan for SMC codes in panicString if single line format like "sensor array 0x1000" or "0x500000"
        if (smcCodes.isEmpty() && logText.contains("SMC", ignoreCase = true)) {
            val codeMatcher = Pattern.compile("\\b(0x[0-9a-fA-F]{2,8})\\b").matcher(logText)
            while (codeMatcher.find()) {
                val token = codeMatcher.group(1)
                val longVal = HexUtils.parseCodeToLong(token)
                if (longVal != null && longVal > 0L) {
                    smcCodes.add(token)
                }
            }
        }

        return ExtractedSensors(
            missingSensorTokens = missingSensors.distinct(),
            smcSensorCodes = smcCodes.distinct(),
            rawSensorArrayLines = rawArrayLines
        )
    }
}
