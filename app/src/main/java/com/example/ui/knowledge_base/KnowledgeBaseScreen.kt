package com.example.ui.knowledge_base

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
import com.example.domain.model.DiagnosticRule
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.components.PanicCodeBadge
import com.example.ui.components.PanicLabBottomNavBar
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    viewModel: KnowledgeBaseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRulePacks: () -> Unit,
    onSelectRule: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedProfile by viewModel.selectedProfileFilter.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val kbVersion by viewModel.currentVersion.collectAsState()

    val profiles = listOf(
        "SMC_13",
        "SMC_13_MINI",
        "SMC_14_BASE",
        "SMC_14_PRO",
        "SMC_15_PRO",
        "SMC_16_PRO",
        "THERMAL_CLASSIC_X_TO_12"
    )

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Base de Reglas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Versión $kbVersion • ${rules.size} reglas deterministas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("kb_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = onNavigateToRulePacks,
                        shape = RoundedCornerShape(12.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .testTag("manage_rule_packs_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Source,
                                contentDescription = "Paquetes de Reglas",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
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
                selectedTab = 2,
                onNavigateToHome = onNavigateToHome,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToKnowledgeBase = {},
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
                        .testTag("kb_search_input"),
                    placeholder = {
                        Text(
                            "Buscar código (0x1000, 0x80000), sensor o componente...",
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

            // Diagnostic Profile Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { viewModel.setProfileFilter(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedProfile == null) ElectricBlueContainer else TechDarkCard,
                    border = BorderStroke(1.dp, if (selectedProfile == null) ElectricBlue else TechDarkBorder)
                ) {
                    Text(
                        text = "Todos",
                        fontSize = 12.sp,
                        fontWeight = if (selectedProfile == null) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedProfile == null) ElectricCyanLight else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                profiles.forEach { prof ->
                    val isSelected = selectedProfile == prof
                    Surface(
                        onClick = {
                            viewModel.setProfileFilter(if (isSelected) null else prof)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricBlueContainer else TechDarkCard,
                        border = BorderStroke(1.dp, if (isSelected) ElectricBlue else TechDarkBorder)
                    ) {
                        Text(
                            text = prof,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ElectricCyanLight else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No se encontraron reglas que coincidan con la búsqueda.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onClick = { onSelectRule(rule.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleItemCard(
    rule: DiagnosticRule,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kb_rule_item_${rule.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Sensor Code on Left, Status Badges on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rule.sensorCodesExact.isNotEmpty()) {
                    PanicCodeBadge(code = rule.sensorCodesExact.first())
                } else if (rule.sensorTokens.isNotEmpty()) {
                    PanicCodeBadge(code = rule.sensorTokens.first())
                } else {
                    PanicCodeBadge(code = "RULE")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConfidenceBadge(level = rule.confidence)
                    VerificationBadge(status = rule.verificationStatus)
                }
            }

            // Title on full width with proper line height and readability
            Text(
                text = rule.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            // Diagnosis Component Label
            Text(
                text = rule.diagnosis.label,
                fontSize = 13.sp,
                color = ElectricCyanLight,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )

            // Bottom Profile / Target Scope Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (rule.deviceScope.diagnosticProfiles.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechDarkSurface)
                                .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rule.deviceScope.diagnosticProfiles.first(),
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (rule.sensorTokens.isNotEmpty()) {
                        Text(
                            text = "Sensores: ${rule.sensorTokens.joinToString(", ")}",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ver detalle",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
