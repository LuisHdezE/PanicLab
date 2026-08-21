package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.repository.DiagnosticRepositoryImpl
import com.example.data.repository.KnowledgeBaseRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.navigation.PanicLabNavGraph
import com.example.navigation.Screen
import com.example.ui.analysis.AnalysisViewModel
import com.example.ui.analysis.AnalysisViewModelFactory
import com.example.ui.history.HistoryViewModel
import com.example.ui.history.HistoryViewModelFactory
import com.example.ui.knowledge_base.KnowledgeBaseViewModel
import com.example.ui.knowledge_base.KnowledgeBaseViewModelFactory
import com.example.ui.settings.SettingsViewModel
import com.example.ui.settings.SettingsViewModelFactory
import com.example.ui.theme.PanicLabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val dataStoreManager = DataStoreManager(applicationContext)

        val kbRepository = KnowledgeBaseRepositoryImpl(applicationContext, database)
        val settingsRepository = SettingsRepositoryImpl(dataStoreManager)
        val diagnosticRepository = DiagnosticRepositoryImpl(database, kbRepository)

        val analysisViewModel by viewModels<AnalysisViewModel> {
            AnalysisViewModelFactory(diagnosticRepository, settingsRepository)
        }
        val historyViewModel by viewModels<HistoryViewModel> {
            HistoryViewModelFactory(diagnosticRepository)
        }
        val kbViewModel by viewModels<KnowledgeBaseViewModel> {
            KnowledgeBaseViewModelFactory(kbRepository)
        }
        val settingsViewModel by viewModels<SettingsViewModel> {
            SettingsViewModelFactory(settingsRepository, kbRepository)
        }

        // Handle incoming shared file or text if opened via ACTION_SEND
        handleIncomingIntent(intent, analysisViewModel)

        setContent {
            val darkModePreference by settingsViewModel.darkMode.collectAsState()
            val isDarkTheme = when (darkModePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            val recentReports by diagnosticRepository.getSessionHistory().collectAsState(initial = emptyList())
            val kbVersion by kbViewModel.currentVersion.collectAsState()

            LaunchedEffect(Unit) {
                kbRepository.initializeDefaultRulePackIfNeeded()
            }

            PanicLabTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    PanicLabNavGraph(
                        navController = navController,
                        diagnosticRepository = diagnosticRepository,
                        kbRepository = kbRepository,
                        settingsRepository = settingsRepository,
                        analysisViewModel = analysisViewModel,
                        historyViewModel = historyViewModel,
                        kbViewModel = kbViewModel,
                        settingsViewModel = settingsViewModel,
                        recentReports = recentReports,
                        kbVersion = kbVersion
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?, analysisViewModel: AnalysisViewModel) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        if ("text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                analysisViewModel.updateLogInputText(sharedText)
            }
        }

        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri != null) {
            analysisViewModel.loadFromUri(applicationContext, uri, "Archivo Compartido")
        }
    }
}
