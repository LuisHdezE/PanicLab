package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device_models")
    fun getAllDevices(): Flow<List<DeviceModelEntity>>

    @Query("SELECT * FROM device_models WHERE productCode = :productCode LIMIT 1")
    suspend fun getDeviceByProductCode(productCode: String): DeviceModelEntity?

    @Query("SELECT * FROM device_models WHERE productCode = :query OR marketingName LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findDevice(query: String): DeviceModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceModelEntity>)

    @Query("DELETE FROM device_models")
    suspend fun deleteAll()
}

@Dao
interface DiagnosticRuleDao {
    @Query("SELECT * FROM diagnostic_rules WHERE active = 1 ORDER BY priority DESC")
    fun getAllActiveRules(): Flow<List<DiagnosticRuleEntity>>

    @Query("SELECT * FROM diagnostic_rules ORDER BY priority DESC")
    suspend fun getAllRulesDirect(): List<DiagnosticRuleEntity>

    @Query("SELECT * FROM diagnostic_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: String): DiagnosticRuleEntity?

    @Query("""
        SELECT * FROM diagnostic_rules 
        WHERE active = 1 AND (
            title LIKE '%' || :query || '%' OR 
            label LIKE '%' || :query || '%' OR 
            sensorCodesExactJson LIKE '%' || :query || '%' OR 
            sensorTokensJson LIKE '%' || :query || '%' OR 
            suspectedComponentsJson LIKE '%' || :query || '%'
        ) ORDER BY priority DESC
    """)
    fun searchRules(query: String): Flow<List<DiagnosticRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DiagnosticRuleEntity>)

    @Query("DELETE FROM diagnostic_rules")
    suspend fun deleteAll()
}

@Dao
interface DiagnosticSessionDao {
    @Query("SELECT * FROM diagnostic_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<DiagnosticSessionEntity>>

    @Query("SELECT * FROM diagnostic_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): DiagnosticSessionEntity?

    @Query("""
        SELECT * FROM diagnostic_sessions 
        WHERE deviceName LIKE '%' || :query || '%' OR 
              deviceProductCode LIKE '%' || :query || '%' OR 
              primaryDiagnosis LIKE '%' || :query || '%' OR 
              sourceFilename LIKE '%' || :query || '%' OR
              technicianNotes LIKE '%' || :query || '%' OR
              customerName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchSessions(query: String): Flow<List<DiagnosticSessionEntity>>

    @Query("UPDATE diagnostic_sessions SET technicianNotes = :notes WHERE id = :sessionId")
    suspend fun updateTechnicianNotes(sessionId: String, notes: String?)

    @Query("UPDATE diagnostic_sessions SET customerName = :customerName, technicianNotes = :notes WHERE id = :sessionId")
    suspend fun updateCustomerInfo(sessionId: String, customerName: String?, notes: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DiagnosticSessionEntity)

    @Query("DELETE FROM diagnostic_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM diagnostic_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM diagnostic_sessions")
    fun getSessionsCount(): Flow<Int>
}

@Dao
interface DiagnosticEvidenceDao {
    @Query("SELECT * FROM diagnostic_evidences WHERE sessionId = :sessionId")
    suspend fun getEvidencesForSession(sessionId: String): List<DiagnosticEvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidences: List<DiagnosticEvidenceEntity>)

    @Query("DELETE FROM diagnostic_evidences WHERE sessionId = :sessionId")
    suspend fun deleteEvidencesForSession(sessionId: String)
}

@Dao
interface DiagnosisCandidateDao {
    @Query("SELECT * FROM diagnosis_candidates WHERE sessionId = :sessionId")
    suspend fun getCandidatesForSession(sessionId: String): List<DiagnosisCandidateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candidates: List<DiagnosisCandidateEntity>)

    @Query("DELETE FROM diagnosis_candidates WHERE sessionId = :sessionId")
    suspend fun deleteCandidatesForSession(sessionId: String)
}

@Dao
interface RulePackDao {
    @Query("SELECT * FROM rule_packs ORDER BY importedAt DESC")
    fun getAllRulePacks(): Flow<List<RulePackEntity>>

    @Query("SELECT * FROM rule_packs WHERE version = :version LIMIT 1")
    suspend fun getRulePack(version: String): RulePackEntity?

    @Query("SELECT * FROM rule_packs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveRulePack(): RulePackEntity?

    @Query("SELECT * FROM rule_packs WHERE isActive = 1 LIMIT 1")
    fun getActiveRulePackFlow(): Flow<RulePackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRulePack(pack: RulePackEntity)

    @Query("UPDATE rule_packs SET isActive = 0")
    suspend fun deactivateAllPacks()

    @Query("UPDATE rule_packs SET isActive = 1 WHERE version = :version")
    suspend fun setActivePack(version: String)

    @Query("DELETE FROM rule_packs WHERE version = :version")
    suspend fun deleteRulePack(version: String)
}
