package com.example.data.repository

import com.example.data.local.dao.DeviceDao
import com.example.data.mapper.RulePackPersistenceMapper
import com.example.diagnostic.DiagnosticReportBuilder
import com.example.diagnostic.ExtractedSensors
import com.example.domain.model.DeviceModel
import com.example.domain.model.DiagnosisCandidate
import com.example.domain.model.DiagnosticEvidence
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.PanicFamily
import com.example.domain.model.ParsedMetadata
import com.example.parser.DeviceResolver
import com.example.parser.EvidenceExtractor
import com.example.platform.SystemEpochClock
import com.example.platform.UuidIdGenerator

/**
 * Android composition adapters used only during the I5 cutover.
 *
 * COMMON remains deterministic and platform-free. Android supplies Room lookup,
 * wall-clock and UUID implementations at the composition boundary.
 */
suspend fun DeviceResolver.resolve(
    productCode: String?,
    deviceDao: DeviceDao?
): DeviceModel? {
    if (productCode.isNullOrBlank()) return null
    val cleanCode = productCode.trim()

    if (deviceDao != null) {
        val persisted = runCatching { deviceDao.getDeviceByProductCode(cleanCode) }.getOrNull()
        if (persisted != null) {
            return RulePackPersistenceMapper.toDomainDevice(persisted)
        }
    }

    return resolveSynchronous(cleanCode)
}

fun EvidenceExtractor.extractEvidences(
    logText: String,
    metadata: ParsedMetadata,
    panicFamilies: List<PanicFamily>,
    extractedSensors: ExtractedSensors
): List<DiagnosticEvidence> = extractEvidences(
    logText = logText,
    metadata = metadata,
    panicFamilies = panicFamilies,
    extractedSensors = extractedSensors,
    idGenerator = UuidIdGenerator
)

fun DiagnosticReportBuilder.build(
    sourceFilename: String?,
    rawLog: String,
    metadata: ParsedMetadata,
    deviceModel: DeviceModel?,
    panicFamilies: List<PanicFamily>,
    evidences: List<DiagnosticEvidence>,
    primaryCandidate: DiagnosisCandidate?,
    alternativeCandidates: List<DiagnosisCandidate>,
    kbVersion: String,
    saveRawLogsPreference: Boolean
): DiagnosticReport = build(
    sourceFilename = sourceFilename,
    rawLog = rawLog,
    metadata = metadata,
    deviceModel = deviceModel,
    panicFamilies = panicFamilies,
    evidences = evidences,
    primaryCandidate = primaryCandidate,
    alternativeCandidates = alternativeCandidates,
    kbVersion = kbVersion,
    saveRawLogsPreference = saveRawLogsPreference,
    clock = SystemEpochClock,
    idGenerator = UuidIdGenerator
)
