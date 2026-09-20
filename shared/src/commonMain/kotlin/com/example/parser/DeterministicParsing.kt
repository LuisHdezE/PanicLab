package com.example.parser

import com.example.diagnostic.ExtractedSensors
import com.example.domain.model.DeviceModel
import com.example.domain.model.DiagnosticEvidence
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.platform.IdGenerator
import com.example.util.HexUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MetadataExtractor {

    fun extract(normalizedLog: String): ParsedMetadata {
        if (normalizedLog.isBlank()) return ParsedMetadata()

        val jsonResult = tryParseJson(normalizedLog)
        if (jsonResult != null && (jsonResult.product != null || jsonResult.panicString != null)) {
            return jsonResult
        }

        return extractWithRegex(normalizedLog)
    }

    private fun tryParseJson(text: String): ParsedMetadata? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        return try {
            val json = Json.parseToJsonElement(trimmed).jsonObject
            ParsedMetadata(
                product = json.string("product") ?: json.string("model"),
                osVersion = json.string("os_version") ?: json.string("osVersion"),
                build = json.string("build"),
                bugType = json.string("bug_type"),
                incidentId = json.string("incident_id") ?: json.string("incident"),
                crashReporterKey = json.string("crashReporterKey"),
                panicInitiator = json.string("panicInitiator"),
                panicString = json.string("panicString"),
                kernel = json.string("kernel"),
                socId = json.string("socId"),
                timestamp = json.string("timestamp"),
                repairStatus = json.string("repairStatus"),
                rootsInstalled = json.string("roots_installed")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

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
            panicString = extractPanicString(text)?.trim(),
            kernel = kernel?.trim(),
            socId = socId?.trim(),
            timestamp = timestamp?.trim(),
            repairStatus = repairStatus?.trim(),
            rootsInstalled = rootsInstalled?.trim()
        )
    }

    private fun extractPanicString(text: String): String? {
        val jsonPanic = extractJsonStringValue(text, "panicString")
        if (!jsonPanic.isNullOrEmpty()) {
            return jsonPanic
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        val block = extractPanicBlock(text)
        if (!block.isNullOrBlank()) return block

        val panicLines = text.lineSequence().filter { line ->
            line.contains("panic", ignoreCase = true) ||
                line.contains("Missing sensor", ignoreCase = true) ||
                line.contains("SMC BSC", ignoreCase = true) ||
                line.contains("S.sensor array", ignoreCase = true) ||
                line.contains("watchdog timeout", ignoreCase = true) ||
                line.contains("AOP PANIC", ignoreCase = true) ||
                line.contains("ANS2", ignoreCase = true)
        }.take(6).toList()
        return panicLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun extractJsonStringValue(text: String, key: String): String? {
        val quotedKey = "\"$key\""
        val keyIndex = text.indexOf(quotedKey, ignoreCase = true)
        if (keyIndex < 0) return null

        var cursor = keyIndex + quotedKey.length
        while (cursor < text.length && text[cursor].isWhitespace()) cursor += 1
        if (cursor >= text.length || text[cursor] != ':') return null

        cursor += 1
        while (cursor < text.length && text[cursor].isWhitespace()) cursor += 1
        if (cursor >= text.length || text[cursor] != '"') return null

        cursor += 1
        val valueStart = cursor
        var escaped = false
        while (cursor < text.length) {
            val current = text[cursor]
            if (escaped) {
                escaped = false
                cursor += 1
                continue
            }
            if (current == '\\') {
                escaped = true
                cursor += 1
                continue
            }
            if (current == '"') {
                return text.substring(valueStart, cursor)
            }
            cursor += 1
        }
        return null
    }

    private fun extractPanicBlock(text: String): String? {
        val panicStart = text.indexOf("panic(", ignoreCase = true)
        if (panicStart < 0) return null

        val headerEnd = text.indexOf("):", startIndex = panicStart)
        if (headerEnd < 0) return null

        val contentStart = headerEnd + 2
        var blockEnd = text.length

        val doubleNewline = text.indexOf("\n\n", startIndex = contentStart)
        if (doubleNewline >= 0) blockEnd = minOf(blockEnd, doubleNewline)

        val debuggerMessage = text.indexOf("Debugger message", startIndex = contentStart, ignoreCase = true)
        if (debuggerMessage >= 0) blockEnd = minOf(blockEnd, debuggerMessage)

        val backtrace = text.indexOf("Backtrace:", startIndex = contentStart, ignoreCase = true)
        if (backtrace >= 0) blockEnd = minOf(blockEnd, backtrace)

        return text.substring(panicStart, blockEnd).trim()
    }

    private fun findRegexMatch(text: String, pattern: String): String? = try {
        Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
    } catch (_: Exception) {
        null
    }
}

object PanicClassifier {

    private val missingSensorRegex = Regex("Missing sensor\\(s\\)\\s*:", RegexOption.IGNORE_CASE)
    private val i2cRegex = Regex("\\bi2c\\d*\\b|\\bi²c\\b", RegexOption.IGNORE_CASE)
    private val dcpRegex = Regex("\\bDCP\\b|iomfb|display coprocessor", RegexOption.IGNORE_CASE)
    private val basebandRegex = Regex("\\bbaseband\\b|AppleBaseband|\\bBB[A-Z_]*\\b", RegexOption.IGNORE_CASE)

    fun classify(logText: String, panicString: String?): List<PanicFamily> {
        val combinedText = buildString {
            append(logText)
            if (!panicString.isNullOrBlank()) append("\n").append(panicString)
        }

        val families = linkedSetOf<PanicFamily>()

        if (combinedText.contains("thermalmonitord", ignoreCase = true) && missingSensorRegex.containsMatchIn(combinedText)) {
            families.add(PanicFamily.THERMAL_MISSING_SENSOR)
        }
        if (combinedText.contains("SMC", ignoreCase = true) && combinedText.contains("BSC failure", ignoreCase = true)) {
            families.add(PanicFamily.SMC_BSC_FAILURE)
        }
        if (combinedText.contains("SMC", ignoreCase = true) && combinedText.contains("ASSERT", ignoreCase = true)) {
            families.add(PanicFamily.SMC_ASSERTION)
        }
        if (combinedText.contains("AOP", ignoreCase = true) && combinedText.contains("NMI POWER", ignoreCase = true)) {
            families.add(PanicFamily.AOP_NMI_POWER)
        }
        if (combinedText.contains("Bosch", ignoreCase = true) &&
            combinedText.contains("control channel", ignoreCase = true) &&
            combinedText.contains("write failure", ignoreCase = true)
        ) {
            families.add(PanicFamily.AOP_BOSCH_CONTROL)
        }
        if (combinedText.contains("ANS2", ignoreCase = true)) families.add(PanicFamily.ANS2)
        if (combinedText.contains("AppleSocHot", ignoreCase = true) || combinedText.contains("Hot Hot Hot", ignoreCase = true)) {
            families.add(PanicFamily.APPLE_SOC_HOT)
        }
        if (combinedText.contains("SEP ROM", ignoreCase = true)) families.add(PanicFamily.SEP_ROM_BOOT)
        if (combinedText.contains("Undefined Kernel Instruction", ignoreCase = true)) {
            families.add(PanicFamily.UNDEFINED_KERNEL_INSTRUCTION)
        }
        if (i2cRegex.containsMatchIn(combinedText)) families.add(PanicFamily.I2C)
        if (dcpRegex.containsMatchIn(combinedText)) families.add(PanicFamily.DCP_DISPLAY)
        if (basebandRegex.containsMatchIn(combinedText)) families.add(PanicFamily.BASEBAND)
        if ((combinedText.contains("watchdog timeout", ignoreCase = true) ||
                combinedText.contains("userspace watchdog timeout", ignoreCase = true)) &&
            !missingSensorRegex.containsMatchIn(combinedText)
        ) {
            families.add(PanicFamily.WATCHDOG_NO_CHECKIN)
        }

        if (families.isEmpty()) families.add(PanicFamily.UNKNOWN)
        return families.toList()
    }
}

object DeviceResolver {
    private val staticDeviceMap: Map<String, DeviceModel> = mapOf(
        "iPhone10,3" to DeviceModel("iPhone10,3", "iPhone X", "IPHONE_X", "X", "THERMAL_CLASSIC_X_TO_12", 2017),
        "iPhone10,6" to DeviceModel("iPhone10,6", "iPhone X", "IPHONE_X", "X", "THERMAL_CLASSIC_X_TO_12", 2017),
        "iPhone11,2" to DeviceModel("iPhone11,2", "iPhone XS", "IPHONE_XS", "XS", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,4" to DeviceModel("iPhone11,4", "iPhone XS Max", "IPHONE_XS_MAX", "XS_MAX", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,6" to DeviceModel("iPhone11,6", "iPhone XS Max", "IPHONE_XS_MAX", "XS_MAX", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,8" to DeviceModel("iPhone11,8", "iPhone XR", "IPHONE_XR", "XR", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone12,1" to DeviceModel("iPhone12,1", "iPhone 11", "IPHONE_11", "11", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,3" to DeviceModel("iPhone12,3", "iPhone 11 Pro", "IPHONE_11_PRO", "11_PRO", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,5" to DeviceModel("iPhone12,5", "iPhone 11 Pro Max", "IPHONE_11_PRO_MAX", "11_PRO_MAX", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,8" to DeviceModel("iPhone12,8", "iPhone SE (2nd generation)", "IPHONE_SE2", "SE2", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,1" to DeviceModel("iPhone13,1", "iPhone 12 mini", "IPHONE_12_MINI", "12_MINI", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,2" to DeviceModel("iPhone13,2", "iPhone 12", "IPHONE_12", "12", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,3" to DeviceModel("iPhone13,3", "iPhone 12 Pro", "IPHONE_12_PRO", "12_PRO", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,4" to DeviceModel("iPhone13,4", "iPhone 12 Pro Max", "IPHONE_12_PRO_MAX", "12_PRO_MAX", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone14,2" to DeviceModel("iPhone14,2", "iPhone 13 Pro", "IPHONE_13_PRO", "13_PRO", "SMC_13", 2021),
        "iPhone14,3" to DeviceModel("iPhone14,3", "iPhone 13 Pro Max", "IPHONE_13_PRO_MAX", "13_PRO_MAX", "SMC_13", 2021),
        "iPhone14,4" to DeviceModel("iPhone14,4", "iPhone 13 mini", "IPHONE_13_MINI", "13_MINI", "SMC_13_MINI", 2021),
        "iPhone14,5" to DeviceModel("iPhone14,5", "iPhone 13", "IPHONE_13", "13", "SMC_13", 2021),
        "iPhone14,6" to DeviceModel("iPhone14,6", "iPhone SE (3rd generation)", "IPHONE_SE3", "SE3", "THERMAL_CLASSIC_X_TO_12", 2022),
        "iPhone14,7" to DeviceModel("iPhone14,7", "iPhone 14", "IPHONE_14", "14", "SMC_14_BASE", 2022),
        "iPhone14,8" to DeviceModel("iPhone14,8", "iPhone 14 Plus", "IPHONE_14_PLUS", "14_PLUS", "SMC_14_BASE", 2022),
        "iPhone15,2" to DeviceModel("iPhone15,2", "iPhone 14 Pro", "IPHONE_14_PRO", "14_PRO", "SMC_14_PRO", 2022),
        "iPhone15,3" to DeviceModel("iPhone15,3", "iPhone 14 Pro Max", "IPHONE_14_PRO_MAX", "14_PRO_MAX", "SMC_14_PRO", 2022),
        "iPhone15,4" to DeviceModel("iPhone15,4", "iPhone 15", "IPHONE_15", "15", "SMC_15_BASE", 2023),
        "iPhone15,5" to DeviceModel("iPhone15,5", "iPhone 15 Plus", "IPHONE_15_PLUS", "15_PLUS", "SMC_15_BASE", 2023),
        "iPhone16,1" to DeviceModel("iPhone16,1", "iPhone 15 Pro", "IPHONE_15_PRO", "15_PRO", "SMC_15_PRO", 2023),
        "iPhone16,2" to DeviceModel("iPhone16,2", "iPhone 15 Pro Max", "IPHONE_15_PRO_MAX", "15_PRO_MAX", "SMC_15_PRO", 2023),
        "iPhone17,1" to DeviceModel("iPhone17,1", "iPhone 16 Pro", "IPHONE_16_PRO", "16_PRO", "SMC_16_PRO", 2024),
        "iPhone17,2" to DeviceModel("iPhone17,2", "iPhone 16 Pro Max", "IPHONE_16_PRO_MAX", "16_PRO_MAX", "SMC_16_PRO", 2024),
        "iPhone17,3" to DeviceModel("iPhone17,3", "iPhone 16", "IPHONE_16", "16", "SMC_16_BASE", 2024),
        "iPhone17,4" to DeviceModel("iPhone17,4", "iPhone 16 Plus", "IPHONE_16_PLUS", "16_PLUS", "SMC_16_BASE", 2024),
        "iPhone17,5" to DeviceModel("iPhone17,5", "iPhone 16e", "IPHONE_16E", "16E", "SMC_16E", 2025),
        "iPhone18,1" to DeviceModel("iPhone18,1", "iPhone 17 Pro", "IPHONE_17_PRO", "17_PRO", "SMC_17", 2025),
        "iPhone18,2" to DeviceModel("iPhone18,2", "iPhone 17 Pro Max", "IPHONE_17_PRO_MAX", "17_PRO_MAX", "SMC_17", 2025),
        "iPhone18,3" to DeviceModel("iPhone18,3", "iPhone 17", "IPHONE_17", "17", "SMC_17", 2025),
        "iPhone18,4" to DeviceModel("iPhone18,4", "iPhone Air", "IPHONE_AIR", "AIR", "SMC_17", 2025),
        "iPhone18,5" to DeviceModel("iPhone18,5", "iPhone 17e", "IPHONE_17E", "17E", "SMC_17E", 2026)
    )

    fun resolveSynchronous(productCode: String?): DeviceModel? {
        if (productCode.isNullOrBlank()) return null
        return staticDeviceMap[productCode.trim()]
    }
}

object EvidenceExtractor {
    fun extractEvidences(
        logText: String,
        metadata: ParsedMetadata,
        panicFamilies: List<PanicFamily>,
        extractedSensors: ExtractedSensors,
        idGenerator: IdGenerator
    ): List<DiagnosticEvidence> {
        val evidences = mutableListOf<DiagnosticEvidence>()
        val lines = logText.lines()

        for (sensor in extractedSensors.missingSensorTokens) {
            val (lineIndex, excerpt) = findLineWithContext(lines, sensor)
            evidences += DiagnosticEvidence(
                id = idGenerator.nextId(),
                type = "MISSING_SENSOR",
                title = "Sensor Térmico Faltante: $sensor",
                rawValue = sensor,
                normalizedValue = sensor.uppercase(),
                excerpt = excerpt ?: "Missing sensor(s): $sensor",
                lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
            )
        }

        for (code in extractedSensors.smcSensorCodes) {
            val (lineIndex, excerpt) = findLineWithContext(lines, code)
            val canonicalHex = HexUtils.toCanonicalHex(code)
            val decimal = HexUtils.toDecimalString(code)
            val description = if (canonicalHex != decimal) "$canonicalHex ($decimal)" else canonicalHex
            evidences += DiagnosticEvidence(
                id = idGenerator.nextId(),
                type = "SMC_CODE",
                title = "Código SMC Sensor Array: $description",
                rawValue = code,
                normalizedValue = canonicalHex,
                excerpt = excerpt ?: "Sensor array code: $code",
                lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
            )
        }

        for (family in panicFamilies) {
            if (family == PanicFamily.UNKNOWN) continue
            val keyword = when (family) {
                PanicFamily.AOP_BOSCH_CONTROL -> "Bosch"
                PanicFamily.AOP_NMI_POWER -> "NMI POWER"
                PanicFamily.ANS2 -> "ANS2"
                PanicFamily.APPLE_SOC_HOT -> "AppleSocHot"
                PanicFamily.SEP_ROM_BOOT -> "SEP ROM"
                PanicFamily.UNDEFINED_KERNEL_INSTRUCTION -> "Undefined Kernel Instruction"
                PanicFamily.I2C -> "i2c"
                PanicFamily.DCP_DISPLAY -> "DCP"
                PanicFamily.BASEBAND -> "baseband"
                PanicFamily.WATCHDOG_NO_CHECKIN -> "watchdog timeout"
                PanicFamily.SMC_BSC_FAILURE -> "BSC failure"
                PanicFamily.SMC_ASSERTION -> "ASSERT"
                PanicFamily.THERMAL_MISSING_SENSOR -> "thermalmonitord"
                else -> null
            }
            if (keyword != null) {
                val (lineIndex, excerpt) = findLineWithContext(lines, keyword)
                if (excerpt != null) {
                    evidences += DiagnosticEvidence(
                        id = idGenerator.nextId(),
                        type = "PANIC_SIGNATURE",
                        title = "Firma de Pánico: ${family.name}",
                        rawValue = keyword,
                        normalizedValue = family.name,
                        excerpt = excerpt,
                        lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
                    )
                }
            }
        }

        val product = metadata.product
        if (!product.isNullOrBlank()) {
            val (lineIndex, excerpt) = findLineWithContext(lines, product)
            evidences += DiagnosticEvidence(
                id = idGenerator.nextId(),
                type = "PRODUCT_CODE",
                title = "Identificador de Hardware: $product",
                rawValue = product,
                normalizedValue = product,
                excerpt = excerpt ?: "product: $product",
                lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
            )
        }

        return evidences.distinctBy { "${it.type}_${it.rawValue}" }
    }

    private fun findLineWithContext(lines: List<String>, search: String): Pair<Int, String?> {
        for (index in lines.indices) {
            if (lines[index].contains(search, ignoreCase = true)) {
                val start = maxOf(0, index - 1)
                val end = minOf(lines.size - 1, index + 1)
                return index to lines.subList(start, end + 1).joinToString("\n")
            }
        }
        return -1 to null
    }
}
