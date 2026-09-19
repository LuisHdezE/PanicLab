package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.mapper.RulePackPersistenceMapper
import com.example.domain.model.*
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.util.RulePackDiffCalculator
import com.example.util.RulePackJsonParser
import com.example.util.RulePackValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class KnowledgeBaseRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : KnowledgeBaseRepository {

    private val ruleDao = database.diagnosticRuleDao()
    private val deviceDao = database.deviceDao()
    private val rulePackDao = database.rulePackDao()

    override fun getAllRules(): Flow<List<DiagnosticRule>> {
        return ruleDao.getAllActiveRules().map { list ->
            list.map { RulePackPersistenceMapper.toDomainRule(it) }
        }
    }

    override suspend fun getAllRulesDirect(): List<DiagnosticRule> = withContext(Dispatchers.IO) {
        ruleDao.getAllRulesDirect().map { RulePackPersistenceMapper.toDomainRule(it) }
    }

    override fun searchRules(query: String): Flow<List<DiagnosticRule>> {
        return ruleDao.searchRules(query).map { list ->
            list.map { RulePackPersistenceMapper.toDomainRule(it) }
        }
    }

    override suspend fun getRuleById(id: String): DiagnosticRule? = withContext(Dispatchers.IO) {
        ruleDao.getRuleById(id)?.let { RulePackPersistenceMapper.toDomainRule(it) }
    }

    override fun getAllDevices(): Flow<List<DeviceModel>> {
        return deviceDao.getAllDevices().map { list ->
            list.map { RulePackPersistenceMapper.toDomainDevice(it) }
        }
    }

    override suspend fun findDevice(query: String): DeviceModel? = withContext(Dispatchers.IO) {
        deviceDao.findDevice(query)?.let { RulePackPersistenceMapper.toDomainDevice(it) }
    }

    override fun getActiveRulePack(): Flow<RulePackMetadata?> {
        return rulePackDao.getActiveRulePackFlow().map { entity ->
            entity?.let { RulePackPersistenceMapper.toMetadata(it) }
        }
    }

    override fun getAllRulePacks(): Flow<List<RulePackMetadata>> {
        return rulePackDao.getAllRulePacks().map { packs ->
            packs.map { RulePackPersistenceMapper.toMetadata(it) }
        }
    }

    override suspend fun initializeDefaultRulePackIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val activePack = rulePackDao.getActiveRulePack()
        val existingRules = ruleDao.getAllRulesDirect()
        if (activePack != null && existingRules.isNotEmpty()) {
            return@withContext false
        }

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("paniclab_rules_v1.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonContent = reader.use { it.readText() }

            val parseResult = RulePackJsonParser.parse(
                jsonString = jsonContent,
                origin = RulePackOrigin.BUNDLED,
                sourceFilename = "paniclab_rules_v1.json"
            )

            database.withTransaction {
                deviceDao.deleteAll()
                ruleDao.deleteAll()
                rulePackDao.deactivateAllPacks()

                deviceDao.insertAll(parseResult.deviceModelEntities)
                ruleDao.insertAll(parseResult.diagnosticRuleEntities)
                rulePackDao.insertRulePack(parseResult.rulePackEntity.copy(isActive = true, isDefault = true))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun validateRulePack(
        jsonContent: String,
        origin: RulePackOrigin,
        filename: String?
    ): Pair<RulePackValidationResult, ParsedRulePack?> = withContext(Dispatchers.IO) {
        try {
            val parseResult = RulePackJsonParser.parse(
                jsonString = jsonContent,
                origin = origin,
                sourceFilename = filename
            )
            val validation = RulePackValidator.validate(parseResult.parsedPack)
            Pair(validation, if (validation.isValid) parseResult.parsedPack else null)
        } catch (e: Exception) {
            val errorResult = RulePackValidationResult(
                isValid = false,
                schemaVersion = -1,
                knowledgeBaseVersion = "Desconocida",
                rulesCount = 0,
                modelsCount = 0,
                classifiersCount = 0,
                sourcesCount = 0,
                bitmaskCount = 0,
                errors = listOf(
                    ValidationIssue(
                        type = ValidationIssue.IssueType.ERROR,
                        path = "json",
                        message = "Error al parsear archivo JSON: ${e.localizedMessage ?: e.message}"
                    )
                ),
                warnings = emptyList(),
                checksum = ""
            )
            Pair(errorResult, null)
        }
    }

    override suspend fun computeDiffWithCurrent(incomingPack: ParsedRulePack): RulePackDiffSummary = withContext(Dispatchers.IO) {
        val currentRules = getAllRulesDirect()
        val activePack = rulePackDao.getActiveRulePack()

        val currentParsed = if (activePack != null && currentRules.isNotEmpty()) {
            val rawJson = activePack.rawJson
            if (!rawJson.isNullOrBlank()) {
                try {
                    RulePackJsonParser.parse(rawJson).parsedPack
                } catch (e: Exception) {
                    ParsedRulePack(
                        schemaVersion = activePack.schemaVersion,
                        knowledgeBaseVersion = activePack.version,
                        title = activePack.title,
                        generatedAt = activePack.generatedAt,
                        locale = "es-UY",
                        sources = emptyList(),
                        deviceModels = emptyList(),
                        panicClassifiers = emptyList(),
                        bitmaskPolicies = emptyList(),
                        diagnosticRules = currentRules
                    )
                }
            } else {
                ParsedRulePack(
                    schemaVersion = activePack.schemaVersion,
                    knowledgeBaseVersion = activePack.version,
                    title = activePack.title,
                    generatedAt = activePack.generatedAt,
                    locale = "es-UY",
                    sources = emptyList(),
                    deviceModels = emptyList(),
                    panicClassifiers = emptyList(),
                    bitmaskPolicies = emptyList(),
                    diagnosticRules = currentRules
                )
            }
        } else {
            null
        }

        RulePackDiffCalculator.calculateDiff(currentParsed, incomingPack)
    }

    override suspend fun installRulePackAtomic(
        parsedPack: ParsedRulePack,
        filename: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val validation = RulePackValidator.validate(parsedPack)
            if (!validation.isValid) {
                return@withContext Result.failure(
                    IllegalArgumentException("El Rule Pack contiene ${validation.errors.size} errores de validación y no puede instalarse.")
                )
            }

            val parseResult = RulePackJsonParser.parse(
                jsonString = parsedPack.rawJson,
                origin = parsedPack.origin,
                sourceFilename = filename
            )

            val currentActive = rulePackDao.getActiveRulePack()
            val previousVer = currentActive?.version

            database.withTransaction {
                deviceDao.deleteAll()
                ruleDao.deleteAll()

                deviceDao.insertAll(parseResult.deviceModelEntities)
                ruleDao.insertAll(parseResult.diagnosticRuleEntities)

                rulePackDao.deactivateAllPacks()

                val newPackEntity = parseResult.rulePackEntity.copy(
                    isActive = true,
                    previousVersion = previousVer
                )
                rulePackDao.insertRulePack(newPackEntity)
            }

            Result.success(parsedPack.knowledgeBaseVersion)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun restoreBundledDefault(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("paniclab_rules_v1.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonContent = reader.use { it.readText() }

            val parseResult = RulePackJsonParser.parse(
                jsonString = jsonContent,
                origin = RulePackOrigin.BUNDLED,
                sourceFilename = "paniclab_rules_v1.json"
            )

            val currentActive = rulePackDao.getActiveRulePack()
            val previousVer = currentActive?.version

            database.withTransaction {
                deviceDao.deleteAll()
                ruleDao.deleteAll()
                rulePackDao.deactivateAllPacks()

                deviceDao.insertAll(parseResult.deviceModelEntities)
                ruleDao.insertAll(parseResult.diagnosticRuleEntities)

                val bundledEntity = parseResult.rulePackEntity.copy(
                    isActive = true,
                    isDefault = true,
                    previousVersion = previousVer
                )
                rulePackDao.insertRulePack(bundledEntity)
            }

            Result.success(parseResult.parsedPack.knowledgeBaseVersion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreRulePackVersion(version: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetPack = rulePackDao.getRulePack(version)
                ?: return@withContext Result.failure(IllegalArgumentException("No se encontró el Rule Pack versión $version en el historial."))

            val rawJson = targetPack.rawJson
                ?: return@withContext Result.failure(IllegalStateException("El Rule Pack versión $version no tiene el archivo JSON almacenado."))

            val origin = try { RulePackOrigin.valueOf(targetPack.origin) } catch (e: Exception) { RulePackOrigin.USER_IMPORTED }
            val parseResult = RulePackJsonParser.parse(rawJson, origin = origin, sourceFilename = targetPack.sourceFilename)

            val currentActive = rulePackDao.getActiveRulePack()
            val previousVer = currentActive?.version

            database.withTransaction {
                deviceDao.deleteAll()
                ruleDao.deleteAll()
                rulePackDao.deactivateAllPacks()

                deviceDao.insertAll(parseResult.deviceModelEntities)
                ruleDao.insertAll(parseResult.diagnosticRuleEntities)

                val updatedPack = targetPack.copy(
                    isActive = true,
                    previousVersion = previousVer
                )
                rulePackDao.insertRulePack(updatedPack)
            }

            Result.success(version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importRulePackJson(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        val (validation, parsedPack) = validateRulePack(jsonContent, RulePackOrigin.USER_IMPORTED, null)
        if (!validation.isValid || parsedPack == null) {
            return@withContext Result.failure(
                IllegalArgumentException("Validación fallida con ${validation.errors.size} errores.")
            )
        }
        installRulePackAtomic(parsedPack, null)
    }

    override suspend fun getCurrentRulePackVersion(): String = withContext(Dispatchers.IO) {
        val active = rulePackDao.getActiveRulePack()
        if (active != null) {
            active.version
        } else {
            val rules = ruleDao.getAllRulesDirect()
            rules.firstOrNull()?.version ?: "1.0.0"
        }
    }
}
