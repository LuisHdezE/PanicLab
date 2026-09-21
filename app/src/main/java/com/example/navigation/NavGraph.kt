package com.example.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.domain.model.DiagnosticReport
import com.example.domain.repository.DiagnosticRepository
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.domain.repository.SettingsRepository
import com.example.ui.analysis.AnalysisViewModel
import com.example.ui.analysis.LogViewerScreen
import com.example.ui.analysis.ResultScreen
import com.example.ui.analysis.TechnicalEvidenceScreen
import com.example.ui.appleknowledge.AppleOfficialHomeEntry
import com.example.ui.appleknowledge.AppleOfficialKnowledgeScreen
import com.example.ui.appleknowledge.AppleOfficialKnowledgeViewModel
import com.example.ui.appleknowledge.AppleOfficialKnowledgeViewModelFactory
import com.example.ui.export.ExportReportScreen
import com.example.ui.history.CaseDetailScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.import_log.ImportFileScreen
import com.example.ui.import_log.PasteLogScreen
import com.example.ui.scanner.CameraLogScannerScreen
import com.example.ui.knowledge_base.KnowledgeBaseScreen
import com.example.ui.knowledge_base.KnowledgeBaseViewModel
import com.example.ui.knowledge_base.ManageRulePacksScreen
import com.example.ui.knowledge_base.RuleDetailScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.trends.PanicTrendsDashboardScreen
import com.example.ui.trends.TrendDashboardViewModel

