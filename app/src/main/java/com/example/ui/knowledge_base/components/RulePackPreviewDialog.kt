package com.example.ui.knowledge_base.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun RulePackPreviewDialog(
    validationResult: RulePackValidationResult,
    parsedPack: ParsedRulePack,
    diffSummary: RulePackDiffSummary,
    filename: String?,
    isInstalling: Boolean,
    onConfirmInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<RuleDiffItem.ChangeType?>(null) }
    var expandedRuleId by remember { mutableStateOf<String?>(null) }

    val filteredRules = remember(diffSummary.ruleDiffs, selectedFilter) {
        if (selectedFilter == null) diffSummary.ruleDiffs
        else diffSummary.ruleDiffs.filter { it.changeType == selectedFilter }
    }

    Dialog(
        onDismissRequest = { if (!isInstalling) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = TechDarkBg,
            border = BorderStroke(1.dp, TechDarkBorderGlow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Vista Previa de Paquete",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricCyanLight.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v${diffSummary.currentVersion} ➔ v${diffSummary.incomingVersion}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyanLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = parsedPack.title.ifBlank { filename ?: "Paquete de reglas" },
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        modifier = Modifier.testTag("close_preview_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Checksum & Origin info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "SHA-256: ${validationResult.checksum.take(12)}...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = "Locale: ${parsedPack.locale.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Diff Statistics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        count = diffSummary.addedRulesCount,
                        label = "Nuevas",
                        color = Color(0xFF10B981),
                        isSelected = selectedFilter == RuleDiffItem.ChangeType.ADDED,
                        onClick = {
                            selectedFilter = if (selectedFilter == RuleDiffItem.ChangeType.ADDED) null else RuleDiffItem.ChangeType.ADDED
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        count = diffSummary.modifiedRulesCount,
                        label = "Modificadas",
                        color = Color(0xFFF59E0B),
                        isSelected = selectedFilter == RuleDiffItem.ChangeType.MODIFIED,
                        onClick = {
                            selectedFilter = if (selectedFilter == RuleDiffItem.ChangeType.MODIFIED) null else RuleDiffItem.ChangeType.MODIFIED
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        count = diffSummary.deactivatedRulesCount + diffSummary.removedRulesCount,
                        label = "Desactivadas",
                        color = Color(0xFFEF4444),
                        isSelected = selectedFilter == RuleDiffItem.ChangeType.DEACTIVATED || selectedFilter == RuleDiffItem.ChangeType.REMOVED,
                        onClick = {
                            selectedFilter = if (selectedFilter == RuleDiffItem.ChangeType.DEACTIVATED) null else RuleDiffItem.ChangeType.DEACTIVATED
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Warnings section (if any)
                if (validationResult.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Text(
                                text = "${validationResult.warnings.size} advertencia(s) no bloqueantes en la validación.",
                                fontSize = 11.sp,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CAMBIOS DETALLADOS (${filteredRules.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Detailed Rules Diff List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredRules.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay reglas en esta categoría de cambio.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(filteredRules, key = { it.ruleId }) { diff ->
                            RuleDiffCard(
                                item = diff,
                                isExpanded = expandedRuleId == diff.ruleId,
                                onToggle = {
                                    expandedRuleId = if (expandedRuleId == diff.ruleId) null else diff.ruleId
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancelar", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onConfirmInstall,
                        enabled = !isInstalling,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("confirm_install_rulepack_button")
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instalando...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instalar v${parsedPack.knowledgeBaseVersion}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    count: Int,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else TechDarkCard,
        border = BorderStroke(1.dp, if (isSelected) color else TechDarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun RuleDiffCard(
    item: RuleDiffItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val (chipColor, chipText) = when (item.changeType) {
        RuleDiffItem.ChangeType.ADDED -> Color(0xFF10B981) to "NUEVA"
        RuleDiffItem.ChangeType.MODIFIED -> Color(0xFFF59E0B) to "MODIFICADA"
        RuleDiffItem.ChangeType.DEACTIVATED -> Color(0xFFEF4444) to "DESACTIVADA"
        RuleDiffItem.ChangeType.REMOVED -> Color(0xFFEF4444) to "ELIMINADA"
        RuleDiffItem.ChangeType.UNCHANGED -> Color(0xFF64748B) to "SIN CAMBIOS"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, if (isExpanded) ElectricBlue.copy(alpha = 0.5f) else TechDarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = chipColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = chipText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = chipColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = item.ruleId,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TechDarkBorder)
                Spacer(modifier = Modifier.height(8.dp))

                item.details.forEach { detail ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "•", color = ElectricCyanLight, fontSize = 12.sp)
                        Text(text = detail, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }

                if (item.details.isEmpty()) {
                    Text(
                        text = item.newSummary ?: "Sin cambios semánticos adicionales.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
