package com.example.diagnostic

import com.example.domain.model.*
import com.example.platform.Clock
import com.example.platform.IdGenerator
import com.example.platform.SystemEpochClock
import com.example.platform.UuidIdGenerator

object DiagnosticReportBuilder {

    fun build(
        sourceFilename: String?,
        rawLog: String,
        metadata: ParsedMetadata,
        deviceModel: DeviceModel?,
        panicFamilies: List<PanicFamily>,
        evidences: List<DiagnosticEvidence>,
        primaryCandidate: DiagnosisCandidate?,
        alternativeCandidates: List<DiagnosisCandidate>,
        kbVersion: String,
        saveRawLogsPreference: Boolean,
        clock: Clock = SystemEpochClock,
        idGenerator: IdGenerator = UuidIdGenerator
    ): DiagnosticReport {
        val resolvedConfidence = primaryCandidate?.confidence ?: ConfidenceLevel.UNKNOWN
        val resolvedVerification = primaryCandidate?.verificationStatus ?: VerificationStatus.UNKNOWN
        val resolvedRepairFlow = primaryCandidate?.repairFlow ?: RepairFlow()

        val panicSummary = buildString {
            if (!metadata.panicString.isNullOrBlank()) {
                val lines = metadata.panicString.lines()
                append(lines.take(3).joinToString("\n"))
            } else if (panicFamilies.isNotEmpty()) {
                append("Familias de pánico identificadas: ")
                append(panicFamilies.joinToString(", ") { it.name })
            } else {
                append("No se detectó un mensaje de pánico estructurado.")
            }
        }

        return DiagnosticReport(
            id = idGenerator.nextId(),
            createdAt = clock.nowEpochMillis(),
            sourceFilename = sourceFilename,
            deviceModel = deviceModel,
            productCode = metadata.product ?: deviceModel?.productCode ?: "Desconocido",
            osVersion = metadata.osVersion ?: "Desconocido",
            build = metadata.build ?: "N/A",
            panicFamilies = panicFamilies,
            panicStringSummary = panicSummary,
            evidences = evidences,
            primaryCandidate = primaryCandidate,
            alternativeCandidates = alternativeCandidates,
            confidence = resolvedConfidence,
            verificationStatus = resolvedVerification,
            repairFlow = resolvedRepairFlow,
            knowledgeBaseVersion = kbVersion,
            rawLog = if (saveRawLogsPreference) rawLog else null,
            rawLogSaved = saveRawLogsPreference
        )
    }
}
