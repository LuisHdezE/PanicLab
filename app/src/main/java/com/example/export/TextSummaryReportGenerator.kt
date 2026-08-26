package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.VerificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TextSummaryOptions(
    val customerName: String? = null,
    val technicianOrShopName: String? = null,
    val customNotes: String? = null,
    val includeRepairSteps: Boolean = true,
    val includeTechnicalEvidences: Boolean = true,
    val includeAlternativeCandidates: Boolean = false,
    val redactSensitiveData: Boolean = true
)

object TextSummaryReportGenerator {

    /**
     * Generates a comprehensive, human-readable diagnostic report formatted
     * for technicians to share with customers via messaging apps (WhatsApp, Telegram, SMS, Email).
     */
    fun generateCustomerSummary(
        report: DiagnosticReport,
        options: TextSummaryOptions = TextSummaryOptions()
    ): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
        val deviceName = report.deviceModel?.marketingName ?: report.productCode
        val primary = report.primaryCandidate

        return buildString {
            appendLine("==========================================")
            appendLine("📱 PANICLAB - INFORME TÉCNICO DE DIAGNÓSTICO")
            appendLine("==========================================")
            appendLine("📅 Fecha: $dateStr")
            if (!options.technicianOrShopName.isNullOrBlank()) {
                appendLine("🏢 Servicio Técnico: ${options.technicianOrShopName.trim()}")
            }
            if (!options.customerName.isNullOrBlank()) {
                appendLine("👤 Cliente: ${options.customerName.trim()}")
            }
            appendLine()

            appendLine("📱 INFORMACIÓN DEL DISPOSITIVO")
            appendLine("• Modelo: $deviceName")
            appendLine("• Identificador: ${report.productCode}")
            appendLine("• Sistema Operativo: iOS ${report.osVersion} (${report.build})")
            if (report.panicFamilies.isNotEmpty()) {
                val familiesStr = report.panicFamilies.joinToString(", ") { it.name }
                appendLine("• Categoría de Pánico: $familiesStr")
            }
            appendLine()

            appendLine("🔍 VEREDICTO DE DIAGNÓSTICO")
            val diagnosisLabel = primary?.label ?: "Diagnóstico no concluyente / En investigación"
            appendLine("• Falla detectada: $diagnosisLabel")
            if (primary != null) {
                appendLine("• Subsistema: ${primary.subsystem}")
            }
            val confidenceStr = when (report.confidence) {
                ConfidenceLevel.HIGH -> "ALTA (Certeza técnica directa comprobada)"
                ConfidenceLevel.MEDIUM -> "MEDIA (Probabilidad respaldada por patrones)"
                ConfidenceLevel.LOW -> "BAJA (Requiere descarte de componentes)"
                ConfidenceLevel.UNKNOWN -> "NO CONCLUYENTE (Requiere inspección en microscopio)"
            }
            appendLine("• Certeza del diagnóstico: $confidenceStr")

            val verificationStr = when (report.verificationStatus) {
                VerificationStatus.VERIFIED -> "Verificado en laboratorio físico"
                VerificationStatus.WELL_DOCUMENTED -> "Ampliamente documentado"
                VerificationStatus.COMMUNITY_SUPPORTED -> "Soporte comunitario"
                VerificationStatus.CONFLICTING_SOURCE -> "Fuentes con divergencia"
                VerificationStatus.EXPERIMENTAL -> "Patrón experimental"
                VerificationStatus.UNKNOWN -> "Sin verificar"
            }
            appendLine("• Nivel de validación: $verificationStr")
            appendLine()

            appendLine("📝 EXPLICACIÓN TÉCNICA PARA EL CLIENTE")
            val interpretation = primary?.interpretation
                ?: "El registro Panic Full no contiene códigos de sensor conocidos o el fallo requiere una inspección manual de líneas primarias de alimentación."
            appendLine(interpretation)
            appendLine()

            // Suspected hardware components
            if (primary?.suspectedComponents?.isNotEmpty() == true) {
                appendLine("🛠️ COMPONENTES AFECTADOS / A REVISAR")
                primary.suspectedComponents.forEachIndexed { idx, comp ->
                    val roleTag = when (comp.role.uppercase()) {
                        "PRIMARY" -> "Principal causante"
                        "SECONDARY" -> "Causante secundario"
                        "ALTERNATIVE" -> "Alternativa de descarte"
                        else -> comp.role
                    }
                    appendLine("  ${idx + 1}. ${comp.name} — [$roleTag]")
                }
                appendLine()
            }

            // Evidence section
            if (options.includeTechnicalEvidences && report.evidences.isNotEmpty()) {
                appendLine("🔬 EVIDENCIAS EXTRAÍDAS DEL REGISTRO")
                report.evidences.take(4).forEach { ev ->
                    appendLine("• ${ev.title}: ${ev.normalizedValue}")
                    if (ev.excerpt.isNotBlank()) {
                        val cleanExcerpt = ev.excerpt.replace("\n", " ").trim()
                        val truncatedExcerpt = if (cleanExcerpt.length > 80) cleanExcerpt.take(80) + "..." else cleanExcerpt
                        appendLine("  └ Línea: \"$truncatedExcerpt\"")
                    }
                }
                appendLine()
            }

            // Repair flow / steps
            if (options.includeRepairSteps && report.repairFlow.firstChecks.isNotEmpty()) {
                appendLine("💡 PROCEDIMIENTO TÉCNICO RECOMENDADO")
                report.repairFlow.firstChecks.forEachIndexed { idx, step ->
                    appendLine("  ${idx + 1}. $step")
                }
                if (!report.repairFlow.knownGoodTest.isNullOrBlank()) {
                    appendLine("• Prueba con repuesto conocido: ${report.repairFlow.knownGoodTest}")
                }
                appendLine()
            }

            // Alternative candidates if enabled
            if (options.includeAlternativeCandidates && report.alternativeCandidates.isNotEmpty()) {
                appendLine("🔄 OTRAS HIPÓTESIS SECUNDARIAS")
                report.alternativeCandidates.take(3).forEach { alt ->
                    appendLine("• ${alt.label} (${alt.subsystem})")
                }
                appendLine()
            }

            // Custom technician notes
            if (!options.customNotes.isNullOrBlank()) {
                appendLine("📋 OBSERVACIONES DEL TALLER")
                appendLine(options.customNotes.trim())
                appendLine()
            }

            appendLine("------------------------------------------")
            appendLine("ℹ️ Informe generado por PanicLab (Motor Local Determinista)")
            appendLine("🔒 La información de este diagnóstico es confidencial y para fines de servicio técnico.")
            appendLine("==========================================")
        }
    }

    /**
     * Generates a compact summary text (ideal for SMS or quick chat messages).
     */
    fun generateCompactSummary(report: DiagnosticReport): String {
        val deviceName = report.deviceModel?.marketingName ?: report.productCode
        val primary = report.primaryCandidate
        val label = primary?.label ?: "Diagnóstico no concluyente"
        val comp = primary?.suspectedComponents?.firstOrNull()?.name ?: "Componente no identificado"

        return buildString {
            appendLine("📋 Diagnóstico PanicLab: $deviceName")
            appendLine("• Fallo: $label")
            appendLine("• Componente sospechoso: $comp")
            appendLine("• Certeza: ${report.confidence.name}")
            if (report.repairFlow.firstChecks.isNotEmpty()) {
                appendLine("• Primer paso: ${report.repairFlow.firstChecks.first()}")
            }
        }
    }

    /**
     * Opens Android system share sheet with the plain text report.
     */
    fun shareReport(context: Context, reportText: String, title: String = "Informe de Diagnóstico PanicLab") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir informe con el cliente"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies report to clipboard and displays user confirmation Toast.
     */
    fun copyToClipboard(context: Context, reportText: String, message: String = "Resumen de diagnóstico copiado al portapapeles") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Informe PanicLab", reportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al copiar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
