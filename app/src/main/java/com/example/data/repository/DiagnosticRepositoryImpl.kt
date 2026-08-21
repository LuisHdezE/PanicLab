package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.DiagnosisCandidateEntity
import com.example.data.local.entity.DiagnosticEvidenceEntity
import com.example.data.local.entity.DiagnosticSessionEntity
import com.example.diagnostic.CandidateRanker
import com.example.diagnostic.DiagnosticReportBuilder
import com.example.diagnostic.DiagnosticRulesEngine
import com.example.diagnostic.ExtractedSensors
import com.example.diagnostic.SensorExtractor
import com.example.domain.model.*
import com.example.domain.repository.DiagnosticRepository
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.parser.*
import com.example.util.RulePackJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiagnosticRepositoryImpl(
    private val database: AppDatabase,
    private val kbRepository: KnowledgeBaseRepository
) : DiagnosticRepository {

    private val sessionDao = database.diagnosticSessionDao()
    private val evidenceDao = database.diagnosticEvidenceDao()
    private val candidateDao = database.diagnosisCandidateDao()
    private val deviceDao = database.deviceDao()

    override suspend fun analyzeLog(
        rawLogContent: String,
        sourceFilename: String?,
        saveRawLog: Boolean
    ): DiagnosticReport = withContext(Dispatchers.IO) {
        // Ensure knowledge base is seeded
        kbRepository.initializeDefaultRulePackIfNeeded()

        // 1. Normalize
        val normalizedLog = LogNormalizer.normalize(rawLogContent)

        // 2. Extract Metadata
        val metadata = MetadataExtractor.extract(normalizedLog)

        // 3. Resolve Device Model
        val deviceModel = DeviceResolver.resolve(metadata.product, deviceDao)

        // 4. Classify Panic Families
        val panicFamilies = PanicClassifier.classify(normalizedLog, metadata.panicString)

        // 5. Extract Sensors & Codes
        val extractedSensors = SensorExtractor.extract(normalizedLog)

        // 6. Extract Evidences
        val evidences = EvidenceExtractor.extractEvidences(
            logText = normalizedLog,
            metadata = metadata,
            panicFamilies = panicFamilies,
            extractedSensors = extractedSensors
        )

        // 7. Load all rules and evaluate
        val allRules = kbRepository.getAllRulesDirect()
        val ruleMatchResult = DiagnosticRulesEngine.evaluate(
            deviceModel = deviceModel,
            productCode = metadata.product,
            panicFamilies = panicFamilies,
            extractedSensors = extractedSensors,
            allRules = allRules
        )

        // 8. Rank candidates
        val (primaryCandidate, altCandidates) = CandidateRanker.toCandidates(
            primaryRule = ruleMatchResult.primaryRule,
            alternativeRules = ruleMatchResult.alternativeRules
        )

        val kbVersion = kbRepository.getCurrentRulePackVersion()

        // 9. Build Report
        val report = DiagnosticReportBuilder.build(
            sourceFilename = sourceFilename,
            rawLog = normalizedLog,
            metadata = metadata,
            deviceModel = deviceModel,
            panicFamilies = panicFamilies,
            evidences = evidences,
            primaryCandidate = primaryCandidate,
            alternativeCandidates = altCandidates,
            kbVersion = kbVersion,
            saveRawLogsPreference = saveRawLog
        )

        // 10. Persist Session, Evidences, Candidates to Room
        saveSessionToDatabase(report, ruleMatchResult.appliedRuleIds, saveRawLog, normalizedLog)

        report
    }

    private suspend fun saveSessionToDatabase(
        report: DiagnosticReport,
        appliedRuleIds: List<String>,
        saveRawLog: Boolean,
        normalizedLog: String
    ) {
        val repairFlowJson = JSONObject().apply {
            put("firstChecks", JSONArray(report.repairFlow.firstChecks))
            put("knownGoodTest", report.repairFlow.knownGoodTest ?: "")
            put("boardLevelNextSteps", JSONArray(report.repairFlow.boardLevelNextSteps))
            put("cautions", JSONArray(report.repairFlow.cautions))
        }.toString()

        val sessionEntity = DiagnosticSessionEntity(
            id = report.id,
            createdAt = report.createdAt,
            sourceFilename = report.sourceFilename,
            deviceProductCode = report.productCode,
            deviceName = report.deviceModel?.marketingName ?: report.productCode,
            osVersion = report.osVersion,
            build = report.build,
            panicFamiliesJson = JSONArray(report.panicFamilies.map { it.name }).toString(),
            panicStringSummary = report.panicStringSummary,
            primaryRuleId = report.primaryCandidate?.ruleId,
            primaryDiagnosis = report.primaryCandidate?.label ?: "Diagnóstico no concluyente",
            confidence = report.confidence.name,
            verificationStatus = report.verificationStatus.name,
            knowledgeBaseVersion = report.knowledgeBaseVersion,
            appliedRuleIdsJson = JSONArray(appliedRuleIds).toString(),
            repairFlowJson = repairFlowJson,
            rawLog = if (saveRawLog) normalizedLog else null,
            rawLogSaved = saveRawLog
        )

        sessionDao.insertSession(sessionEntity)

        val evidenceEntities = report.evidences.map { ev ->
            DiagnosticEvidenceEntity(
                id = ev.id,
                sessionId = report.id,
                type = ev.type,
                title = ev.title,
                rawValue = ev.rawValue,
                normalizedValue = ev.normalizedValue,
                excerpt = ev.excerpt,
                lineNumber = ev.lineNumber
            )
        }
        evidenceDao.insertAll(evidenceEntities)

        val candidateEntities = mutableListOf<DiagnosisCandidateEntity>()
        report.primaryCandidate?.let { pc ->
            candidateEntities.add(
                DiagnosisCandidateEntity(
                    id = "${report.id}_primary",
                    sessionId = report.id,
                    ruleId = pc.ruleId,
                    label = pc.label,
                    subsystem = pc.subsystem,
                    suspectedComponentsJson = serializeSuspectedComponents(pc.suspectedComponents),
                    interpretation = pc.interpretation,
                    confidence = pc.confidence.name,
                    verificationStatus = pc.verificationStatus.name,
                    isPrimary = true,
                    repairFlowJson = serializeRepairFlow(pc.repairFlow)
                )
            )
        }

        report.alternativeCandidates.forEachIndexed { index, ac ->
            candidateEntities.add(
                DiagnosisCandidateEntity(
                    id = "${report.id}_alt_$index",
                    sessionId = report.id,
                    ruleId = ac.ruleId,
                    label = ac.label,
                    subsystem = ac.subsystem,
                    suspectedComponentsJson = serializeSuspectedComponents(ac.suspectedComponents),
                    interpretation = ac.interpretation,
                    confidence = ac.confidence.name,
                    verificationStatus = ac.verificationStatus.name,
                    isPrimary = false,
                    repairFlowJson = serializeRepairFlow(ac.repairFlow)
                )
            )
        }

        candidateDao.insertAll(candidateEntities)
    }

    override fun getSessionHistory(): Flow<List<DiagnosticReport>> {
        return sessionDao.getAllSessions().map { sessionEntities ->
            sessionEntities.map { entity ->
                mapSessionEntityToReportSummary(entity)
            }
        }
    }

    override suspend fun getSessionById(sessionId: String): DiagnosticReport? = withContext(Dispatchers.IO) {
        val sessionEntity = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val evidences = evidenceDao.getEvidencesForSession(sessionId).map { ev ->
            DiagnosticEvidence(
                id = ev.id,
                type = ev.type,
                title = ev.title,
                rawValue = ev.rawValue,
                normalizedValue = ev.normalizedValue,
                excerpt = ev.excerpt,
                lineNumber = ev.lineNumber
            )
        }

        val candidates = candidateDao.getCandidatesForSession(sessionId).map { cand ->
            DiagnosisCandidate(
                ruleId = cand.ruleId,
                label = cand.label,
                subsystem = cand.subsystem,
                suspectedComponents = RulePackJsonParser.parseSuspectedComponents(cand.suspectedComponentsJson),
                interpretation = cand.interpretation,
                confidence = try { ConfidenceLevel.valueOf(cand.confidence) } catch (e: Exception) { ConfidenceLevel.UNKNOWN },
                verificationStatus = try { VerificationStatus.valueOf(cand.verificationStatus) } catch (e: Exception) { VerificationStatus.UNKNOWN },
                isPrimary = cand.isPrimary,
                repairFlow = deserializeRepairFlow(cand.repairFlowJson)
            )
        }

        val primaryCand = candidates.firstOrNull { it.isPrimary }
        val altCands = candidates.filter { !it.isPrimary }
        val device = DeviceResolver.resolve(sessionEntity.deviceProductCode, deviceDao)

        val panicFamilies = RulePackJsonParser.parseJsonStringArray(sessionEntity.panicFamiliesJson).mapNotNull {
            try { PanicFamily.valueOf(it) } catch (e: Exception) { null }
        }

        DiagnosticReport(
            id = sessionEntity.id,
            createdAt = sessionEntity.createdAt,
            sourceFilename = sessionEntity.sourceFilename,
            deviceModel = device,
            productCode = sessionEntity.deviceProductCode,
            osVersion = sessionEntity.osVersion,
            build = sessionEntity.build,
            panicFamilies = panicFamilies,
            panicStringSummary = sessionEntity.panicStringSummary,
            evidences = evidences,
            primaryCandidate = primaryCand,
            alternativeCandidates = altCands,
            confidence = try { ConfidenceLevel.valueOf(sessionEntity.confidence) } catch (e: Exception) { ConfidenceLevel.UNKNOWN },
            verificationStatus = try { VerificationStatus.valueOf(sessionEntity.verificationStatus) } catch (e: Exception) { VerificationStatus.UNKNOWN },
            repairFlow = deserializeRepairFlow(sessionEntity.repairFlowJson),
            knowledgeBaseVersion = sessionEntity.knowledgeBaseVersion,
            rawLog = sessionEntity.rawLog,
            rawLogSaved = sessionEntity.rawLogSaved,
            reanalyzedAt = sessionEntity.reanalyzedAt,
            previousDiagnosis = sessionEntity.previousDiagnosis,
            previousKnowledgeBaseVersion = sessionEntity.previousKnowledgeBaseVersion
        )
    }

    override suspend fun reanalyzeSession(sessionId: String): Result<DiagnosticReport> = withContext(Dispatchers.IO) {
        try {
            val sessionEntity = sessionDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(IllegalArgumentException("Sesión no encontrada: $sessionId"))

            val currentKbVersion = kbRepository.getCurrentRulePackVersion()
            val allRules = kbRepository.getAllRulesDirect()
            val device = DeviceResolver.resolve(sessionEntity.deviceProductCode, deviceDao)

            val panicFamilies = RulePackJsonParser.parseJsonStringArray(sessionEntity.panicFamiliesJson).mapNotNull {
                try { PanicFamily.valueOf(it) } catch (e: Exception) { null }
            }

            val rawLog = sessionEntity.rawLog
            val (evaluatedPrimary, evaluatedAlts, appliedRules, updatedEvidences) = if (!rawLog.isNullOrBlank()) {
                val normalizedLog = LogNormalizer.normalize(rawLog)
                val metadata = MetadataExtractor.extract(normalizedLog)
                val reclassifiedFamilies = PanicClassifier.classify(normalizedLog, metadata.panicString)
                val sensors = SensorExtractor.extract(normalizedLog)
                val newEvidences = EvidenceExtractor.extractEvidences(normalizedLog, metadata, reclassifiedFamilies, sensors)
                val matchRes = DiagnosticRulesEngine.evaluate(
                    deviceModel = device,
                    productCode = sessionEntity.deviceProductCode,
                    panicFamilies = if (reclassifiedFamilies.isNotEmpty()) reclassifiedFamilies else panicFamilies,
                    extractedSensors = sensors,
                    allRules = allRules
                )
                val (p, a) = CandidateRanker.toCandidates(matchRes.primaryRule, matchRes.alternativeRules)
                Quad(p, a, matchRes.appliedRuleIds, newEvidences)
            } else {
                // Reconstruct extracted sensors from stored evidences
                val existingEvidences = evidenceDao.getEvidencesForSession(sessionId)
                val missingTokens = mutableListOf<String>()
                val smcCodes = mutableListOf<String>()
                val rawLines = mutableListOf<String>()

                existingEvidences.forEach { ev ->
                    if (ev.type == "SMC_CODE" || ev.type == "SENSOR") {
                        smcCodes.add(ev.rawValue)
                    } else if (ev.type == "MISSING_SENSOR") {
                        missingTokens.add(ev.normalizedValue)
                    }
                    if (!ev.excerpt.isNullOrBlank()) {
                        rawLines.add(ev.excerpt)
                    }
                }
                val sensors = ExtractedSensors(
                    missingSensorTokens = missingTokens,
                    smcSensorCodes = smcCodes,
                    rawSensorArrayLines = rawLines
                )

                val matchRes = DiagnosticRulesEngine.evaluate(
                    deviceModel = device,
                    productCode = sessionEntity.deviceProductCode,
                    panicFamilies = panicFamilies,
                    extractedSensors = sensors,
                    allRules = allRules
                )
                val (p, a) = CandidateRanker.toCandidates(matchRes.primaryRule, matchRes.alternativeRules)
                val mappedEvs = existingEvidences.map { ev ->
                    DiagnosticEvidence(
                        id = ev.id,
                        type = ev.type,
                        title = ev.title,
                        rawValue = ev.rawValue,
                        normalizedValue = ev.normalizedValue,
                        excerpt = ev.excerpt,
                        lineNumber = ev.lineNumber
                    )
                }
                Quad(p, a, matchRes.appliedRuleIds, mappedEvs)
            }

            val newPrimaryLabel = evaluatedPrimary?.label ?: "Diagnóstico no catalogado"
            val newConfidence = evaluatedPrimary?.confidence ?: ConfidenceLevel.UNKNOWN
            val newStatus = evaluatedPrimary?.verificationStatus ?: VerificationStatus.UNKNOWN
            val newRepairFlow = evaluatedPrimary?.repairFlow ?: RepairFlow()

            // Update database
            val updatedSessionEntity = sessionEntity.copy(
                primaryRuleId = evaluatedPrimary?.ruleId,
                primaryDiagnosis = newPrimaryLabel,
                confidence = newConfidence.name,
                verificationStatus = newStatus.name,
                knowledgeBaseVersion = currentKbVersion,
                appliedRuleIdsJson = JSONArray(appliedRules).toString(),
                repairFlowJson = serializeRepairFlow(newRepairFlow),
                reanalyzedAt = System.currentTimeMillis(),
                previousDiagnosis = sessionEntity.primaryDiagnosis,
                previousKnowledgeBaseVersion = sessionEntity.knowledgeBaseVersion
            )
            sessionDao.insertSession(updatedSessionEntity)

            // Replace candidates
            candidateDao.deleteCandidatesForSession(sessionId)
            val candidateEntities = mutableListOf<DiagnosisCandidateEntity>()
            evaluatedPrimary?.let { pc ->
                candidateEntities.add(
                    DiagnosisCandidateEntity(
                        id = "${sessionId}_primary_${System.currentTimeMillis()}",
                        sessionId = sessionId,
                        ruleId = pc.ruleId,
                        label = pc.label,
                        subsystem = pc.subsystem,
                        suspectedComponentsJson = serializeSuspectedComponents(pc.suspectedComponents),
                        interpretation = pc.interpretation,
                        confidence = pc.confidence.name,
                        verificationStatus = pc.verificationStatus.name,
                        isPrimary = true,
                        repairFlowJson = serializeRepairFlow(pc.repairFlow)
                    )
                )
            }
            evaluatedAlts.forEachIndexed { idx, ac ->
                candidateEntities.add(
                    DiagnosisCandidateEntity(
                        id = "${sessionId}_alt_${idx}_${System.currentTimeMillis()}",
                        sessionId = sessionId,
                        ruleId = ac.ruleId,
                        label = ac.label,
                        subsystem = ac.subsystem,
                        suspectedComponentsJson = serializeSuspectedComponents(ac.suspectedComponents),
                        interpretation = ac.interpretation,
                        confidence = ac.confidence.name,
                        verificationStatus = ac.verificationStatus.name,
                        isPrimary = false,
                        repairFlowJson = serializeRepairFlow(ac.repairFlow)
                    )
                )
            }
            candidateDao.insertAll(candidateEntities)

            // Reconstruct updated report
            val updatedReport = DiagnosticReport(
                id = sessionId,
                createdAt = sessionEntity.createdAt,
                sourceFilename = sessionEntity.sourceFilename,
                deviceModel = device,
                productCode = sessionEntity.deviceProductCode,
                osVersion = sessionEntity.osVersion,
                build = sessionEntity.build,
                panicFamilies = panicFamilies,
                panicStringSummary = sessionEntity.panicStringSummary,
                evidences = updatedEvidences,
                primaryCandidate = evaluatedPrimary,
                alternativeCandidates = evaluatedAlts,
                confidence = newConfidence,
                verificationStatus = newStatus,
                repairFlow = newRepairFlow,
                knowledgeBaseVersion = currentKbVersion,
                rawLog = sessionEntity.rawLog,
                rawLogSaved = sessionEntity.rawLogSaved,
                reanalyzedAt = updatedSessionEntity.reanalyzedAt,
                previousDiagnosis = sessionEntity.primaryDiagnosis,
                previousKnowledgeBaseVersion = sessionEntity.knowledgeBaseVersion
            )

            Result.success(updatedReport)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    override suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        sessionDao.deleteSessionById(sessionId)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        sessionDao.deleteAllSessions()
    }

    private fun mapSessionEntityToReportSummary(entity: DiagnosticSessionEntity): DiagnosticReport {
        val panicFamilies = RulePackJsonParser.parseJsonStringArray(entity.panicFamiliesJson).mapNotNull {
            try { PanicFamily.valueOf(it) } catch (e: Exception) { null }
        }
        val device = DeviceResolver.resolveSynchronous(entity.deviceProductCode)

        return DiagnosticReport(
            id = entity.id,
            createdAt = entity.createdAt,
            sourceFilename = entity.sourceFilename,
            deviceModel = device,
            productCode = entity.deviceProductCode,
            osVersion = entity.osVersion,
            build = entity.build,
            panicFamilies = panicFamilies,
            panicStringSummary = entity.panicStringSummary,
            evidences = emptyList(),
            primaryCandidate = DiagnosisCandidate(
                ruleId = entity.primaryRuleId ?: "",
                label = entity.primaryDiagnosis,
                subsystem = "",
                suspectedComponents = emptyList(),
                interpretation = "",
                confidence = try { ConfidenceLevel.valueOf(entity.confidence) } catch (e: Exception) { ConfidenceLevel.UNKNOWN },
                verificationStatus = try { VerificationStatus.valueOf(entity.verificationStatus) } catch (e: Exception) { VerificationStatus.UNKNOWN },
                isPrimary = true
            ),
            alternativeCandidates = emptyList(),
            confidence = try { ConfidenceLevel.valueOf(entity.confidence) } catch (e: Exception) { ConfidenceLevel.UNKNOWN },
            verificationStatus = try { VerificationStatus.valueOf(entity.verificationStatus) } catch (e: Exception) { VerificationStatus.UNKNOWN },
            repairFlow = deserializeRepairFlow(entity.repairFlowJson),
            knowledgeBaseVersion = entity.knowledgeBaseVersion,
            rawLog = entity.rawLog,
            rawLogSaved = entity.rawLogSaved,
            reanalyzedAt = entity.reanalyzedAt,
            previousDiagnosis = entity.previousDiagnosis,
            previousKnowledgeBaseVersion = entity.previousKnowledgeBaseVersion
        )
    }

    private fun serializeSuspectedComponents(components: List<SuspectedComponent>): String {
        val arr = JSONArray()
        components.forEach { comp ->
            val obj = JSONObject().apply {
                put("name", comp.name)
                put("role", comp.role)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun serializeRepairFlow(flow: RepairFlow): String {
        return JSONObject().apply {
            put("firstChecks", JSONArray(flow.firstChecks))
            put("knownGoodTest", flow.knownGoodTest ?: "")
            put("boardLevelNextSteps", JSONArray(flow.boardLevelNextSteps))
            put("cautions", JSONArray(flow.cautions))
        }.toString()
    }

    private fun deserializeRepairFlow(json: String?): RepairFlow {
        if (json.isNullOrBlank()) return RepairFlow()
        return try {
            val obj = JSONObject(json)
            val firstChecksArr = obj.optJSONArray("firstChecks") ?: JSONArray()
            val firstChecks = (0 until firstChecksArr.length()).map { firstChecksArr.getString(it) }
            val knownGoodTest = if (obj.isNull("knownGoodTest")) null else obj.optString("knownGoodTest")
            val boardStepsArr = obj.optJSONArray("boardLevelNextSteps") ?: JSONArray()
            val boardSteps = (0 until boardStepsArr.length()).map { boardStepsArr.getString(it) }
            val cautionsArr = obj.optJSONArray("cautions") ?: JSONArray()
            val cautions = (0 until cautionsArr.length()).map { cautionsArr.getString(it) }

            RepairFlow(firstChecks, knownGoodTest, boardSteps, cautions)
        } catch (e: Exception) {
            RepairFlow()
        }
    }
}
