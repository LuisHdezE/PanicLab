package com.example.ui.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DiagnosticReport
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.PanicLabBottomNavBar
import com.example.ui.components.PanicLabTopHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    recentReports: List<DiagnosticReport>,
    kbVersion: String,
    onNavigateToImportFile: () -> Unit,
    onNavigateToPasteLog: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToKnowledgeBase: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReport: (String) -> Unit
) {
    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            PanicLabTopHeader(
                onSearchClick = onNavigateToKnowledgeBase,
                onSettingsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            PanicLabBottomNavBar(
                selectedTab = 0,
                onNavigateToHome = {},
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToKnowledgeBase = onNavigateToKnowledgeBase,
                onNavigateToImport = onNavigateToImportFile
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // 2x2 Primary Action Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionGridCard(
                            title = "Analizar archivo",
                            subtitle = "Subir .ips, .txt o .log",
                            badgeText = "Subir",
                            icon = Icons.Default.FileUpload,
                            accentColor = ElectricBlue,
                            onClick = onNavigateToImportFile,
                            tag = "home_upload_card",
                            modifier = Modifier.weight(1f)
                        )
                        ActionGridCard(
                            title = "Pegar log",
                            subtitle = "Texto copiado del panic",
                            badgeText = "Pegar",
                            icon = Icons.Default.ContentPaste,
                            accentColor = ElectricCyan,
                            onClick = onNavigateToPasteLog,
                            tag = "home_paste_card",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionGridCard(
                            title = "Historial",
                            subtitle = "Casos previos guardados",
                            badgeText = "Ver",
                            icon = Icons.Default.History,
                            accentColor = Color(0xFFA855F7),
                            onClick = onNavigateToHistory,
                            tag = "home_history_card",
                            modifier = Modifier.weight(1f)
                        )
                        ActionGridCard(
                            title = "Base de reglas",
                            subtitle = "Sensores y modelos",
                            badgeText = "Explorar",
                            icon = Icons.Default.MenuBook,
                            accentColor = Color(0xFFF59E0B),
                            onClick = onNavigateToKnowledgeBase,
                            tag = "home_kb_card",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Access Filter Pills
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ACCESOS RÁPIDOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAccessChip(
                            label = "Buscar código (0x1000, 0x80000...)",
                            icon = Icons.Default.Code,
                            onClick = onNavigateToKnowledgeBase
                        )
                        QuickAccessChip(
                            label = "Modelos iPhone 11 - 16",
                            icon = Icons.Default.Smartphone,
                            onClick = onNavigateToKnowledgeBase
                        )
                        QuickAccessChip(
                            label = "Todos los casos",
                            icon = Icons.Default.Folder,
                            onClick = onNavigateToHistory
                        )
                    }
                }
            }

            // Recent Diagnostics Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIAGNÓSTICOS RECIENTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if (recentReports.isNotEmpty()) {
                        Text(
                            text = "Ver todos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyanLight,
                            modifier = Modifier
                                .clickable(onClick = onNavigateToHistory)
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (recentReports.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TechDarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Sin diagnósticos recientes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Importa un archivo .ips o pega un log para comenzar.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(recentReports.take(5), key = { it.id }) { session ->
                    RecentSessionCard(
                        report = session,
                        onClick = { onNavigateToReport(session.id) }
                    )
                }
            }

            // Knowledge Base Installed Banner
            item {
                Surface(
                    onClick = onNavigateToKnowledgeBase,
                    shape = RoundedCornerShape(18.dp),
                    color = TechDarkCardElevated,
                    border = BorderStroke(1.dp, TechDarkBorderGlow),
                    modifier = Modifier.fillMaxWidth().testTag("kb_active_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f))
                                .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ElectricCyanLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Base Activa v$kbVersion Determinista",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Reglas SMC, I2C, térmicos y PMU verificadas offline",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGridCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = modifier
            .height(138.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TechDarkSurface)
                        .border(1.dp, TechDarkBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAccessChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyanLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    report: DiagnosticReport,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    val deviceName = report.deviceModel?.marketingName ?: report.productCode

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_item_${report.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
