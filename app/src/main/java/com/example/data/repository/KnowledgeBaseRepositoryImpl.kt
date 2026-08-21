package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.domain.model.DeviceModel
import com.example.domain.model.DiagnosticRule
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.util.RulePackJsonParser
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
            list.map { RulePackJsonParser.entityToDomain(it) }
        }
    }

    override suspend fun getAllRulesDirect(): List<DiagnosticRule> = withContext(Dispatchers.IO) {
        ruleDao.getAllRulesDirect().map { RulePackJsonParser.entityToDomain(it) }
    }

    override fun searchRules(query: String): Flow<List<DiagnosticRule>> {
        return ruleDao.searchRules(query).map { list ->
            list.map { RulePackJsonParser.entityToDomain(it) }
        }
    }

    override suspend fun getRuleById(id: String): DiagnosticRule? = withContext(Dispatchers.IO) {
        ruleDao.getRuleById(id)?.let { RulePackJsonParser.entityToDomain(it) }
    }

    override fun getAllDevices(): Flow<List<DeviceModel>> {
        return deviceDao.getAllDevices().map { list ->
            list.map { RulePackJsonParser.entityToDevice(it) }
        }
    }

    override suspend fun findDevice(query: String): DeviceModel? = withContext(Dispatchers.IO) {
        deviceDao.findDevice(query)?.let { RulePackJsonParser.entityToDevice(it) }
    }

    override suspend fun initializeDefaultRulePackIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val existingRules = ruleDao.getAllRulesDirect()
        if (existingRules.isNotEmpty()) {
            return@withContext false
        }

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("paniclab_rules_v1.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonContent = reader.use { it.readText() }

            val parsedPack = RulePackJsonParser.parse(jsonContent, isDefault = true)

            deviceDao.insertAll(parsedPack.deviceModelEntities)
            ruleDao.insertAll(parsedPack.diagnosticRuleEntities)
            rulePackDao.insertRulePack(parsedPack.rulePackEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun importRulePackJson(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parsedPack = RulePackJsonParser.parse(jsonContent, isDefault = false)
            deviceDao.insertAll(parsedPack.deviceModelEntities)
            ruleDao.insertAll(parsedPack.diagnosticRuleEntities)
            rulePackDao.insertRulePack(parsedPack.rulePackEntity)
            Result.success(parsedPack.rulePackEntity.version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentRulePackVersion(): String = withContext(Dispatchers.IO) {
        val rules = ruleDao.getAllRulesDirect()
        rules.firstOrNull()?.version ?: "1.0.0"
    }
}
