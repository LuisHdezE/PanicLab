package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.DiagnosticRepositoryImpl
import com.example.data.repository.KnowledgeBaseRepositoryImpl
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.PanicFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-KMP-051: proves the Android product path is wired through the shared
 * deterministic engine while Room and bundled rule-pack persistence stay native.
 *
 * The diagnostic input and expected outcome are frozen from the I0 baseline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidSharedCutoverIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DiagnosticRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val knowledgeBaseRepository = KnowledgeBaseRepositoryImpl(context, database)
        repository = DiagnosticRepositoryImpl(database, knowledgeBaseRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun analyzeLog_usesSharedEngineAndPersistsResultThroughRoom() = runBlocking {
        val rawLog = """
            {"bug_type":"210","product":"iPhone14,7","os_version":"17.3"}
            panic(cpu 1): "SMC PANIC - ASSERTION FAILED: S.sensor array is 0x0, 0x500000, 0x0"
        """.trimIndent()

        val report = repository.analyzeLog(
            rawLogContent = rawLog,
            sourceFilename = "i5-cutover-baseline.ips",
            saveRawLog = true
        )

        assertEquals("iPhone14,7", report.productCode)
        assertEquals("iPhone 14", report.deviceModel?.marketingName)
        assertTrue(report.panicFamilies.contains(PanicFamily.SMC_ASSERTION))
        assertEquals(
            "Batería / Línea I2C Gas Gauge (BATT_HDQ / BATT_SWI)",
            report.primaryCandidate?.label
        )
        assertEquals(ConfidenceLevel.HIGH, report.confidence)
        assertTrue(
            report.evidences.any {
                it.type == "SMC_CODE" && it.normalizedValue == "0x500000"
            }
        )
        assertTrue(report.rawLogSaved)
        assertTrue(report.rawLog?.contains("0x500000") == true)

        val persisted = repository.getSessionById(report.id)
        assertNotNull(persisted)
        assertEquals(report.id, persisted?.id)
        assertEquals(report.productCode, persisted?.productCode)
        assertEquals(report.primaryCandidate?.ruleId, persisted?.primaryCandidate?.ruleId)
        assertEquals(report.primaryCandidate?.label, persisted?.primaryCandidate?.label)
        assertEquals(report.confidence, persisted?.confidence)
        assertEquals(report.knowledgeBaseVersion, persisted?.knowledgeBaseVersion)
        assertTrue(persisted?.rawLogSaved == true)
        assertTrue(persisted?.rawLog?.contains("0x500000") == true)

        val history = repository.getSessionHistory().first()
        assertEquals(1, history.size)
        assertEquals(report.id, history.single().id)
        assertEquals(report.primaryCandidate?.label, history.single().primaryCandidate?.label)
    }

    @Test
    fun decimalSensorFixture_keepsFrozenSharedDiagnosticSemantics() = runBlocking {
        val rawLog = """
            "product":"iPhone14,7"
            "panicString":"SMC PANIC - ASSERT: SMC BSC failure\nS.sensor array 0 - 5 is 0, 4194304, 0, 0, 0"
        """.trimIndent()

        val report = repository.analyzeLog(
            rawLogContent = rawLog,
            sourceFilename = "i5-decimal-regression.ips",
            saveRawLog = false
        )

        assertEquals("iPhone14,7", report.productCode)
        assertEquals("iPhone 14", report.deviceModel?.marketingName)
        assertEquals("SMC_14_BASE", report.deviceModel?.diagnosticProfile)
        assertTrue(report.panicFamilies.contains(PanicFamily.SMC_BSC_FAILURE))
        assertEquals("Wireless Charging Coil", report.primaryCandidate?.label)
        assertEquals(ConfidenceLevel.HIGH, report.confidence)
        assertTrue(
            report.evidences.any {
                it.type == "SMC_CODE" &&
                    it.rawValue == "4194304" &&
                    it.normalizedValue == "0x400000"
            }
        )
        assertTrue(!report.rawLogSaved)
        assertEquals(null, report.rawLog)

        val persisted = repository.getSessionById(report.id)
        assertNotNull(persisted)
        assertEquals("Wireless Charging Coil", persisted?.primaryCandidate?.label)
        assertTrue(persisted?.rawLogSaved == false)
        assertEquals(null, persisted?.rawLog)
    }
}
