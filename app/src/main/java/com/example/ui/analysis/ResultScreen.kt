package com.example.ui.analysis

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    report: DiagnosticReport?,
    onNavigateBack: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToLogViewer: (Int) -> Unit,
    onNavigateToExport: () -> Unit
) {
    val context = LocalContext.current

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

    val deviceName = report.deviceModel?.marketingName ?: report.productCode

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diagnóstico Técnico",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$deviceName • iOS ${report.osVersion}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("result_back_button")
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
                        onClick = onNavigateToExport,
                        modifier = Modifier.testTag("result_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportar",
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
                        onClick = onNavigateToEvidence,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TechDarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("result_bottom_evidence_button")
                    ) {
                        Icon(imageVector = Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyanLight)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Evidencia (${report.evidences.size})", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToExport,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("result_bottom_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar Informe", fontWeight = FontWeight.Bold)
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
            // Device & Panic Identifiers Strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HexagonMicroscopeEmblem(size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = deviceName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ElectricBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = report.productCode,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyanLight
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Base v${report.knowledgeBaseVersion} • ${report.panicFamilies.joinToString(", ") { it.name }}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        ConfidenceBadge(level = report.confidence)
                    }
                }
            }

            // Main Diagnostic Verdict Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_main_verdict_card"),
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
                            Text(
                                text = "VEREDICTO DETERMINISTA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight,
                                letterSpacing = 1.sp
                            )
                            if (report.primaryCandidate != null) {
                                VerificationBadge(status = report.primaryCandidate.verificationStatus)
                            }
                        }

                        Text(
                            text = report.primaryCandidate?.label ?: "Diagnóstico no concluyente",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = report.primaryCandidate?.interpretation ?: "No se encontraron coincidencias exactas en la base de reglas para este patrón de pánico.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        // Suspected components chips
                        if (report.primaryCandidate?.suspectedComponents?.isNotEmpty() == true) {
                            HorizontalDivider(color = TechDarkBorder)
                            Text(
                                text = "COMPONENTES SOSPECHOSOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                report.primaryCandidate.suspectedComponents.forEach { comp ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TechDarkSurface)
                                            .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(8.dp))
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

            // Recommended First Checks (Repair Flow)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PROCEDIMIENTO TÉCNICO SUGERIDO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF94A3B8)
                        )

                        report.repairFlow.firstChecks.forEachIndexed { index, step ->
                            RepairStepRow(index = index + 1, text = step)
                        }

                        if (!report.repairFlow.knownGoodTest.isNullOrBlank()) {
                            HorizontalDivider(color = TechDarkBorder)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Prueba con repuesto conocido:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = report.repairFlow.knownGoodTest,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Log Viewer Quick Action Card
            item {
                Surface(
                    onClick = { onNavigateToLogViewer(-1) },
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder),
                    modifier = Modifier.fillMaxWidth().testTag("result_log_viewer_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Visor de Registro Panic Full",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Navega y resalta las líneas exactas del pánico",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairStepRow(index: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(ElectricBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyanLight
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
