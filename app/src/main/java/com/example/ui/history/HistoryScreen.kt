package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiagnosticReport
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.PanicLabBottomNavBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onSelectReport: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToKnowledgeBase: () -> Unit,
    onNavigateToTrends: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedConfidence by viewModel.selectedConfidenceFilter.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Vaciar Historial", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("¿Deseas eliminar permanentemente todas las sesiones de diagnóstico guardadas en la base local?", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCaution),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Eliminar Todo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TechDarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = TechDarkCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Historial de Casos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${sessions.size} casos registrados en Room",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
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
                        onClick = onNavigateToTrends,
                        modifier = Modifier.testTag("history_trends_dashboard_button")
                    ) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = "Ver Dashboard de Tendencias D3", tint = ElectricCyanLight)
                    }
                    if (sessions.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Vaciar Historial", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechDarkBg
                )
            )
        },
        bottomBar = {
            PanicLabBottomNavBar(
                selectedTab = 1,
                onNavigateToHome = onNavigateToHome,
                onNavigateToHistory = {},
                onNavigateToKnowledgeBase = onNavigateToKnowledgeBase,
                onNavigateToImport = onNavigateToImport
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = TechDarkCard,
                border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) ElectricBlue else TechDarkBorder)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    placeholder = {
                        Text(
                            "Buscar dispositivo, código SMC o síntoma...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ElectricCyanLight,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Confidence Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { viewModel.setConfidenceFilter(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedConfidence == null) ElectricBlueContainer else TechDarkCard,
                    border = BorderStroke(1.dp, if (selectedConfidence == null) ElectricBlue else TechDarkBorder)
                ) {
                    Text(
                        text = "Todos",
                        fontSize = 12.sp,
                        fontWeight = if (selectedConfidence == null) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedConfidence == null) ElectricCyanLight else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                ConfidenceLevel.values().forEach { level ->
                    val isSelected = selectedConfidence == level
                    Surface(
                        onClick = {
                            viewModel.setConfidenceFilter(if (isSelected) null else level)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricBlueContainer else TechDarkCard,
                        border = BorderStroke(1.dp, if (isSelected) ElectricBlue else TechDarkBorder)
                    ) {
                        Text(
                            text = level.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ElectricCyanLight else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // D3 Trends Dashboard quick launch banner
            Surface(
                onClick = onNavigateToTrends,
                shape = RoundedCornerShape(16.dp),
                color = ElectricBlueContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_open_d3_trends_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricCyanLight.copy(alpha = 0.2f))
                                .border(1.dp, ElectricCyanLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = ElectricCyanLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tendencias de Falla D3",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Frecuencia de códigos y correlación de modelos",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ElectricCyanLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TechDarkCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No se encontraron diagnósticos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los reportes analizados aparecerán aquí.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        HistorySessionItem(
                            report = session,
                            onClick = { onSelectReport(session.id) },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionItem(
    report: DiagnosticReport,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    val deviceName = report.deviceModel?.marketingName ?: report.productCode

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${report.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TechDarkSurface)
                    .border(1.dp, TechDarkBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = deviceName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "• $dateStr",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = report.primaryCandidate?.label ?: "Diagnóstico no concluyente",
                    fontSize = 13.sp,
                    color = ElectricCyanLight,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConfidenceBadge(level = report.confidence)
                    Text(
                        text = report.productCode,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    if (!report.sourceFilename.isNullOrBlank()) {
                        Text(
                            text = "• ${report.sourceFilename}",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_session_${report.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = Color(0xFF64748B)
                )
            }
        }
    }
}
