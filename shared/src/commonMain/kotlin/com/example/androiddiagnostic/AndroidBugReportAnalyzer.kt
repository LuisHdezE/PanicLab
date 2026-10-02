package com.example.androiddiagnostic

import com.example.artifact.AndroidBugReportImporter
import com.example.artifact.AndroidDiagnosticSession
import com.example.artifact.AndroidEvidenceExtractor
import com.example.artifact.AndroidEvidenceReport
import com.example.artifact.BugReportArchiveReader
import com.example.artifact.BugReportImportLimits

data class AndroidAnalysisResult(
    val session: AndroidDiagnosticSession,
    val evidenceReport: AndroidEvidenceReport,
    val diagnosticReport: AndroidDiagnosticReport
)

class AndroidBugReportAnalyzer(
    private val importer: AndroidBugReportImporter = AndroidBugReportImporter(),
    private val evidenceExtractor: AndroidEvidenceExtractor = AndroidEvidenceExtractor(),
    private val diagnosticEngine: DeterministicAndroidDiagnosticEngine = DeterministicAndroidDiagnosticEngine()
) {
    fun analyzeText(sourceFilename: String, text: String): AndroidAnalysisResult =
        analyze(importer.importText(sourceFilename, text))

    fun analyzeArchive(
        sourceFilename: String,
        bytes: ByteArray,
        archiveReader: BugReportArchiveReader,
        limits: BugReportImportLimits = BugReportImportLimits()
    ): AndroidAnalysisResult =
        analyze(importer.importArchive(sourceFilename, bytes, archiveReader, limits))

    private fun analyze(session: AndroidDiagnosticSession): AndroidAnalysisResult {
        val evidenceReport = evidenceExtractor.extract(session)
        val diagnosticReport = diagnosticEngine.diagnose(evidenceReport)

        return AndroidAnalysisResult(
            session = session,
            evidenceReport = evidenceReport,
            diagnosticReport = diagnosticReport
        )
    }
}
