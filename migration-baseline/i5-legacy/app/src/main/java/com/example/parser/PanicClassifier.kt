package com.example.parser

import com.example.domain.model.PanicFamily
import java.util.regex.Pattern

object PanicClassifier {

    fun classify(logText: String, panicString: String?): List<PanicFamily> {
        val combinedText = buildString {
            append(logText)
            if (!panicString.isNullOrBlank()) {
                append("\n").append(panicString)
            }
        }

        val families = mutableSetOf<PanicFamily>()

        // THERMAL_MISSING_SENSOR: "thermalmonitord" and "Missing sensor(s):"
        if (combinedText.contains("thermalmonitord", ignoreCase = true) &&
            Pattern.compile("Missing sensor\\(s\\)\\s*:", Pattern.CASE_INSENSITIVE).matcher(combinedText).find()
        ) {
            families.add(PanicFamily.THERMAL_MISSING_SENSOR)
        }

        // SMC_BSC_FAILURE: "SMC" and "BSC failure"
        if (combinedText.contains("SMC", ignoreCase = true) &&
            combinedText.contains("BSC failure", ignoreCase = true)
        ) {
            families.add(PanicFamily.SMC_BSC_FAILURE)
        }

        // SMC_ASSERTION: SMC and ASSERTION / ASSERT
        if (combinedText.contains("SMC", ignoreCase = true) &&
            (combinedText.contains("ASSERT", ignoreCase = true) ||
             combinedText.contains("ASSERTION FAILED", ignoreCase = true))
        ) {
            families.add(PanicFamily.SMC_ASSERTION)
        }

        // AOP_NMI_POWER
        if (combinedText.contains("AOP", ignoreCase = true) &&
            combinedText.contains("NMI POWER", ignoreCase = true)
        ) {
            families.add(PanicFamily.AOP_NMI_POWER)
        }

        // AOP_BOSCH_CONTROL
        if (combinedText.contains("Bosch", ignoreCase = true) &&
            combinedText.contains("control channel", ignoreCase = true) &&
            combinedText.contains("write failure", ignoreCase = true)
        ) {
            families.add(PanicFamily.AOP_BOSCH_CONTROL)
        }

        // ANS2
        if (combinedText.contains("ANS2", ignoreCase = true)) {
            families.add(PanicFamily.ANS2)
        }

        // APPLE_SOC_HOT
        if (combinedText.contains("AppleSocHot", ignoreCase = true) ||
            combinedText.contains("Hot Hot Hot", ignoreCase = true)
        ) {
            families.add(PanicFamily.APPLE_SOC_HOT)
        }

        // SEP_ROM_BOOT
        if (combinedText.contains("SEP ROM", ignoreCase = true) ||
            combinedText.contains("SEP ROM Boot Panic", ignoreCase = true)
        ) {
            families.add(PanicFamily.SEP_ROM_BOOT)
        }

        // UNDEFINED_KERNEL_INSTRUCTION
        if (combinedText.contains("Undefined Kernel Instruction", ignoreCase = true)) {
            families.add(PanicFamily.UNDEFINED_KERNEL_INSTRUCTION)
        }

        // I2C
        if (Pattern.compile("\\bi2c\\d*\\b|\\bi²c\\b", Pattern.CASE_INSENSITIVE).matcher(combinedText).find()) {
            families.add(PanicFamily.I2C)
        }

        // DCP_DISPLAY
        if (Pattern.compile("\\bDCP\\b|iomfb|display coprocessor", Pattern.CASE_INSENSITIVE).matcher(combinedText).find()) {
            families.add(PanicFamily.DCP_DISPLAY)
        }

        // BASEBAND
        if (Pattern.compile("\\bbaseband\\b|AppleBaseband|\\bBB[A-Z_]*\\b", Pattern.CASE_INSENSITIVE).matcher(combinedText).find()) {
            families.add(PanicFamily.BASEBAND)
        }

        // WATCHDOG_NO_CHECKIN
        if ((combinedText.contains("watchdog timeout", ignoreCase = true) ||
             combinedText.contains("userspace watchdog timeout", ignoreCase = true)) &&
            !Pattern.compile("Missing sensor\\(s\\)\\s*:", Pattern.CASE_INSENSITIVE).matcher(combinedText).find()
        ) {
            families.add(PanicFamily.WATCHDOG_NO_CHECKIN)
        }

        if (families.isEmpty()) {
            families.add(PanicFamily.UNKNOWN)
        }

        return families.toList()
    }
}
