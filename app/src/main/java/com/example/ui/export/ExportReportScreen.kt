package com.example.ui.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.DiagnosticReport
import com.example.export.PdfReportGenerator
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportScreen(
    report: DiagnosticReport?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var redactIdentifiers by remember { mutableStateOf(true) }
    var includeFullLog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    if (report == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TechDarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ElectricBlue)
        }
        return
    }

    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    val deviceName = report.deviceModel?.marketingName ?: report.productCode

    fun generateAndSharePdf() {
        isGenerating = true
        try {
            val pdfFile = PdfReportGenerator.generatePdf(
                context = context,
                report = report,
                redactIdentifiers = redactIdentifiers,
                includeRawLog = includeFullLog
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar Informe PanicLab"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isGenerating = false
        }
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exportar Informe",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("export_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechDarkBg
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = TechDarkSurface,
                border = BorderStroke(1.dp, TechDarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { generateAndSharePdf() },
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_pdf_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compilando PDF...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar y Compartir PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // PDF Document Sheet Visual Preview
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PanicLab Technical Report",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Diagnostic Summary • $dateStr",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            HexagonMicroscopeEmblem(size = 32.dp, surfaceBg = Color(0xFFF1F5F9))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text(
                            text = "Dispositivo: $deviceName (${if (redactIdentifiers) report.productCode else "${report.productCode} • ${report.osVersion}"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Text(
                            text = "Veredicto: ${report.primaryCandidate?.label ?: "Diagnóstico no concluyente"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )

                        Text(
                            text = report.primaryCandidate?.interpretation ?: "Análisis técnico de sensor array.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )

                        if (report.repairFlow.firstChecks.isNotEmpty()) {
                            Text(
                                text = "Primeras comprobaciones: ${report.repairFlow.firstChecks.firstOrNull()}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Export Options Configuration
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "OPCIONES DE EXPORTACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Redactar identificadores privados",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Oculta CrashReporter Key, Incident UUID y nombres de archivo personales.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = redactIdentifiers,
                                onCheckedChange = { redactIdentifiers = it },
                                modifier = Modifier.testTag("switch_redact_identifiers")
                            )
                        }

                        HorizontalDivider(color = TechDarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Incluir log Panic Full en anexo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Adjunta las líneas de texto del registro para revisión de otros laboratorios.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = includeFullLog,
                                onCheckedChange = { includeFullLog = it },
                                modifier = Modifier.testTag("switch_include_full_log")
                            )
                        }
                    }
                }
            }

            // Local privacy disclaimer
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricCyanLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "El PDF se genera localmente mediante Canvas nativo de Android sin enviar información a servidores externos.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
