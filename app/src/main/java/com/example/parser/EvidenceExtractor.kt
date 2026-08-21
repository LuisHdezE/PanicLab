package com.example.parser

import com.example.diagnostic.ExtractedSensors
import com.example.domain.model.DiagnosticEvidence
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.util.HexUtils
import java.util.UUID

object EvidenceExtractor {

    fun extractEvidences(
        logText: String,
        metadata: ParsedMetadata,
        panicFamilies: List<PanicFamily>,
        extractedSensors: ExtractedSensors
    ): List<DiagnosticEvidence> {
        val evidences = mutableListOf<DiagnosticEvidence>()
        val lines = logText.lines()

        // 1. Missing Sensor Evidences
        for (sensor in extractedSensors.missingSensorTokens) {
            val (lineIndex, excerpt) = findLineWithContext(lines, sensor)
            evidences.add(
                DiagnosticEvidence(
                    id = UUID.randomUUID().toString(),
                    type = "MISSING_SENSOR",
                    title = "Sensor Térmico Faltante: $sensor",
                    rawValue = sensor,
                    normalizedValue = sensor.uppercase(),
                    excerpt = excerpt ?: "Missing sensor(s): $sensor",
                    lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
                )
            )
        }

        // 2. SMC Sensor Array Code Evidences
        for (code in extractedSensors.smcSensorCodes) {
            val (lineIndex, excerpt) = findLineWithContext(lines, code)
            val canonicalHex = HexUtils.toCanonicalHex(code)
            val decimalStr = HexUtils.toDecimalString(code)
            val codeDesc = if (canonicalHex != decimalStr) "$canonicalHex ($decimalStr)" else canonicalHex

            evidences.add(
                DiagnosticEvidence(
                    id = UUID.randomUUID().toString(),
                    type = "SMC_CODE",
                    title = "Código SMC Sensor Array: $codeDesc",
                    rawValue = code,
                    normalizedValue = canonicalHex,
                    excerpt = excerpt ?: "Sensor array code: $code",
                    lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
                )
            )
        }

        // 3. Panic Signature / Subsystem Excerpts
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
                    evidences.add(
                        DiagnosticEvidence(
                            id = UUID.randomUUID().toString(),
                            type = "PANIC_SIGNATURE",
                            title = "Firma de Pánico: ${family.name}",
                            rawValue = keyword,
                            normalizedValue = family.name,
                            excerpt = excerpt,
                            lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
                        )
                    )
                }
            }
        }

        // 4. Product / Hardware identifier evidence
        if (!metadata.product.isNullOrBlank()) {
            val (lineIndex, excerpt) = findLineWithContext(lines, metadata.product)
            evidences.add(
                DiagnosticEvidence(
                    id = UUID.randomUUID().toString(),
                    type = "PRODUCT_CODE",
                    title = "Identificador de Hardware: ${metadata.product}",
                    rawValue = metadata.product,
                    normalizedValue = metadata.product,
                    excerpt = excerpt ?: "product: ${metadata.product}",
                    lineNumber = if (lineIndex >= 0) lineIndex + 1 else -1
                )
            )
        }

        return evidences.distinctBy { "${it.type}_${it.rawValue}" }
    }

    private fun findLineWithContext(lines: List<String>, search: String): Pair<Int, String?> {
        for (i in lines.indices) {
            if (lines[i].contains(search, ignoreCase = true)) {
                val start = maxOf(0, i - 1)
                val end = minOf(lines.size - 1, i + 1)
                val excerpt = lines.subList(start, end + 1).joinToString("\n")
                return Pair(i, excerpt)
            }
        }
        return Pair(-1, null)
    }
}
