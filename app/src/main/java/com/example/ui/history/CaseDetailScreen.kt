package com.example.ui.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.PanicCodeBadge
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    report: DiagnosticReport?,
    onNavigateBack: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToLogViewer: () -> Unit,
    onReanalyze: () -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    var technicianNotes by remember { mutableStateOf("") }
    var notesSaved by remember { mutableStateOf(false) }

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

    val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    val deviceName = report.deviceModel?.marketingName ?: report.productCode

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detalle del Caso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ID: ${report.id.take(8)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("case_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.testTag("case_detail_export_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToLogViewer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TechDarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("case_detail_view_log_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Log", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("case_detail_export_pdf_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF", fontWeight = FontWeight.Bold)
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Summary Header Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INFORMACIÓN DEL REGISTRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = dateStr,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        if (!report.sourceFilename.isNullOrBlank()) {
                            Text(
                                text = "Archivo: ${report.sourceFilename}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Base de reglas instalada: v${report.knowledgeBaseVersion} (Determinista)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Device & Main Verdict Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("case_detail_verdict_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorderGlow)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HexagonMicroscopeEmblem(size = 38.dp)
                                Column {
                                    Text(
                                        text = deviceName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${report.productCode} • iOS ${report.osVersion}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            ConfidenceBadge(level = report.confidence)
                        }

                        HorizontalDivider(color = TechDarkBorder)

                        Text(
                            text = report.primaryCandidate?.label ?: "Diagnóstico no concluyente",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = report.primaryCandidate?.interpretation ?: "Sin evidencias suficientes para emitir un diagnóstico con alta confianza.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        if (report.primaryCandidate?.suspectedComponents?.isNotEmpty() == true) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                report.primaryCandidate.suspectedComponents.forEach { comp ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElectricBlue.copy(alpha = 0.15f))
                                            .border(BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${comp.name} (${comp.role})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElectricCyanLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action: Reanalyze & Evidence Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onNavigateToEvidence,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("case_detail_evidence_btn"),
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Troubleshoot, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Evidencia (${report.evidences.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Surface(
                        onClick = onReanalyze,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("case_detail_reanalyze_btn"),
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reanalizar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Technician Notes Field
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOTAS DEL TÉCNICO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF94A3B8)
                            )
                            if (notesSaved) {
                                Text(
                                    text = "✓ Guardado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConfidenceHigh
                                )
                            }
                        }

                        OutlinedTextField(
                            value = technicianNotes,
                            onValueChange = {
                                technicianNotes = it
                                notesSaved = false
                            },
                            placeholder = {
                                Text(
                                    text = "Escribe observaciones del microscopio, mediciones con multímetro o estado del flex...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .testTag("technician_notes_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = TechDarkBg,
                                unfocusedContainerColor = TechDarkBg,
                                focusedIndicatorColor = ElectricBlue,
                                unfocusedIndicatorColor = TechDarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                notesSaved = true
                                Toast.makeText(context, "Notas guardadas localmente", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueContainer),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("save_notes_button")
                        ) {
                            Text("Guardar Nota", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyanLight)
                        }
                    }
                }
            }
        }
    }
}
