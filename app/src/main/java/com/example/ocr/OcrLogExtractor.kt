package com.example.ocr

import java.util.Locale
import java.util.regex.Pattern

/**
 * Data structure representing the analysis of OCR scanned text.
 */
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

/**
 * Utility responsible for sanitizing, repairing OCR optical errors (e.g., 'O' vs '0', 'l' vs '1' in hex),
 * and structuring raw camera text captures into deterministic-ready Panic Full logs.
 */
object OcrLogExtractor {

    // Common hex sensor patterns and codes
    private val HEX_CODE_PATTERN = Pattern.compile("0x[0-9A-Fa-f]{3,8}", Pattern.CASE_INSENSITIVE)
    private val DECIMAL_SENSOR_PATTERN = Pattern.compile("\\b(4194304|524288|262144|131072|65536|32768|16384|8192|4096|2048|1024)\\b")
    private val IPHONE_MODEL_PATTERN = Pattern.compile("\\biPhone\\s*([0-9]{1,2}(?:,[0-9])?|\\d+\\s*(?:Pro(?:\\s*Max)?|Plus|Mini)?)\\b", Pattern.CASE_INSENSITIVE)
    private val BUILD_PATTERN = Pattern.compile("\\b([12][0-9][A-Z][0-9]{2,4}[a-z]?)\\b")
    private val SENSOR_ARRAY_PATTERN = Pattern.compile("(?:S\\.?\\s*sensor\\s*array|sensor\\s*array\\s*0\\s*-\\s*5)", Pattern.CASE_INSENSITIVE)

    /**
     * Cleans OCR artifacts and extracts panic signatures from raw text captured by camera or image OCR.
     */
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

        val lines = rawText.lines()
        val cleanedLines = mutableListOf<String>()
        val detectedKeywords = mutableListOf<String>()
        val detectedPanicCodes = mutableSetOf<String>()

        var detectedDeviceModel: String? = null
        var detectedBuild: String? = null

        for (line in lines) {
            var cleaned = line.trim()
            if (cleaned.isEmpty()) continue

            // Repair common OCR misreadings for hex addresses (e.g., 'Ox400000', '0X4OOOOO')
            cleaned = repairOcrHexArtifacts(cleaned)

            // Detect iPhone product models
            val modelMatcher = IPHONE_MODEL_PATTERN.matcher(cleaned)
            if (modelMatcher.find() && detectedDeviceModel == null) {
                detectedDeviceModel = modelMatcher.group(0)
                detectedKeywords.add("Dispositivo: $detectedDeviceModel")
            }

            // Detect iOS builds
            val buildMatcher = BUILD_PATTERN.matcher(cleaned)
            if (buildMatcher.find() && detectedBuild == null) {
                detectedBuild = buildMatcher.group(1)
                detectedKeywords.add("Build: $detectedBuild")
            }

            // Detect Hex codes
            val hexMatcher = HEX_CODE_PATTERN.matcher(cleaned)
            while (hexMatcher.find()) {
                val hex = hexMatcher.group(0).uppercase(Locale.ROOT)
                detectedPanicCodes.add(hex)
            }

            // Detect Decimal codes
            val decMatcher = DECIMAL_SENSOR_PATTERN.matcher(cleaned)
            while (decMatcher.find()) {
                val dec = decMatcher.group(0)
                try {
                    val decLong = dec.toLong()
                    val hexFromDec = "0x" + decLong.toString(16).uppercase(Locale.ROOT)
                    detectedPanicCodes.add(hexFromDec)
                } catch (_: Exception) {}
            }

            // Detect known panic families
            val upperLine = cleaned.uppercase(Locale.ROOT)
            if (upperLine.contains("SMC PANIC") || upperLine.contains("BSC FAILURE")) {
                if (!detectedKeywords.contains("SMC BSC Failure")) {
                    detectedKeywords.add("SMC BSC Failure")
                }
            }
            if (upperLine.contains("I2C") || upperLine.contains("TIMEOUT")) {
                if (!detectedKeywords.contains("I2C Bus Timeout")) {
                    detectedKeywords.add("I2C Bus Timeout")
                }
            }
            if (upperLine.contains("WATCHDOG") || upperLine.contains("WDT")) {
                if (!detectedKeywords.contains("Watchdog Reset")) {
                    detectedKeywords.add("Watchdog Reset")
                }
            }
            if (upperLine.contains("THERMAL") || upperLine.contains("PRESSURE")) {
                if (!detectedKeywords.contains("Sensor Térmico")) {
                    detectedKeywords.add("Sensor Térmico")
                }
            }
            if (upperLine.contains("AOP PANIC") || upperLine.contains("ALWAYS ON")) {
                if (!detectedKeywords.contains("AOP Panic")) {
                    detectedKeywords.add("AOP Panic")
                }
            }
            if (upperLine.contains("SEP PANIC") || upperLine.contains("SECURE ENCLAVE")) {
                if (!detectedKeywords.contains("SEP Panic")) {
                    detectedKeywords.add("SEP Panic")
                }
            }

            cleanedLines.add(cleaned)
        }

