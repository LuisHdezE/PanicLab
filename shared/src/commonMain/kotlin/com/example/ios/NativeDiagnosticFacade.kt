package com.example.ios

import com.example.diagnostic.CandidateRanker
import com.example.diagnostic.DiagnosticRulesEngine
import com.example.diagnostic.SensorExtractor
import com.example.domain.model.ConfidenceLevel
import com.example.ocr.OcrLogExtractor
import com.example.parser.DeviceResolver
import com.example.parser.EvidenceExtractor
import com.example.parser.LogNormalizer
import com.example.parser.MetadataExtractor
import com.example.parser.PanicClassifier
import com.example.platform.IdGenerator
import com.example.platform.Sha256Hasher
import com.example.rulepack.RulePackJsonParser
import com.example.rulepack.RulePackValidator

/**
 * Small Swift-friendly composition facade for the native iOS diagnostic slice.
 *
 * The deterministic engine remains in COMMON. iOS supplies the canonical bundled
 * rule-pack JSON plus its SHA-256 digest from CryptoKit, keeping cryptographic and
 * UI concerns native while avoiding Room/persistence in the iOS slice.
 */
class NativeDiagnosticFacade {
    @Throws(IllegalArgumentException::class)
    fun analyze(
        rawLog: String,
        rulePackJson: String,
        rulePackChecksum: String
    ): NativeDiagnosticResult {
        require(rawLog.isNotBlank()) { "El log no puede estar vacío." }
        require(rulePackJson.isNotBlank()) { "El Rule Pack no puede estar vacío." }
        require(rulePackChecksum.isNotBlank()) { "El checksum del Rule Pack es obligatorio." }

        val parsedPack = RulePackJsonParser.parse(
            jsonString = rulePackJson,
            hasher = Sha256Hasher { rulePackChecksum }
        )
        val validation = RulePackValidator.validate(parsedPack)
        require(validation.isValid) {
            validation.errors.joinToString("; ") { issue -> issue.message }
                .ifBlank { "Rule Pack inválido." }
        }

        val normalizedLog = LogNormalizer.normalize(rawLog)
        val metadata = MetadataExtractor.extract(normalizedLog)
        val productCode = metadata.product?.trim()
        val device = productCode?.let { code ->
            parsedPack.deviceModels.firstOrNull { it.productCode == code }
                ?: DeviceResolver.resolveSynchronous(code)
        }
        val panicFamilies = PanicClassifier.classify(normalizedLog, metadata.panicString)
        val sensors = SensorExtractor.extract(normalizedLog, metadata.panicString)

        val evidenceIds = SequentialIdGenerator("ios-evidence")
        val evidences = EvidenceExtractor.extractEvidences(
            logText = normalizedLog,
            metadata = metadata,
            panicFamilies = panicFamilies,
            extractedSensors = sensors,
            idGenerator = evidenceIds
        )

        val match = DiagnosticRulesEngine.evaluate(
            deviceModel = device,
            productCode = productCode,
            panicFamilies = panicFamilies,
            extractedSensors = sensors,
            allRules = parsedPack.diagnosticRules
        )
        val (primary, alternatives) = CandidateRanker.toCandidates(
            primaryRule = match.primaryRule,
            alternativeRules = match.alternativeRules
        )

        val isConclusive = primary != null && primary.confidence != ConfidenceLevel.UNKNOWN

        return NativeDiagnosticResult(
            productCode = productCode ?: device?.productCode ?: "Desconocido",
            deviceName = device?.marketingName ?: productCode ?: "Dispositivo no identificado",
            osVersion = metadata.osVersion ?: "Desconocido",
            build = metadata.build ?: "N/A",
            diagnosis = primary?.label ?: "Diagnóstico no concluyente",
            subsystem = primary?.subsystem ?: "GENERAL",
            interpretation = primary?.interpretation.orEmpty(),
            confidence = primary?.confidence?.name ?: ConfidenceLevel.UNKNOWN.name,
            verificationStatus = primary?.verificationStatus?.name ?: "UNKNOWN",
            panicFamiliesText = panicFamilies.joinToString("\n") { it.name },
            suspectedComponentsText = primary?.suspectedComponents
                ?.joinToString("\n") { component -> component.name }
                .orEmpty(),
            firstChecksText = primary?.repairFlow?.firstChecks?.joinToString("\n").orEmpty(),
            knownGoodTest = primary?.repairFlow?.knownGoodTest.orEmpty(),
            boardLevelNextStepsText = primary?.repairFlow?.boardLevelNextSteps?.joinToString("\n").orEmpty(),
            cautionsText = primary?.repairFlow?.cautions?.joinToString("\n").orEmpty(),
            alternativesText = alternatives.joinToString("\n") { it.label },
            evidenceCount = evidences.size,
            knowledgeBaseVersion = parsedPack.knowledgeBaseVersion,
            isConclusive = isConclusive
        )
    }
}

data class NativeDiagnosticResult(
    val productCode: String,
    val deviceName: String,
    val osVersion: String,
    val build: String,
    val diagnosis: String,
    val subsystem: String,
    val interpretation: String,
    val confidence: String,
    val verificationStatus: String,
    val panicFamiliesText: String,
    val suspectedComponentsText: String,
    val firstChecksText: String,
    val knownGoodTest: String,
    val boardLevelNextStepsText: String,
    val cautionsText: String,
    val alternativesText: String,
    val evidenceCount: Int,
    val knowledgeBaseVersion: String,
    val isConclusive: Boolean
)

/**
 * Swift-friendly projection of the existing COMMON OCR cleanup result.
 *
 * This facade deliberately delegates every cleanup/classification decision to
 * [OcrLogExtractor]. It adds no platform-specific parsing semantics.
 */
class NativeOcrFacade {
    fun process(rawText: String): NativeOcrScanResult {
        val result = OcrLogExtractor.processScannedText(rawText)
        return NativeOcrScanResult(
            rawText = result.rawText,
            cleanedText = result.cleanedText,
            detectedDeviceModel = result.detectedDeviceModel,
            detectedBuild = result.detectedBuild,
            panicCodesText = result.detectedPanicCodes.joinToString("\n"),
            keywordsText = result.detectedKeywords.joinToString("\n"),
            lineCount = result.lineCount,
            hasValidPanicSignatures = result.hasValidPanicSignatures,
            confidenceHint = result.confidenceHint
        )
    }
}

data class NativeOcrScanResult(
    val rawText: String,
    val cleanedText: String,
    val detectedDeviceModel: String?,
    val detectedBuild: String?,
    val panicCodesText: String,
    val keywordsText: String,
    val lineCount: Int,
    val hasValidPanicSignatures: Boolean,
    val confidenceHint: String
)

private class SequentialIdGenerator(
    private val prefix: String
) : IdGenerator {
    private var value: Int = 0

    override fun nextId(): String {
        value += 1
        return "$prefix-$value"
    }
}
