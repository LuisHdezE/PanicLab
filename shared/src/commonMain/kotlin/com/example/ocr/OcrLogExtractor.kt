package com.example.ocr

data class OcrScanResult(
    val rawText: String,
    val cleanedText: String,
    val detectedDeviceModel: String?,
    val detectedBuild: String?,
    val detectedPanicCodes: List<String>,
    val detectedKeywords: List<String>,
    val lineCount: Int,
    val hasValidPanicSignatures: Boolean,
    val confidenceHint: String
)

object OcrLogExtractor {
    private val hexCodeRegex = Regex("0x[0-9A-Fa-f]{3,8}", RegexOption.IGNORE_CASE)
    private val decimalSensorRegex = Regex("\\b(4194304|524288|262144|131072|65536|32768|16384|8192|4096|2048|1024)\\b")
    private val iphoneModelRegex = Regex("\\biPhone\\s*([0-9]{1,2}(?:,[0-9])?|\\d+\\s*(?:Pro(?:\\s*Max)?|Plus|Mini)?)\\b", RegexOption.IGNORE_CASE)
    private val buildRegex = Regex("\\b([12][0-9][A-Z][0-9]{2,4}[a-z]?)\\b")
    private val sensorArrayRegex = Regex("(?:S\\.?\\s*sensor\\s*array|sensor\\s*array\\s*0\\s*-\\s*5)", RegexOption.IGNORE_CASE)
    private val hexLikeRegex = Regex("\\b(?:0|O|o)(?:x|X)([0-9A-Fa-fOoIl]{3,8})\\b")
    private val sensorArraySpacingRegex = Regex("S\\s*\\.\\s*sensor\\s*array", RegexOption.IGNORE_CASE)

    fun processScannedText(rawText: String): OcrScanResult {
        if (rawText.isBlank()) {
            return OcrScanResult(
                rawText = "",
                cleanedText = "",
                detectedDeviceModel = null,
                detectedBuild = null,
                detectedPanicCodes = emptyList(),
                detectedKeywords = emptyList(),
                lineCount = 0,
                hasValidPanicSignatures = false,
                confidenceHint = "Sin texto detectado"
            )
        }

        val cleanedLines = mutableListOf<String>()
        val detectedKeywords = mutableListOf<String>()
        val detectedPanicCodes = linkedSetOf<String>()
        var detectedDeviceModel: String? = null
        var detectedBuild: String? = null

        for (line in rawText.lines()) {
            var cleaned = line.trim()
            if (cleaned.isEmpty()) continue
            cleaned = repairOcrHexArtifacts(cleaned)

            if (detectedDeviceModel == null) {
                val model = iphoneModelRegex.find(cleaned)?.value
                if (model != null) {
                    detectedDeviceModel = model
                    detectedKeywords += "Dispositivo: $model"
                }
            }

            if (detectedBuild == null) {
                val build = buildRegex.find(cleaned)?.groupValues?.getOrNull(1)
                if (build != null) {
                    detectedBuild = build
                    detectedKeywords += "Build: $build"
                }
            }

            hexCodeRegex.findAll(cleaned).forEach { detectedPanicCodes += it.value.uppercase() }
            decimalSensorRegex.findAll(cleaned).forEach { match ->
                match.value.toLongOrNull()?.let { value ->
                    detectedPanicCodes += "0x${value.toString(16).uppercase()}"
                }
            }

            val upperLine = cleaned.uppercase()
            addKeywordIf(upperLine.contains("SMC PANIC") || upperLine.contains("BSC FAILURE"), "SMC BSC Failure", detectedKeywords)
            addKeywordIf(upperLine.contains("I2C") || upperLine.contains("TIMEOUT"), "I2C Bus Timeout", detectedKeywords)
            addKeywordIf(upperLine.contains("WATCHDOG") || upperLine.contains("WDT"), "Watchdog Reset", detectedKeywords)
            addKeywordIf(upperLine.contains("THERMAL") || upperLine.contains("PRESSURE"), "Sensor Térmico", detectedKeywords)
            addKeywordIf(upperLine.contains("AOP PANIC") || upperLine.contains("ALWAYS ON"), "AOP Panic", detectedKeywords)
            addKeywordIf(upperLine.contains("SEP PANIC") || upperLine.contains("SECURE ENCLAVE"), "SEP Panic", detectedKeywords)

            cleanedLines += cleaned
        }

        detectedPanicCodes.forEach { detectedKeywords += "Código: $it" }

        val formattedLog = buildSanitizedPanicLog(cleanedLines, detectedDeviceModel, detectedBuild)
        val hasValidPanicSignatures = detectedPanicCodes.isNotEmpty() ||
            detectedKeywords.any {
                it.contains("SMC") || it.contains("I2C") || it.contains("Watchdog") || it.contains("Térmico")
            } ||
            cleanedLines.any { sensorArrayRegex.containsMatchIn(it) }

        val confidenceHint = when {
            detectedPanicCodes.isNotEmpty() && detectedKeywords.size >= 2 -> "Excelente captura (Patrones y Códigos Hex identificados)"
            detectedPanicCodes.isNotEmpty() -> "Buena captura (Código de sensor identificado)"
            hasValidPanicSignatures -> "Captura parcial (Palabras clave de pánico detectadas)"
            cleanedLines.size > 5 -> "Texto detectado (Requiere confirmar códigos de sensor)"
            else -> "Captura débil (Apunta directamente a las líneas de pánico)"
        }

        return OcrScanResult(
            rawText = rawText,
            cleanedText = formattedLog,
            detectedDeviceModel = detectedDeviceModel,
            detectedBuild = detectedBuild,
            detectedPanicCodes = detectedPanicCodes.toList(),
            detectedKeywords = detectedKeywords.distinct(),
            lineCount = cleanedLines.size,
            hasValidPanicSignatures = hasValidPanicSignatures,
            confidenceHint = confidenceHint
        )
    }

    private fun addKeywordIf(condition: Boolean, keyword: String, target: MutableList<String>) {
        if (condition && keyword !in target) target += keyword
    }

    private fun repairOcrHexArtifacts(line: String): String {
        val repairedHex = hexLikeRegex.replace(line) { match ->
            val inner = match.groupValues[1]
                .replace('O', '0')
                .replace('o', '0')
                .replace('l', '1')
                .replace('I', '1')
            "0x$inner"
        }
        return sensorArraySpacingRegex.replace(repairedHex, "S.sensor array")
    }

    private fun buildSanitizedPanicLog(
        cleanedLines: List<String>,
        detectedDevice: String?,
        detectedBuild: String?
    ): String {
        val rawJoined = cleanedLines.joinToString("\n")
        if (rawJoined.contains("{\"bug_type\"") ||
            (rawJoined.contains("panic(") && rawJoined.contains("macOS version"))
        ) {
            return rawJoined
        }

        return buildString {
            if (!rawJoined.contains("product", ignoreCase = true) && detectedDevice != null) {
                appendLine("{\"product\":\"$detectedDevice\",\"build\":\"${detectedBuild ?: "21D61"}\",\"incident\":\"OCR_SCAN\"}")
            }
            append(rawJoined)
        }
    }
}
