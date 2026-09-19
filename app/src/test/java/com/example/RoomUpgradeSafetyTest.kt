package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DiagnosisCandidateEntity
import com.example.data.local.entity.DiagnosticEvidenceEntity
import com.example.data.local.entity.DiagnosticSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * I6 / TASK-KMP-061 safety harness.
 *
 * Historical fixtures are reconstructed test inputs derived from the source declarations proven by
 * TASK-KMP-060. They are not original Room schema exports and are not recovered user databases.
 *
 * The strict Room opener intentionally does NOT use fallbackToDestructiveMigration(). A historical
 * database without a registered migration must fail closed so the fixture remains available for a
 * future explicit migration test instead of being silently recreated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomUpgradeSafetyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databasesToDelete = mutableSetOf<String>()

    @After
    fun cleanup() {
        databasesToDelete.forEach(context::deleteDatabase)
    }

    @Test
    fun reconstructedV1_withoutMigration_failsClosedAndPreservesSentinels() {
        assertLegacyFixtureFailsClosedAndPreservesData(version = 1)
    }

    @Test
    fun reconstructedV2_withoutMigration_failsClosedAndPreservesSentinels() {
        assertLegacyFixtureFailsClosedAndPreservesData(version = 2)
    }

    @Test
    fun currentV3_reopenPreservesRepresentativeHistory() = runBlocking {
        val dbName = "paniclab-i6-v3-preservation.db"
        databasesToDelete += dbName
        context.deleteDatabase(dbName)

        var database = openStrictCurrent(dbName)
        val sessionId = "i6-v3-session"

        database.diagnosticSessionDao().insertSession(
            DiagnosticSessionEntity(
                id = sessionId,
                createdAt = 1_789_790_000_000L,
                sourceFilename = "i6-v3-baseline.ips",
                deviceProductCode = "iPhone12,8",
                deviceName = "iPhone SE (2nd generation)",
                osVersion = "17.6.1",
                build = "21G93",
                panicFamiliesJson = "[\"THERMAL_MISSING_SENSOR\"]",
                panicStringSummary = "Missing sensor(s): mic1",
                primaryRuleId = "classic_mic1_charge_port",
                primaryDiagnosis = "Ruta de micrófono inferior / conjunto del puerto de carga",
                confidence = "HIGH",
                verificationStatus = "WELL_DOCUMENTED",
                knowledgeBaseVersion = "1.0.0",
                appliedRuleIdsJson = "[\"classic_mic1_charge_port\"]",
                repairFlowJson = "{\"firstChecks\":[\"Inspeccionar dock flex\"]}",
                rawLog = "panicString: Missing sensor(s): mic1",
                rawLogSaved = true,
                technicianNotes = "I6 preservation sentinel",
                customerName = "I6 Test Customer"
            )
        )
        database.diagnosticEvidenceDao().insertAll(
            listOf(
                DiagnosticEvidenceEntity(
                    id = "i6-v3-evidence",
                    sessionId = sessionId,
                    type = "THERMAL_SENSOR_MISSING",
                    title = "Mic1 missing",
                    rawValue = "mic1",
                    normalizedValue = "MIC1",
                    excerpt = "Missing sensor(s): mic1",
                    lineNumber = 1
                )
            )
        )
        database.diagnosisCandidateDao().insertAll(
            listOf(
                DiagnosisCandidateEntity(
                    id = "i6-v3-candidate",
                    sessionId = sessionId,
                    ruleId = "classic_mic1_charge_port",
                    label = "Ruta de micrófono inferior / conjunto del puerto de carga",
                    subsystem = "THERMAL_SENSOR",
                    suspectedComponentsJson = "[\"Charge Port Assembly / bottom microphone path\"]",
                    interpretation = "Mic1 corresponde al micrófono inferior.",
                    confidence = "HIGH",
                    verificationStatus = "WELL_DOCUMENTED",
                    isPrimary = true,
                    repairFlowJson = "{\"knownGoodTest\":\"Probar flex conocido\"}"
                )
            )
        )
        database.close()

        database = openStrictCurrent(dbName)
        val reopenedSession = database.diagnosticSessionDao().getSessionById(sessionId)
        val reopenedEvidence = database.diagnosticEvidenceDao().getEvidencesForSession(sessionId)
        val reopenedCandidates = database.diagnosisCandidateDao().getCandidatesForSession(sessionId)

        assertNotNull(reopenedSession)
        assertEquals("I6 preservation sentinel", reopenedSession?.technicianNotes)
        assertEquals("I6 Test Customer", reopenedSession?.customerName)
        assertEquals("Ruta de micrófono inferior / conjunto del puerto de carga", reopenedSession?.primaryDiagnosis)
        assertEquals(1, reopenedEvidence.size)
        assertEquals("MIC1", reopenedEvidence.single().normalizedValue)
        assertEquals(1, reopenedCandidates.size)
        assertEquals("classic_mic1_charge_port", reopenedCandidates.single().ruleId)
        database.close()
    }

    private fun assertLegacyFixtureFailsClosedAndPreservesData(version: Int) {
        val dbName = "paniclab-i6-v${version}-reconstructed.db"
        databasesToDelete += dbName
        context.deleteDatabase(dbName)
        createHistoricalFixture(dbName, version)

        val database = openStrictCurrent(dbName)
        assertThrows(IllegalStateException::class.java) {
            database.openHelper.writableDatabase
        }
        database.close()

        val path = context.getDatabasePath(dbName)
        val sqlite = SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                cursor.moveToFirst()
                assertEquals(version, cursor.getInt(0))
            }
            assertEquals(1, count(sqlite, "diagnostic_sessions", "legacy-session-v$version"))
            assertEquals(1, count(sqlite, "diagnostic_evidences", "legacy-evidence-v$version"))
            assertEquals(1, count(sqlite, "diagnosis_candidates", "legacy-candidate-v$version"))
        } finally {
            sqlite.close()
        }
    }

    private fun openStrictCurrent(dbName: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()

    private fun createHistoricalFixture(dbName: String, version: Int) {
        require(version == 1 || version == 2)
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(path, null)
        try {
            sqlite.execSQL("PRAGMA foreign_keys = ON")
            sqlite.execSQL(createDiagnosticSessionsSql(version))
            sqlite.execSQL(
                """
                CREATE TABLE `diagnostic_evidences` (
                    `id` TEXT NOT NULL,
                    `sessionId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `rawValue` TEXT NOT NULL,
                    `normalizedValue` TEXT NOT NULL,
                    `excerpt` TEXT NOT NULL,
                    `lineNumber` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`sessionId`) REFERENCES `diagnostic_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                "CREATE INDEX `index_diagnostic_evidences_sessionId` ON `diagnostic_evidences` (`sessionId`)"
            )
            sqlite.execSQL(
                """
                CREATE TABLE `diagnosis_candidates` (
                    `id` TEXT NOT NULL,
                    `sessionId` TEXT NOT NULL,
                    `ruleId` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `subsystem` TEXT NOT NULL,
                    `suspectedComponentsJson` TEXT NOT NULL,
                    `interpretation` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `verificationStatus` TEXT NOT NULL,
                    `isPrimary` INTEGER NOT NULL,
                    `repairFlowJson` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`sessionId`) REFERENCES `diagnostic_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                "CREATE INDEX `index_diagnosis_candidates_sessionId` ON `diagnosis_candidates` (`sessionId`)"
            )

            sqlite.execSQL(
                """
                INSERT INTO `diagnostic_sessions` (
                    `id`, `createdAt`, `sourceFilename`, `deviceProductCode`, `deviceName`,
                    `osVersion`, `build`, `panicFamiliesJson`, `panicStringSummary`, `primaryRuleId`,
                    `primaryDiagnosis`, `confidence`, `verificationStatus`, `knowledgeBaseVersion`,
                    `appliedRuleIdsJson`, `repairFlowJson`, `rawLog`, `rawLogSaved`
                ) VALUES (
                    'legacy-session-v$version', 1720000000000, 'legacy-v$version.ips', 'iPhone12,8',
                    'iPhone SE (2nd generation)', '17.6.1', '21G93', '["THERMAL_MISSING_SENSOR"]',
                    'Missing sensor(s): mic1', 'classic_mic1_charge_port',
                    'Ruta de micrófono inferior / conjunto del puerto de carga', 'HIGH',
                    'WELL_DOCUMENTED', 'legacy-$version', '["classic_mic1_charge_port"]', '{}',
                    'panicString: Missing sensor(s): mic1', 1
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `diagnostic_evidences` (
                    `id`, `sessionId`, `type`, `title`, `rawValue`, `normalizedValue`, `excerpt`, `lineNumber`
                ) VALUES (
                    'legacy-evidence-v$version', 'legacy-session-v$version', 'THERMAL_SENSOR_MISSING',
                    'Mic1 missing', 'mic1', 'MIC1', 'Missing sensor(s): mic1', 1
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `diagnosis_candidates` (
                    `id`, `sessionId`, `ruleId`, `label`, `subsystem`, `suspectedComponentsJson`,
                    `interpretation`, `confidence`, `verificationStatus`, `isPrimary`, `repairFlowJson`
                ) VALUES (
                    'legacy-candidate-v$version', 'legacy-session-v$version', 'classic_mic1_charge_port',
                    'Ruta de micrófono inferior / conjunto del puerto de carga', 'THERMAL_SENSOR',
                    '["Charge Port Assembly / bottom microphone path"]',
                    'Mic1 corresponde al micrófono inferior.', 'HIGH', 'WELL_DOCUMENTED', 1, '{}'
                )
                """.trimIndent()
            )
            sqlite.execSQL("PRAGMA user_version = $version")
        } finally {
            sqlite.close()
        }
    }

    private fun createDiagnosticSessionsSql(version: Int): String {
        val v2Columns = if (version >= 2) {
            """
                , `reanalyzedAt` INTEGER
                , `previousDiagnosis` TEXT
                , `previousKnowledgeBaseVersion` TEXT
            """.trimIndent()
        } else {
            ""
        }

        return """
            CREATE TABLE `diagnostic_sessions` (
                `id` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `sourceFilename` TEXT,
                `deviceProductCode` TEXT NOT NULL,
                `deviceName` TEXT NOT NULL,
                `osVersion` TEXT NOT NULL,
                `build` TEXT NOT NULL,
                `panicFamiliesJson` TEXT NOT NULL,
                `panicStringSummary` TEXT NOT NULL,
                `primaryRuleId` TEXT,
                `primaryDiagnosis` TEXT NOT NULL,
                `confidence` TEXT NOT NULL,
                `verificationStatus` TEXT NOT NULL,
                `knowledgeBaseVersion` TEXT NOT NULL,
                `appliedRuleIdsJson` TEXT NOT NULL,
                `repairFlowJson` TEXT NOT NULL,
                `rawLog` TEXT,
                `rawLogSaved` INTEGER NOT NULL
                $v2Columns,
                PRIMARY KEY(`id`)
            )
        """.trimIndent()
    }

    private fun count(sqlite: SQLiteDatabase, table: String, id: String): Int {
        sqlite.rawQuery("SELECT COUNT(*) FROM `$table` WHERE `id` = ?", arrayOf(id)).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }
}
