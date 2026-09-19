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
import com.example.export.TextSummaryOptions
import com.example.export.TextSummaryReportGenerator
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.components.PanicCodeBadge
import com.example.ui.components.RepairSuggestionsSection
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.*
import androidx.compose.runtime.*
import com.example.domain.model.RepairSuggestionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    report: DiagnosticReport?,
    onNavigateBack: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToLogViewer: (Int) -> Unit,
    onNavigateToExport: () -> Unit,
    repairSuggestionState: RepairSuggestionUiState = RepairSuggestionUiState.Idle,
    onFetchRepairSuggestions: (() -> Unit)? = null,
    onAppendNotes: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showCustomerReportSheet by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var technicianName by remember { mutableStateOf("") }
    var technicianNotes by remember { mutableStateOf("") }
    var includeRepairSteps by remember { mutableStateOf(true) }
    var includeEvidences by remember { mutableStateOf(true) }

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
                            report.primaryCandidate?.let { candidate ->
                                VerificationBadge(status = candidate.verificationStatus)
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
                                report.primaryCandidate?.suspectedComponents?.forEach { comp ->
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
                                    text = report.repairFlow.knownGoodTest.orEmpty(),
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                RepairSuggestionsSection(
                    report = report,
                    state = repairSuggestionState,
                    onFetchSuggestions = {
                        onFetchRepairSuggestions?.invoke()
                    },
                    onAppendToNotes = { notesToAppend ->
                        onAppendNotes?.invoke(notesToAppend)
                    }
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BASE DE REGLAS: v${report.knowledgeBaseVersion}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight,
                                letterSpacing = 1.sp
                            )
                            if (report.confidence == com.example.domain.model.ConfidenceLevel.LOW || report.primaryCandidate == null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "CASO NOVEDOSO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val json = com.example.util.UnknownCaseRedactor.generateUnknownCaseJson(report, "")
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        putExtra(Intent.EXTRA_TITLE, "Caso PanicLab (${report.productCode})")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Exportar Caso Anonimizado"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("result_export_anonymized_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ShareLocation, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar Caso Anonimizado (JSON)", fontSize = 12.sp, color = ElectricCyanLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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

            item {
                val quickSummaryText = remember(report, customerName, technicianName, technicianNotes, includeRepairSteps, includeEvidences) {
                    TextSummaryReportGenerator.generateCustomerSummary(
                        report = report,
                        options = TextSummaryOptions(
                            customerName = customerName.takeIf { it.isNotBlank() },
                            technicianOrShopName = technicianName.takeIf { it.isNotBlank() },
                            customNotes = technicianNotes.takeIf { it.isNotBlank() },
                            includeRepairSteps = includeRepairSteps,
                            includeTechnicalEvidences = includeEvidences
                        )
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_text_report_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorderGlow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = ElectricCyanLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "RESUMEN PARA CLIENTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyanLight,
                                    letterSpacing = 1.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "TEXTO / WHATSAPP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyanLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Genera un informe en texto estructurado y comprensible para enviar directamente a tu cliente por WhatsApp, Telegram o correo.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    TextSummaryReportGenerator.shareReport(
                                        context = context,
                                        reportText = quickSummaryText,
                                        title = "Informe de Diagnóstico - $deviceName"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("result_share_text_report_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    TextSummaryReportGenerator.copyToClipboard(
                                        context = context,
                                        reportText = quickSummaryText,
                                        message = "Resumen copiado para enviar a tu cliente"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TechDarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("result_copy_text_report_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = ElectricCyanLight
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { showCustomerReportSheet = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TechDarkSurface)
                                    .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(12.dp))
                                    .testTag("result_customize_text_report_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Personalizar Informe",
                                    tint = ElectricCyanLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomerReportSheet) {
        CustomerReportBottomSheet(
            report = report,
            customerName = customerName,
            onCustomerNameChange = { customerName = it },
            technicianName = technicianName,
            onTechnicianNameChange = { technicianName = it },
            technicianNotes = technicianNotes,
            onTechnicianNotesChange = { technicianNotes = it },
            includeRepairSteps = includeRepairSteps,
            onIncludeRepairStepsChange = { includeRepairSteps = it },
            includeEvidences = includeEvidences,
            onIncludeEvidencesChange = { includeEvidences = it },
            onDismiss = { showCustomerReportSheet = false },
            onShare = { text ->
                TextSummaryReportGenerator.shareReport(
                    context = context,
                    reportText = text,
                    title = "Informe de Diagnóstico - $deviceName"
                )
            },
            onCopy = { text ->
                TextSummaryReportGenerator.copyToClipboard(
                    context = context,
                    reportText = text,
                    message = "Informe copiado al portapapeles"
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerReportBottomSheet(
    report: DiagnosticReport,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    technicianName: String,
    onTechnicianNameChange: (String) -> Unit,
    technicianNotes: String,
    onTechnicianNotesChange: (String) -> Unit,
    includeRepairSteps: Boolean,
    onIncludeRepairStepsChange: (Boolean) -> Unit,
    includeEvidences: Boolean,
    onIncludeEvidencesChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val generatedReport = remember(
        report,
        customerName,
        technicianName,
        technicianNotes,
        includeRepairSteps,
        includeEvidences
    ) {
        TextSummaryReportGenerator.generateCustomerSummary(
            report = report,
            options = TextSummaryOptions(
                customerName = customerName.takeIf { it.isNotBlank() },
                technicianOrShopName = technicianName.takeIf { it.isNotBlank() },
                customNotes = technicianNotes.takeIf { it.isNotBlank() },
                includeRepairSteps = includeRepairSteps,
                includeTechnicalEvidences = includeEvidences
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TechDarkSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF64748B))
        },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Informe para Cliente",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Personaliza los datos antes de compartir o copiar",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = onCustomerNameChange,
                        label = { Text("Nombre del Cliente (Opcional)") },
                        placeholder = { Text("Ej. Juan Pérez") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ElectricCyanLight)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = TechDarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = ElectricCyanLight,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = technicianName,
                        onValueChange = onTechnicianNameChange,
                        label = { Text("Taller / Nombre del Técnico (Opcional)") },
                        placeholder = { Text("Ej. TechLab Express") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = ElectricCyanLight)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = TechDarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = ElectricCyanLight,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_technician_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = technicianNotes,
                        onValueChange = onTechnicianNotesChange,
                        label = { Text("Observaciones / Cotización para el Cliente") },
                        placeholder = { Text("Ej. Se requiere cambio de flex de carga. Tiempo estimado: 1 hora.") },
                        minLines = 2,
                        maxLines = 4,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Comment, contentDescription = null, tint = ElectricCyanLight)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = TechDarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = ElectricCyanLight,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_technician_notes")
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Incluir pasos técnicos sugeridos",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Switch(
                                    checked = includeRepairSteps,
                                    onCheckedChange = onIncludeRepairStepsChange,
                                    modifier = Modifier.testTag("switch_include_repair_steps")
                                )
                            }
                            HorizontalDivider(color = TechDarkBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Incluir evidencias del pánico",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Switch(
                                    checked = includeEvidences,
                                    onCheckedChange = onIncludeEvidencesChange,
                                    modifier = Modifier.testTag("switch_include_evidences")
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "VISTA PREVIA DEL TEXTO A ENVIAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyanLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_report_preview_box"),
                        shape = RoundedCornerShape(14.dp),
                        color = TechDarkBg,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Text(
                            text = generatedReport,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onCopy(generatedReport) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TechDarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("customer_report_copy_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Texto", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onShare(generatedReport) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("customer_report_share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir", fontWeight = FontWeight.Bold)
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
