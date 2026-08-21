package com.example.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.DiagnosticReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generatePdf(
        context: Context,
        report: DiagnosticReport,
        redactIdentifiers: Boolean = false,
        includeRawLog: Boolean = false
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42) // Slate 900
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59)
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(51, 65, 85)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.rgb(71, 85, 105)
        }

        var y = 40f
        val margin = 36f
        val contentWidth = 595f - (margin * 2)

        // Header Background Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
        }
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + 60f, 6f, 6f, headerPaint)

        canvas.drawText("PanicLab — Informe Técnico de Diagnóstico", margin + 14f, y + 26f, titlePaint)

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
        val kbStr = "Base de Reglas: v${report.knowledgeBaseVersion} (Motor Determinista Local)"
        canvas.drawText("$dateStr  |  $kbStr", margin + 14f, y + 46f, textPaint)

        y += 75f

        // Device Info Card
        val cardPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
        }
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + 70f, 4f, 4f, cardPaint)

        val deviceTitle = "${report.deviceModel?.marketingName ?: report.productCode} (${report.productCode})"
        canvas.drawText("DISPOSITIVO Y ENTORNO", margin + 10f, y + 16f, subTitlePaint)

        canvas.drawText("Modelo:", margin + 10f, y + 34f, boldTextPaint)
        canvas.drawText(deviceTitle, margin + 70f, y + 34f, textPaint)

        canvas.drawText("iOS / Build:", margin + 10f, y + 50f, boldTextPaint)
        canvas.drawText("${report.osVersion} (${report.build})", margin + 70f, y + 50f, textPaint)

        canvas.drawText("Origen:", margin + 300f, y + 34f, boldTextPaint)
        val filename = if (redactIdentifiers) "[REDACTADO]" else (report.sourceFilename ?: "Entrada directa / Portapapeles")
        canvas.drawText(filename, margin + 350f, y + 34f, textPaint)

        canvas.drawText("Pánico:", margin + 300f, y + 50f, boldTextPaint)
        val panicFams = report.panicFamilies.joinToString(", ") { it.name }
        canvas.drawText(if (panicFams.length > 25) panicFams.take(25) + "..." else panicFams, margin + 350f, y + 50f, textPaint)

        y += 85f

        // Verdict & Suspected Component Box
        val verdictBoxPaint = Paint().apply {
            color = Color.rgb(238, 242, 255) // Indigo 50
        }
        canvas.drawRoundRect(margin, y, margin + contentWidth, y + 90f, 4f, 4f, verdictBoxPaint)

        canvas.drawText("DICTAMEN DE DIAGNÓSTICO", margin + 10f, y + 18f, subTitlePaint)

        val primaryLabel = report.primaryCandidate?.label ?: "Diagnóstico no concluyente"
        val verdictPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 58, 138)
        }
        canvas.drawText(primaryLabel, margin + 10f, y + 38f, verdictPaint)

        canvas.drawText("Confianza: ${report.confidence.name}  |  Estado: ${report.verificationStatus.name}  |  Subsistema: ${report.primaryCandidate?.subsystem ?: "GENERAL"}", margin + 10f, y + 56f, boldTextPaint)

        val componentsList = report.primaryCandidate?.suspectedComponents?.joinToString(", ") { "${it.name} (${it.role})" } ?: "No determinado"
        canvas.drawText("Componentes sospechosos: $componentsList", margin + 10f, y + 74f, textPaint)

        y += 105f

        // Technical Evidence Section
        canvas.drawText("EVIDENCIAS TÉCNICAS EXTRAÍDAS", margin, y, subTitlePaint)
        y += 16f

        if (report.evidences.isEmpty()) {
            canvas.drawText("No se detectaron códigos SMC ni sensores explícitos.", margin + 10f, y, textPaint)
            y += 16f
        } else {
            for (ev in report.evidences.take(4)) {
                val lineInfo = if (ev.lineNumber > 0) " [Línea ${ev.lineNumber}]" else ""
                canvas.drawText("• ${ev.title}$lineInfo", margin + 8f, y, boldTextPaint)
                y += 13f
                val excerptLine = ev.excerpt.replace("\n", " ").take(95)
                canvas.drawText("   $excerptLine", margin + 8f, y, textPaint)
                y += 15f
            }
        }

        y += 10f

        // Recommended Repair Protocol
        canvas.drawText("PROTOCOLO DE REPARACIÓN SUGERIDO", margin, y, subTitlePaint)
        y += 16f

        if (report.repairFlow.firstChecks.isNotEmpty()) {
            canvas.drawText("1. Primeras comprobaciones:", margin + 8f, y, boldTextPaint)
            y += 14f
            for (check in report.repairFlow.firstChecks.take(3)) {
                canvas.drawText("   - $check", margin + 12f, y, textPaint)
                y += 13f
            }
        }

        if (!report.repairFlow.knownGoodTest.isNullOrBlank()) {
            y += 4f
            canvas.drawText("2. Prueba con periférico conocido (Known-Good):", margin + 8f, y, boldTextPaint)
            y += 14f
            canvas.drawText("   ${report.repairFlow.knownGoodTest}", margin + 12f, y, textPaint)
            y += 14f
        }

        if (report.repairFlow.boardLevelNextSteps.isNotEmpty()) {
            y += 4f
            canvas.drawText("3. Pasos a nivel de placa (Microelectrónica):", margin + 8f, y, boldTextPaint)
            y += 14f
            for (step in report.repairFlow.boardLevelNextSteps.take(3)) {
                canvas.drawText("   - $step", margin + 12f, y, textPaint)
                y += 13f
            }
        }

        if (report.repairFlow.cautions.isNotEmpty()) {
            y += 4f
            canvas.drawText("Advertencias:", margin + 8f, y, boldTextPaint)
            y += 14f
            for (caution in report.repairFlow.cautions.take(2)) {
                canvas.drawText("   ⚠️ $caution", margin + 12f, y, textPaint)
                y += 13f
            }
        }

        // Footer disclaimer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = Color.rgb(148, 163, 184)
        }
        canvas.drawText("PanicLab es una herramienta de asistencia técnica offline. Los diagnósticos se generan mediante reglas deterministas locales.", margin, 810f, footerPaint)

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "PanicLab_Report_${report.id.take(8)}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