@Composable
fun PanicLabNavGraph(
    navController: NavHostController,
    diagnosticRepository: DiagnosticRepository,
    kbRepository: KnowledgeBaseRepository,
    settingsRepository: SettingsRepository,
    analysisViewModel: AnalysisViewModel,
    historyViewModel: HistoryViewModel,
    kbViewModel: KnowledgeBaseViewModel,
    settingsViewModel: SettingsViewModel,
    trendDashboardViewModel: TrendDashboardViewModel,
    recentReports: List<DiagnosticReport>,
    kbVersion: String
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            AppleOfficialHomeEntry(
                onOpenAppleOfficial = { navController.navigate(Screen.AppleOfficialKnowledge.route) }
            ) {
                HomeScreen(
                    recentReports = recentReports,
                    kbVersion = kbVersion,
                    onNavigateToImportFile = { navController.navigate(Screen.ImportFile.route) },
                    onNavigateToPasteLog = { navController.navigate(Screen.PasteLog.route) },
                    onNavigateToCameraScanner = { navController.navigate(Screen.CameraScanner.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToKnowledgeBase = { navController.navigate(Screen.KnowledgeBase.route) },
                    onNavigateToTrends = { navController.navigate(Screen.TrendsDashboard.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToReport = { sessionId ->
                        navController.navigate(Screen.Result.createRoute(sessionId))
                    }
                )
            }
        }

        composable(Screen.ImportFile.route) {
            ImportFileScreen(
                viewModel = analysisViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCameraScanner = { navController.navigate(Screen.CameraScanner.route) },
                onAnalysisSuccess = { sessionId ->
                    navController.navigate(Screen.Result.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.PasteLog.route) {
            PasteLogScreen(
                viewModel = analysisViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCameraScanner = { navController.navigate(Screen.CameraScanner.route) },
                onAnalysisSuccess = { sessionId ->
                    navController.navigate(Screen.Result.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.CameraScanner.route) {
            CameraLogScannerScreen(
                viewModel = analysisViewModel,
                onNavigateBack = { navController.popBackStack() },
                onAnalysisSuccess = { sessionId ->
                    navController.navigate(Screen.Result.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            var report by remember { mutableStateOf<DiagnosticReport?>(null) }
            val repairState by analysisViewModel.repairSuggestionState.collectAsState()

            LaunchedEffect(sessionId) {
                report = diagnosticRepository.getSessionById(sessionId)
            }

            ResultScreen(
                report = report,
                repairSuggestionState = repairState,
                onFetchRepairSuggestions = {
                    report?.let { analysisViewModel.fetchRepairSuggestions(it) }
                },
                onAppendNotes = { notesToAdd ->
                    report?.let { r ->
                        val updated = if (r.technicianNotes.isNullOrBlank()) notesToAdd else "${r.technicianNotes}\n$notesToAdd"
                        analysisViewModel.saveTechnicianNotes(r.id, updated)
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToEvidence = {
                    navController.navigate(Screen.TechnicalEvidence.createRoute(sessionId))
                },
                onNavigateToLogViewer = { highlight ->
                    navController.navigate(Screen.LogViewer.createRoute(sessionId, highlight))
                },
                onNavigateToExport = {
                    navController.navigate(Screen.ExportReport.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.TechnicalEvidence.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            var report by remember { mutableStateOf<DiagnosticReport?>(null) }

            LaunchedEffect(sessionId) {
                report = diagnosticRepository.getSessionById(sessionId)
            }

            TechnicalEvidenceScreen(
                report = report,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogViewer = { line ->
                    navController.navigate(Screen.LogViewer.createRoute(sessionId, line))
                }
            )
        }

        composable(
            route = Screen.CaseDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            var report by remember { mutableStateOf<DiagnosticReport?>(null) }
            val currentKbVersion by kbViewModel.currentVersion.collectAsState()
            val historyRepairState by historyViewModel.repairSuggestionState.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(sessionId) {
                report = diagnosticRepository.getSessionById(sessionId)
            }

            CaseDetailScreen(
                report = report,
                currentKbVersion = currentKbVersion,
                repairSuggestionState = historyRepairState,
                onFetchRepairSuggestions = {
                    report?.let { historyViewModel.fetchRepairSuggestions(it) }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEvidence = {
                    navController.navigate(Screen.TechnicalEvidence.createRoute(sessionId))
                },
                onNavigateToLogViewer = {
                    navController.navigate(Screen.LogViewer.createRoute(sessionId, -1))
                },
                onNavigateToResult = { id ->
                    navController.navigate(Screen.Result.createRoute(id))
                },
                onSaveNotes = { id, notes ->
                    historyViewModel.saveTechnicianNotes(id, notes)
                },
                onReanalyze = {
                    historyViewModel.reanalyzeSession(sessionId) { result ->
                        if (result.isSuccess) {
                            report = result.getOrNull()
                        }
                    }
                },
                onExportPdf = {
                    navController.navigate(Screen.ExportReport.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.ExportReport.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            var report by remember { mutableStateOf<DiagnosticReport?>(null) }

            LaunchedEffect(sessionId) {
                report = diagnosticRepository.getSessionById(sessionId)
            }

            ExportReportScreen(
                report = report,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LogViewer.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("highlight") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val highlight = backStackEntry.arguments?.getInt("highlight") ?: -1
            var report by remember { mutableStateOf<DiagnosticReport?>(null) }

            LaunchedEffect(sessionId) {
                report = diagnosticRepository.getSessionById(sessionId)
            }

            LogViewerScreen(
                report = report,
                highlightLine = highlight,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectReport = { sessionId ->
                    navController.navigate(Screen.CaseDetail.createRoute(sessionId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToKnowledgeBase = {
                    navController.navigate(Screen.KnowledgeBase.route)
                },
                onNavigateToTrends = {
                    navController.navigate(Screen.TrendsDashboard.route)
                },
                onNavigateToImport = {
                    navController.navigate(Screen.ImportFile.route)
                }
            )
        }

        composable(Screen.TrendsDashboard.route) {
            PanicTrendsDashboardScreen(
                viewModel = trendDashboardViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRuleDetail = { ruleId ->
                    navController.navigate(Screen.RuleDetail.createRoute(ruleId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        composable(Screen.KnowledgeBase.route) {
            KnowledgeBaseScreen(
                viewModel = kbViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRulePacks = { navController.navigate(Screen.RulePacks.route) },
                onSelectRule = { ruleId ->
                    navController.navigate(Screen.RuleDetail.createRoute(ruleId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToImport = {
                    navController.navigate(Screen.ImportFile.route)
                }
            )
        }

        composable(Screen.AppleOfficialKnowledge.route) {
            val context = LocalContext.current
            val factory = remember(context) { AppleOfficialKnowledgeViewModelFactory(context) }
            val appleKnowledgeViewModel: AppleOfficialKnowledgeViewModel = viewModel(factory = factory)
            AppleOfficialKnowledgeScreen(
                viewModel = appleKnowledgeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RuleDetail.route,
            arguments = listOf(navArgument("ruleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId") ?: ""
            val rules by kbViewModel.rules.collectAsState()
            val rule = rules.find { it.id == ruleId }

            RuleDetailScreen(
                rule = rule,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RulePacks.route) {
            ManageRulePacksScreen(
                viewModel = kbViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
