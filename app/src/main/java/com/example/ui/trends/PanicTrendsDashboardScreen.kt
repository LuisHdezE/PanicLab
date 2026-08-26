package com.example.ui.trends

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.PanicCodeStat
import com.example.domain.model.TechnicianTrendDashboardData
import com.example.ui.components.ConfidenceBadge
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicTrendsDashboardScreen(
    viewModel: TrendDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRuleDetail: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val dashboardData by viewModel.dashboardData.collectAsState()
    val selectedModel by viewModel.selectedModelFilter.collectAsState()
    val selectedTime by viewModel.selectedTimeFilter.collectAsState()
    val currentMode by viewModel.currentViewMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var selectedCodeDetails by remember { mutableStateOf<PanicCodeStat?>(null) }
    var showSeedConfirmDialog by remember { mutableStateOf(false) }

    // Synchronize D3 WebView when data or view mode changes
    LaunchedEffect(dashboardData) {
        dashboardData?.let { data ->
            val json = data.toJsonString()
            webViewRef?.let { webView ->
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript("if (window.updateDashboardData) { updateDashboardData($json); }", null)
                }
            }
        }
    }

    if (showSeedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSeedConfirmDialog = false },
            title = { Text("Cargar Casos de Muestra", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Se insertarán 6 casos representativos de fallas comunes de iPhone (Mic2 0x1000, Barómetro 0x80000, NTC Batería 0x400000, etc.) en la base de Room para visualizar la distribución y correlaciones en el motor D3.",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.seedSampleWorkshopCases()
                        showSeedConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_seed_samples_button")
                ) {
                    Text("Cargar Casos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSeedConfirmDialog = false },
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

    // Modal Sheet for clicked Panic Code from D3 chart
    selectedCodeDetails?.let { stat ->
        AlertDialog(
            onDismissRequest = { selectedCodeDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .border(1.dp, ElectricCyanLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(18.dp))
                    }
                    Text(text = stat.code, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = stat.label, fontWeight = FontWeight.SemiBold, color = ElectricCyanLight, fontSize = 14.sp)
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TechDarkSurface,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ocurrencias:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${stat.count} casos (${Math.round(stat.percentage)}%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subsistema:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text(stat.subsystem, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (stat.affectedModels.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Modelos:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(stat.affectedModels.joinToString(", "), color = Color(0xFFE2E8F0), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!stat.diodeModeHint.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricBlueContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Straighten, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(16.dp))
                                Text(text = "Guía Multímetro: ${stat.diodeModeHint}", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!stat.ruleId.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val ruleId = stat.ruleId
                            selectedCodeDetails = null
                            onNavigateToRuleDetail(ruleId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("view_rule_from_trend_modal_button")
                    ) {
                        Text("Ver Regla en Base", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { selectedCodeDetails = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedCodeDetails = null },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TechDarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cerrar")
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Tendencias de Fallas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricCyanLight.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "D3.js",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyanLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${dashboardData?.totalCases ?: 0} casos en Room • ${dashboardData?.totalUniqueCodes ?: 0} códigos detectados",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("trend_dashboard_back_button")
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
                        onClick = { showSeedConfirmDialog = true },
                        modifier = Modifier.testTag("seed_sample_cases_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PostAdd,
                            contentDescription = "Cargar Casos de Muestra",
                            tint = ElectricCyanLight
                        )
                    }
                    IconButton(
                        onClick = {
                            shareTrendsSummary(context, dashboardData)
                        },
                        modifier = Modifier.testTag("share_trends_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir Reporte de Tendencias",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBg)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 4-Card KPI Ribbon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "TOTAL CASOS",
                        value = "${dashboardData?.totalCases ?: 0}",
                        subtext = "En base Room",
                        icon = Icons.Default.Folder,
                        accentColor = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "CÓDIGO #1",
                        value = dashboardData?.mostFrequentCode?.code ?: "N/A",
                        subtext = dashboardData?.mostFrequentCode?.let { "${Math.round(it.percentage)}% frecuencia" } ?: "Sin datos",
                        icon = Icons.Default.Bolt,
                        accentColor = ElectricCyanLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "SUBSISTEMA CRÍTICO",
                        value = dashboardData?.mostFailingSubsystem?.label?.take(16) ?: "N/A",
                        subtext = dashboardData?.mostFailingSubsystem?.let { "${it.count} fallas" } ?: "Sin datos",
                        icon = Icons.Default.Build,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "CERTEZA PROMEDIO",
                        value = "${dashboardData?.averageConfidencePercent ?: 0}%",
                        subtext = "Diagnóstico Certero",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick View Mode Selector Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewModeTabPill(
                        title = "📊 Códigos",
                        isSelected = currentMode == "codes",
                        onClick = {
                            viewModel.setViewMode("codes")
                            webViewRef?.evaluateJavascript("switchViewMode('codes')", null)
                        },
                        tag = "tab_mode_codes"
                    )
                    ViewModeTabPill(
                        title = "🍩 Subsistemas",
                        isSelected = currentMode == "subsystems",
                        onClick = {
                            viewModel.setViewMode("subsystems")
                            webViewRef?.evaluateJavascript("switchViewMode('subsystems')", null)
                        },
                        tag = "tab_mode_subsystems"
                    )
                    ViewModeTabPill(
                        title = "📱 Por Modelo",
                        isSelected = currentMode == "models",
                        onClick = {
                            viewModel.setViewMode("models")
                            webViewRef?.evaluateJavascript("switchViewMode('models')", null)
                        },
                        tag = "tab_mode_models"
                    )
                    ViewModeTabPill(
                        title = "📈 Temporal",
                        isSelected = currentMode == "timeline",
                        onClick = {
                            viewModel.setViewMode("timeline")
                            webViewRef?.evaluateJavascript("switchViewMode('timeline')", null)
                        },
                        tag = "tab_mode_timeline"
                    )
                }
            }

            // D3 WebView Container Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.5.dp, TechDarkBorderGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .testTag("d3_webview_container_card")
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                setBackgroundColor(0x00000000) // Transparent

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        dashboardData?.let { data ->
                                            val json = data.toJsonString()
                                            view?.evaluateJavascript("if (window.updateDashboardData) { updateDashboardData($json); }", null)
                                        }
                                    }
                                }

                                addJavascriptInterface(
                                    AndroidD3Bridge(
                                        onCodeClicked = { code, ruleId ->
                                            val stat = dashboardData?.panicCodeStats?.find { it.code.contains(code, ignoreCase = true) }
                                                ?: PanicCodeStat(code = code, label = "Código $code", count = 1, percentage = 100f, ruleId = ruleId, subsystem = "Hardware", affectedModels = emptyList())
                                            selectedCodeDetails = stat
                                        },
                                        onModelFilterSelected = { model ->
                                            viewModel.setModelFilter(model)
                                        },
                                        onTimeFilterSelected = { time ->
                                            viewModel.setTimeFilter(time)
                                        },
                                        onRequestInitialData = {
                                            dashboardData?.let { data ->
                                                val json = data.toJsonString()
                                                post {
                                                    evaluateJavascript("if (window.updateDashboardData) { updateDashboardData($json); }", null)
                                                }
                                            }
                                        }
                                    ),
                                    "AndroidBridge"
                                )

                                loadUrl("file:///android_asset/trends_dashboard.html")
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Frequency Breakdown List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANKING DE CÓDIGOS DE PÁNICO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if ((dashboardData?.totalCases ?: 0) == 0) {
                        TextButton(
                            onClick = { showSeedConfirmDialog = true },
                            modifier = Modifier.testTag("load_sample_data_text_button")
                        ) {
                            Text("Cargar Ejemplos", fontSize = 12.sp, color = ElectricCyanLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if ((dashboardData?.panicCodeStats ?: emptyList()).isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(40.dp))
                            Text("Sin diagnósticos suficientes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Importa registros .ips o carga casos de muestra para visualizar tendencias de hardware.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showSeedConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_state_seed_button")
                            ) {
                                Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cargar Datos de Muestra de Taller", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(dashboardData?.panicCodeStats ?: emptyList()) { stat ->
                    PanicCodeBreakdownCard(
                        stat = stat,
                        onClick = {
                            selectedCodeDetails = stat
                        },
                        onViewRule = {
                            stat.ruleId?.let { onNavigateToRuleDetail(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = modifier.height(108.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }

            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtext,
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
private fun ViewModeTabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ElectricBlueContainer else TechDarkCard,
        border = BorderStroke(1.dp, if (isSelected) ElectricCyanLight else TechDarkBorder),
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ElectricCyanLight else Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PanicCodeBreakdownCard(
    stat: PanicCodeStat,
    onClick: () -> Unit,
    onViewRule: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trend_code_card_${stat.code}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .border(1.dp, ElectricCyanLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stat.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyanLight
                        )
                    }

                    Text(
                        text = stat.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${stat.count} casos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Progress Bar representing percentage
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ElectricCyanLight,
                    trackColor = TechDarkSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subsistema: ${stat.subsystem}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${Math.round(stat.percentage)}% del total",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyanLight
                    )
                }
            }

            if (!stat.diodeModeHint.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TechDarkSurface,
                    border = BorderStroke(1.dp, TechDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Straighten, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                        Text(
                            text = stat.diodeModeHint,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * JavaScript Interface to communicate between D3 WebView and Android Compose
 */
class AndroidD3Bridge(
    private val onCodeClicked: (code: String, ruleId: String) -> Unit,
    private val onModelFilterSelected: (model: String) -> Unit,
    private val onTimeFilterSelected: (time: String) -> Unit,
    private val onRequestInitialData: () -> Unit
) {
    @JavascriptInterface
    fun onCodeSelected(code: String, ruleId: String) {
        onCodeClicked(code, ruleId)
    }

    @JavascriptInterface
    fun onModelFilterChanged(model: String) {
        onModelFilterSelected(model)
    }

    @JavascriptInterface
    fun onTimeFilterChanged(time: String) {
        onTimeFilterSelected(time)
    }

    @JavascriptInterface
    fun requestInitialData() {
        onRequestInitialData()
    }
}

private fun shareTrendsSummary(context: Context, data: TechnicianTrendDashboardData?) {
    if (data == null || data.totalCases == 0) return
    val text = buildString {
        appendLine("📊 REPORTE DE TENDENCIAS DE HARDWARE - PANICLAB PRO")
        appendLine("--------------------------------------------------")
        appendLine("• Total Diagnósticos Analizados: ${data.totalCases}")
        appendLine("• Código de Pánico más Frecuente: ${data.mostFrequentCode?.code ?: "N/A"} (${data.mostFrequentCode?.label ?: ""})")
        appendLine("• Subsistema Crítico: ${data.mostFailingSubsystem?.label ?: "N/A"} (${data.mostFailingSubsystem?.count ?: 0} fallas)")
        appendLine("• Certeza Promedio: ${data.averageConfidencePercent}%")
        appendLine()
        appendLine("Top Fallas Recurrentes:")
        data.panicCodeStats.take(5).forEachIndexed { i, stat ->
            appendLine("  ${i + 1}. ${stat.code} - ${stat.label} (${stat.count} casos, ${Math.round(stat.percentage)}%)")
        }
        appendLine()
        appendLine("Generado con PanicLab Offline Diagnostics Engine")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Reporte de Tendencias PanicLab")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir Reporte"))
}