        // Add detected codes to keyword badges
        detectedPanicCodes.forEach { code ->
            detectedKeywords.add("Código: $code")
        }

        // Construct standardized log text for deterministic parser
        val formattedLog = buildSanitizedPanicLog(
            cleanedLines = cleanedLines,
            detectedDevice = detectedDeviceModel,
            detectedBuild = detectedBuild
        )

        val hasValidPanicSignatures = detectedPanicCodes.isNotEmpty() ||
                detectedKeywords.any { it.contains("SMC") || it.contains("I2C") || it.contains("Watchdog") || it.contains("Térmico") } ||
                cleanedLines.any { SENSOR_ARRAY_PATTERN.matcher(it).find() }

        val confidenceHint = when {
            detectedPanicCodes.isNotEmpty() && (detectedKeywords.size >= 2) -> "Excelente captura (Patrones y Códigos Hex identificados)"
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

    /**
     * Fixes optical OCR character confusions in hex registers (e.g. 'Ox', '0X4OOOOO' -> '0x400000').
     */
    private fun repairOcrHexArtifacts(line: String): String {
        var res = line
        // Fix uppercase letter O in place of 0 in hex-like tokens
        val hexLikePattern = Pattern.compile("\\b(?:0|O|o)(?:x|X)([0-9A-Fa-fOoIl]{3,8})\\b")
        val matcher = hexLikePattern.matcher(res)
        val sb = StringBuffer()
        while (matcher.find()) {
            var inner = matcher.group(1)
            inner = inner.replace('O', '0').replace('o', '0')
            inner = inner.replace('l', '1').replace('I', '1')
            matcher.appendReplacement(sb, "0x$inner")
        }
        matcher.appendTail(sb)
        res = sb.toString()

        // Fix sensor array spacing e.g. "S . sensor array" -> "S.sensor array"
        res = res.replace(Regex("S\\s*\\.\\s*sensor\\s*array", RegexOption.IGNORE_CASE), "S.sensor array")
        return res
    }

    /**
     * Formats reconstructed lines into a proper log representation for the engine.
     */
    private fun buildSanitizedPanicLog(
        cleanedLines: List<String>,
        detectedDevice: String?,
        detectedBuild: String?
    ): String {
        // If the text already has headers or JSON structure, keep as is
        val rawJoined = cleanedLines.joinToString("\n")
        if (rawJoined.contains("{\"bug_type\"") || (rawJoined.contains("panic(") && rawJoined.contains("macOS version"))) {
            return rawJoined
        }

        // If it's a snippet from screen/camera, wrap with minimal valid IPS context if needed
        return buildString {
            if (!rawJoined.contains("product", ignoreCase = true) && detectedDevice != null) {
                appendLine("{\"product\":\"$detectedDevice\",\"build\":\"${detectedBuild ?: "21D61"}\",\"incident\":\"OCR_SCAN\"}")
            }
            append(rawJoined)
        }
    }
}
