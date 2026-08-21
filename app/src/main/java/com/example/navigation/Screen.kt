package com.example.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object ImportFile : Screen("import_file")
    object PasteLog : Screen("paste_log")
    object Result : Screen("result/{sessionId}") {
        fun createRoute(sessionId: String) = "result/$sessionId"
    }
    object TechnicalEvidence : Screen("technical_evidence/{sessionId}") {
        fun createRoute(sessionId: String) = "technical_evidence/$sessionId"
    }
    object LogViewer : Screen("log_viewer/{sessionId}?highlight={highlight}") {
        fun createRoute(sessionId: String, highlight: Int = -1) = "log_viewer/$sessionId?highlight=$highlight"
    }
    object History : Screen("history")
    object CaseDetail : Screen("case_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "case_detail/$sessionId"
    }
    object KnowledgeBase : Screen("knowledge_base")
    object RuleDetail : Screen("rule_detail/{ruleId}") {
        fun createRoute(ruleId: String) = "rule_detail/$ruleId"
    }
    object RulePacks : Screen("rule_packs")
    object ExportReport : Screen("export_report/{sessionId}") {
        fun createRoute(sessionId: String) = "export_report/$sessionId"
    }
    object Settings : Screen("settings")
}
