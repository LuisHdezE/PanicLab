package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DiagnosisCandidateEntity
import com.example.data.local.entity.DiagnosticEvidenceEntity
import com.example.data.local.entity.DiagnosticSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDiagnosticStorageTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveSessionWithRoom() = runBlocking {
        val sessionDao = database.diagnosticSessionDao()
        val evidenceDao = database.diagnosticEvidenceDao()
        val candidateDao = database.diagnosisCandidateDao()

        val sessionId = "test-session-123"
        val session = DiagnosticSessionEntity(
            id = sessionId,
            createdAt = System.currentTimeMillis(),
            sourceFilename = "panic_full_test.ips",
            deviceProductCode = "D73AP",
            deviceName = "iPhone 14 Pro",
            osVersion = "17.4",
            build = "21E219",
            panicFamiliesJson = "[\"MIC_FAILURE\"]",
            panicStringSummary = "Sensor mic2 failed to respond on I2C3",
            primaryRuleId = "RULE_MIC2_001",
            primaryDiagnosis = "Falla de Micrófono 2 (Flex de Carga)",
            confidence = "HIGH",
            verificationStatus = "COMMUNITY_VERIFIED",
            knowledgeBaseVersion = "1.0.0",
            appliedRuleIdsJson = "[\"RULE_MIC2_001\"]",
            repairFlowJson = "{\"rootCause\":\"Flex dañado\",\"priority\":1,\"repairSteps\":[]}",
            rawLog = "{\"panicString\": \"mic2 timeout\"}",
            rawLogSaved = true,
            technicianNotes = "Revisión inicial en microscopio: condensador sulfatado",
            customerName = "Cliente Juan Pérez"
        )

        sessionDao.insertSession(session)

        val evidence = DiagnosticEvidenceEntity(
            id = "ev-1",
            sessionId = sessionId,
            type = "SENSOR_TIMEOUT",
            title = "Mic2 Timeout",
            rawValue = "0x80000000",
            normalizedValue = "mic2",
            excerpt = "mic2 did not respond on i2c",
            lineNumber = 42
        )
        evidenceDao.insertAll(listOf(evidence))

        val candidate = DiagnosisCandidateEntity(
            id = "cand-1",
            sessionId = sessionId,
            ruleId = "RULE_MIC2_001",
            label = "Falla de Micrófono 2 (Flex de Carga)",
            subsystem = "Audio/Charging Flex",
            suspectedComponentsJson = "[{\"name\":\"Microphone 2\",\"designation\":\"MIC2\"}]",
            interpretation = "El sensor mic2 no responde.",
            confidence = "HIGH",
            verificationStatus = "COMMUNITY_VERIFIED",
            isPrimary = true
        )
        candidateDao.insertAll(listOf(candidate))

        // Retrieve session
        val loadedSession = sessionDao.getSessionById(sessionId)
        assertNotNull(loadedSession)
        assertEquals("iPhone 14 Pro", loadedSession?.deviceName)
        assertEquals("Falla de Micrófono 2 (Flex de Carga)", loadedSession?.primaryDiagnosis)
        assertEquals("Revisión inicial en microscopio: condensador sulfatado", loadedSession?.technicianNotes)

        // Retrieve evidences and candidates
        val loadedEvidences = evidenceDao.getEvidencesForSession(sessionId)
        assertEquals(1, loadedEvidences.size)
        assertEquals("Mic2 Timeout", loadedEvidences[0].title)

        val loadedCandidates = candidateDao.getCandidatesForSession(sessionId)
        assertEquals(1, loadedCandidates.size)
        assertEquals("RULE_MIC2_001", loadedCandidates[0].ruleId)

        // Test search query
        val searchResults = sessionDao.searchSessions("sulfatado").first()
        assertEquals(1, searchResults.size)
        assertEquals(sessionId, searchResults[0].id)

        // Test update technician notes
        sessionDao.updateTechnicianNotes(sessionId, "Nota actualizada: Flex reemplazado con éxito")
        val updatedSession = sessionDao.getSessionById(sessionId)
        assertEquals("Nota actualizada: Flex reemplazado con éxito", updatedSession?.technicianNotes)
    }
}
