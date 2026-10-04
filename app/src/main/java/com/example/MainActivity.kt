package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.repository.DiagnosticRepositoryImpl
import com.example.data.repository.KnowledgeBaseRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.navigation.PanicLabNavGraph
import com.example.navigation.Screen
import com.example.release.TrialAccessManager
import com.example.release.TrialExpiredScreen
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

    private var analysisViewModelRef: AnalysisViewModel? = null
    private var externalFileAnalysisActive by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()

        val database = AppDatabase.getDatabase(applicationContext)
        val dataStoreManager = DataStoreManager(applicationContext)
        val trialAccessManager = TrialAccessManager(applicationContext)

        val kbRepository = KnowledgeBaseRepositoryImpl(applicationContext, database)
        val settingsRepository = SettingsRepositoryImpl(dataStoreManager)
        val diagnosticRepository = DiagnosticRepositoryImpl(database, kbRepository)

        val analysisViewModel by viewModels<AnalysisViewModel> {
            AnalysisViewModelFactory(diagnosticRepository, settingsRepository)
        }
        analysisViewModelRef = analysisViewModel
        val historyViewModel by viewModels<HistoryViewModel> {
            HistoryViewModelFactory(diagnosticRepository)
        }
        val kbViewModel by viewModels<KnowledgeBaseViewModel> {
            KnowledgeBaseViewModelFactory(kbRepository)
        }
        val settingsViewModel by viewModels<SettingsViewModel> {
            SettingsViewModelFactory(settingsRepository, kbRepository)
        }
        val trendDashboardViewModel by viewModels<com.example.ui.trends.TrendDashboardViewModel> {
            com.example.ui.trends.TrendDashboardViewModelFactory(diagnosticRepository, kbRepository)
        }

        // Handle incoming shared file or text if opened via ACTION_SEND
        handleIncomingIntent(intent, analysisViewModel)

        setContent {
            val recentReports by diagnosticRepository.getSessionHistory().collectAsState(initial = emptyList())
            val kbVersion by kbViewModel.currentVersion.collectAsState()

            LaunchedEffect(Unit) {
                kbRepository.initializeDefaultRulePackIfNeeded()
            }

            PanicLabTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var trialState by remember { mutableStateOf(trialAccessManager.currentState()) }

                    if (trialState.canUseApp) {
                        val navController = rememberNavController()
                        val analysisUiState by analysisViewModel.uiState.collectAsState()

                        LaunchedEffect(externalFileAnalysisActive, analysisUiState) {
                            if (!externalFileAnalysisActive) return@LaunchedEffect

                            when (val state = analysisUiState) {
                                is com.example.ui.analysis.AnalysisUiState.Analyzing -> {
                                    if (navController.currentDestination?.route != Screen.ImportFile.route) {
                                        navController.navigate(Screen.ImportFile.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                                is com.example.ui.analysis.AnalysisUiState.Success -> {
                                    externalFileAnalysisActive = false
                                    val currentRoute = navController.currentDestination?.route
                                    val localAnalysisRoutes = setOf(
                                        Screen.ImportFile.route,
                                        Screen.PasteLog.route,
                                        Screen.CameraScanner.route
                                    )
                                    if (currentRoute !in localAnalysisRoutes) {
                                        navController.navigate(Screen.Result.createRoute(state.report.id)) {
                                            launchSingleTop = true
                                        }
                                        analysisViewModel.resetState()
                                    }
                                }

                                is com.example.ui.analysis.AnalysisUiState.Error -> {
                                    externalFileAnalysisActive = false
                                    if (navController.currentDestination?.route != Screen.ImportFile.route) {
                                        navController.navigate(Screen.ImportFile.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                                else -> Unit
                            }
                        }

                        PanicLabNavGraph(
                            navController = navController,
                            diagnosticRepository = diagnosticRepository,
                            kbRepository = kbRepository,
                            settingsRepository = settingsRepository,
                            analysisViewModel = analysisViewModel,
                            historyViewModel = historyViewModel,
                            kbViewModel = kbViewModel,
                            settingsViewModel = settingsViewModel,
                            trendDashboardViewModel = trendDashboardViewModel,
                            recentReports = recentReports,
                            kbVersion = kbVersion
                        )
                    } else {
                        TrialExpiredScreen(
                            onActivate = { code ->
                                val activated = trialAccessManager.activate(code)
                                if (activated) {
                                    trialState = trialAccessManager.currentState()
                                }
                                activated
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        analysisViewModelRef?.let { handleIncomingIntent(intent, it) }
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun handleIncomingIntent(intent: Intent?, analysisViewModel: AnalysisViewModel) {
        try {
            if (intent == null) return

            val uri: Uri? = when (intent.action) {
                Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (!sharedText.isNullOrBlank()) {
                            analysisViewModel.updateLogInputText(sharedText)
                        }
                    }

                    @Suppress("DEPRECATION")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                }

                Intent.ACTION_VIEW -> intent.data
                else -> null
            }

            if (uri != null) {
                externalFileAnalysisActive = true
                analysisViewModel.loadFromUri(applicationContext, uri, "Archivo Compartido")
            }
        } catch (e: Exception) {
            externalFileAnalysisActive = false
            e.printStackTrace()
        }
    }
}
