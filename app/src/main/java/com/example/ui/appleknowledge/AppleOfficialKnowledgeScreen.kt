package com.example.ui.appleknowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appleknowledge.model.AppleCapability
import com.example.appleknowledge.model.AppleCapabilityAvailability
import com.example.appleknowledge.model.AppleKnowledgeCategory
import com.example.appleknowledge.query.AppleKnowledgeCardView
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceResolution
import com.example.appleknowledge.query.AppleKnowledgeSourceReferenceView
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.TechDarkBg
import com.example.ui.theme.TechDarkBorder
import com.example.ui.theme.TechDarkCard
import com.example.ui.theme.TechDarkCardElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleOfficialKnowledgeScreen(
    viewModel: AppleOfficialKnowledgeViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var expandedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Apple Oficial", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .testTag("apple_official_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TechDarkCardElevated,
                    border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Verified, null, tint = ElectricCyanLight)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                "Contexto oficial Apple",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Esta capa muestra documentación, capacidades y antecedentes auditados. No cambia la causa, el score, la confianza ni el Rule Pack del diagnóstico Panic Full.",
                                color = Color(0xFFB8C4D6),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = modelMenuExpanded,
                    onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.selectedModel.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modelo exacto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("apple_official_model_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        state.exactModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    modelMenuExpanded = false
                                    expandedCardId = null
                                    viewModel.selectModel(model)
                                }
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            state.error?.let { message ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Text(message, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::retry) { Text("Reintentar") }
                        }
                    }
                }
            }

            state.modelView?.let { modelView ->
                item {
                    val capability = modelView.capability
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                modelView.exactModel,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${capability.family} · ${capability.releaseYears} · verificado ${capability.verifiedAt}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            HorizontalDivider(color = TechDarkBorder)
                            CapabilityRow("Repair Manual", capability.publicRepairManual)
                            CapabilityRow("Apple Diagnostics SSR", capability.diagnosticsSsr)
                            CapabilityRow("Diagnostics Mode", capability.recoveryDiagnosticsMode)
                            CapabilityRow("Repair Assistant", capability.repairAssistant)
                            CapabilityRow("Troubleshooting", capability.troubleshooting)
                            CapabilityTextRow(
                                "Historial de piezas",
                                capability.partsServiceHistoryCapabilities.joinToString(" · ").ifBlank { "No documentado para este modelo" }
                            )
                            TextButton(onClick = { uriHandler.openUri(capability.primaryOfficialUrl) }) {
                                Icon(Icons.Default.OpenInNew, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Abrir referencia principal Apple")
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "FILTRAR FICHAS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("Todas") }
                            )
                            AppleKnowledgeCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = state.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = { Text(categoryLabel(category)) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "${modelView.cards.size} fichas aplicables",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                items(modelView.cards, key = { it.id }) { card ->
                    AppleKnowledgeCard(
                        card = card,
                        expanded = expandedCardId == card.id,
                        onToggle = {
                            expandedCardId = if (expandedCardId == card.id) null else card.id
                        },
                        onOpenUrl = uriHandler::openUri
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, capability: AppleCapability) {
    CapabilityTextRow(label, availabilityLabel(capability.availability), capability.notes)
}

@Composable
private fun CapabilityTextRow(label: String, value: String, note: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.width(128.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (!note.isNullOrBlank()) {
                Text(note, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun AppleKnowledgeCard(
    card: AppleKnowledgeCardView,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("apple_official_card_${card.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(categoryLabel(card.category), color = ElectricCyanLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(card.subcategory, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(card.modelScope, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                SourceStatusBadge(card)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = Color(0xFF94A3B8)
                )
            }

            if (card.symptoms.isNotBlank()) {
                Text(card.symptoms, color = Color(0xFFD5DDE8), fontSize = 12.sp, lineHeight = 17.sp)
            }

            if (expanded) {
                HorizontalDivider(color = TechDarkBorder)
                DetailBlock("Comprobaciones rápidas", card.quickChecks)
                DetailBlock("Diagnóstico Apple", card.appleDiagnostics)
                DetailBlock("Inspección / descarte", card.inspectionOrDiscard)
                DetailBlock("Acción Apple", card.appleAction)
                DetailBlock("Correlación con PanicLab", card.panicLabCorrelation)
                DetailBlock("Aplicabilidad", card.applicabilityNotes)

                Text("FUENTES", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                SourceReferenceRow(card.primarySource, onOpenUrl)
                card.secondarySources.forEach { SourceReferenceRow(it, onOpenUrl) }
                Text(
                    "Verificado ${card.verifiedAt} · Rule Pack effect: NONE",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SourceStatusBadge(card: AppleKnowledgeCardView) {
    val historical = card.sourceStatus.name != "CURRENT" || card.contextualSourceStatuses.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (historical) Color(0xFF7C2D12).copy(alpha = 0.5f) else Color(0xFF0F766E).copy(alpha = 0.4f)
    ) {
        Text(
            if (historical) "Contexto histórico" else "Actual",
            color = if (historical) Color(0xFFFDBA74) else Color(0xFF99F6E4),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SourceReferenceRow(
    reference: AppleKnowledgeSourceReferenceView,
    onOpenUrl: (String) -> Unit
) {
    val unresolved = reference.resolution != AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TechDarkCardElevated,
        border = BorderStroke(1.dp, if (unresolved) Color(0xFFF59E0B).copy(alpha = 0.4f) else TechDarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onOpenUrl(reference.url) }
                .padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Link, null, tint = ElectricCyanLight, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${reference.role.name} · ${resolutionLabel(reference.resolution)}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(reference.url, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 2)
                if (reference.exactSourceIds.isNotEmpty()) {
                    Text(reference.exactSourceIds.joinToString(), color = Color(0xFF64748B), fontSize = 9.sp)
                } else if (reference.candidateSourceIds.isNotEmpty()) {
                    Text("Candidatos: ${reference.candidateSourceIds.joinToString()}", color = Color(0xFFFBBF24), fontSize = 9.sp)
                }
            }
            Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DetailBlock(title: String, body: String) {
    if (body.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Color(0xFFD5DDE8), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

private fun availabilityLabel(value: AppleCapabilityAvailability) = when (value) {
    AppleCapabilityAvailability.SUPPORTED -> "Soportado"
    AppleCapabilityAvailability.NOT_SUPPORTED -> "No soportado"
    AppleCapabilityAvailability.NOT_FOUND_PUBLICLY -> "No encontrado públicamente"
    AppleCapabilityAvailability.MODEL_OR_REGION_DEPENDENT -> "Depende de modelo o región"
}

private fun resolutionLabel(value: AppleKnowledgeSourceReferenceResolution) = when (value) {
    AppleKnowledgeSourceReferenceResolution.EXACT_SINGLE -> "Fuente exacta"
    AppleKnowledgeSourceReferenceResolution.EXACT_AMBIGUOUS -> "Coincidencia ambigua"
    AppleKnowledgeSourceReferenceResolution.LOCALE_PATH_CANDIDATE -> "Candidato por artículo/locale"
    AppleKnowledgeSourceReferenceResolution.UNMAPPED -> "Sin ID normalizado"
}

private fun categoryLabel(category: AppleKnowledgeCategory) = when (category) {
    AppleKnowledgeCategory.BATTERY_CHARGING_POWER -> "Batería / carga / power"
    AppleKnowledgeCategory.CAMERA -> "Cámara"
    AppleKnowledgeCategory.DISPLAY -> "Pantalla"
    AppleKnowledgeCategory.DISPLAY_SENSORS -> "Pantalla / sensores"
    AppleKnowledgeCategory.MECHANICAL -> "Mecánico"
    AppleKnowledgeCategory.SOUND -> "Audio"
    AppleKnowledgeCategory.REPAIR_ECOSYSTEM -> "Ecosistema de reparación"
    AppleKnowledgeCategory.CONNECTIVITY_POWER -> "Conectividad / power"
    AppleKnowledgeCategory.MODEL_GUARDRAILS -> "Guardrails de modelo"
    AppleKnowledgeCategory.REPAIR_HISTORY -> "Historial de reparación"
    AppleKnowledgeCategory.KNOWN_ISSUE -> "Known issue"
    AppleKnowledgeCategory.BIOMETRICS -> "Biometría"
    AppleKnowledgeCategory.CONNECTIVITY -> "Conectividad"
    AppleKnowledgeCategory.HISTORICAL_SERVICE_PROGRAM -> "Programa histórico"
    AppleKnowledgeCategory.BUTTONS -> "Botones"
    AppleKnowledgeCategory.OTHER -> "Otros"
}
