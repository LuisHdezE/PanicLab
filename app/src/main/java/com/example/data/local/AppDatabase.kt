package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        DeviceModelEntity::class,
        DiagnosticRuleEntity::class,
        DiagnosticSessionEntity::class,
        DiagnosticEvidenceEntity::class,
        DiagnosisCandidateEntity::class,
        RulePackEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun diagnosticRuleDao(): DiagnosticRuleDao
    abstract fun diagnosticSessionDao(): DiagnosticSessionDao
    abstract fun diagnosticEvidenceDao(): DiagnosticEvidenceDao
    abstract fun diagnosisCandidateDao(): DiagnosisCandidateDao
    abstract fun rulePackDao(): RulePackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paniclab_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
