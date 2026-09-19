package com.example.diagnostic

import com.example.domain.model.SensorCode
import com.example.util.HexUtils
import java.util.regex.Pattern

data class ExtractedSensors(
    val missingSensorTokens: List<String> = emptyList(),
    val smcSensorCodes: List<String> = emptyList(),
    val sensorCodes: List<SensorCode> = emptyList(),
    val rawSensorArrayLines: List<String> = emptyList()
)

object SensorExtractor {

    private val SENSOR_ARRAY_PAYLOAD_PATTERN = Pattern.compile(
        """(?:S\.)?sensor[ _]array(?:\s+\d+\s*-\s*\d+)?\s*(?:is|:|=)\s*([^\n\r"]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val MISSING_SENSOR_PATTERN = Pattern.compile(
        """Missing sensor\(s\)\s*:\s*([A-Za-z0-9_,\s]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val VALUE_TOKEN_PATTERN = Pattern.compile("""(0x[0-9a-fA-F]+|\b\d+\b)""")

    fun extract(logText: String, panicString: String? = null): ExtractedSensors {
        val missingSensors = mutableListOf<String>()
        val smcCodes = mutableListOf<String>()
        val sensorCodes = mutableListOf<SensorCode>()
        val rawArrayLines = mutableListOf<String>()

        val combinedText = buildString {
            append(logText.replace("\\n", "\n").replace("\\r", "\r"))
            if (!panicString.isNullOrBlank()) {
                append("\n").append(panicString.replace("\\n", "\n").replace("\\r", "\r"))
            }
        }

        val lines = combinedText.lines()

        for (line in lines) {
            val trimmedLine = line.trim()

            // 1. Missing sensor(s): PRS0, Mic1, etc.
            if (trimmedLine.contains("Missing sensor", ignoreCase = true)) {
                val matcher = MISSING_SENSOR_PATTERN.matcher(trimmedLine)
                if (matcher.find()) {
                    val sensorsPart = matcher.group(1)
                    if (sensorsPart != null) {
                        val tokens = sensorsPart.split(",", " ", "\t")
                            .map { it.trim() }
                            .filter { it.isNotBlank() && it.length in 2..10 }
                        missingSensors.addAll(tokens)
                    }
                }
            }

            // 2. Sensor array lines: e.g. "S.sensor array 0 - N is ..." or "sensor_array: ..."
            if (trimmedLine.contains("sensor array", ignoreCase = true) || trimmedLine.contains("sensor_array", ignoreCase = true)) {
                rawArrayLines.add(trimmedLine)

                val arrayMatcher = SENSOR_ARRAY_PAYLOAD_PATTERN.matcher(trimmedLine)
                val payload = if (arrayMatcher.find()) {
                    arrayMatcher.group(1)
                } else {
                    val isIdx = trimmedLine.indexOf(" is ", ignoreCase = true)
                    val colonIdx = trimmedLine.indexOf(":")
                    val eqIdx = trimmedLine.indexOf("=")
                    val splitIdx = when {
                        isIdx >= 0 -> isIdx + 4
                        colonIdx >= 0 -> colonIdx + 1
                        eqIdx >= 0 -> eqIdx + 1
                        else -> 0
                    }
                    trimmedLine.substring(splitIdx)
                }

                if (!payload.isNullOrBlank()) {
                    val tokenMatcher = VALUE_TOKEN_PATTERN.matcher(payload)
                    while (tokenMatcher.find()) {
                        val rawToken = tokenMatcher.group(1)
                        if (rawToken != null) {
                            val sensorCode = SensorCode.parse(rawToken)
                            if (sensorCode != null && sensorCode.numericValue > 0L) {
                                smcCodes.add(sensorCode.rawValue)
                                sensorCodes.add(sensorCode)
                            }
                        }
                    }
                }
            }
        }

        // 3. Fallback scan for standalone SMC codes in panic lines if no array values found
        if (smcCodes.isEmpty() && combinedText.contains("SMC", ignoreCase = true)) {
            val codeMatcher = Pattern.compile("""\b(0x[0-9a-fA-F]{2,8})\b""").matcher(combinedText)
            while (codeMatcher.find()) {
                val token = codeMatcher.group(1)
                if (token != null) {
                    val sensorCode = SensorCode.parse(token)
                    if (sensorCode != null && sensorCode.numericValue > 0L) {
                        smcCodes.add(sensorCode.rawValue)
                        sensorCodes.add(sensorCode)
                    }
                }
            }
        }

        return ExtractedSensors(
            missingSensorTokens = missingSensors.distinct(),
            smcSensorCodes = smcCodes.distinct(),
            sensorCodes = sensorCodes.distinctBy { it.numericValue },
            rawSensorArrayLines = rawArrayLines.distinct()
        )
    }
}
