package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.GroundedRepairSuggestion
import com.example.domain.model.RepairSuggestionUiState
import com.example.domain.model.SearchGroundingSource
import com.example.ui.theme.*

@Composable
fun RepairSuggestionsSection(
    report: DiagnosticReport,
    state: RepairSuggestionUiState,
    onFetchSuggestions: () -> Unit,
    onAppendToNotes: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("repair_suggestions_section_card"),
        shape = RoundedCornerShape(22.dp),
        color = TechDarkCard,
        border = BorderStroke(1.dp, TechDarkBorderGlow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section Header with Grounding Badge
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = ElectricCyanLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SUGERENCIAS DE REPARACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyanLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Google Search Grounding & Microsoldadura",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricBlue.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, ElectricCyanLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ElectricCyanLight,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "WEB GROUNDED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyanLight
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state,
                label = "repair_suggestion_state_transition"
            ) { currentState ->
                when (currentState) {
                    is RepairSuggestionUiState.Idle -> {
                        IdleSuggestionsView(
                            onFetch = onFetchSuggestions
                        )
                    }
                    is RepairSuggestionUiState.Loading -> {
                        LoadingSuggestionsView()
                    }
                    is RepairSuggestionUiState.Success -> {
                        SuccessSuggestionsView(
                            suggestion = currentState.suggestion,
                            onRefresh = onFetchSuggestions,
                            onCopy = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Sugerencias copiadas al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            onAppendToNotes = onAppendToNotes?.let { appendFn ->
                                { textToAppend ->
                                    appendFn(textToAppend)
                                    Toast.makeText(context, "Añadido a las notas del caso", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenUrl = { url ->
                                openBrowserUrl(context, url)
                            }
                        )
                    }
                    is RepairSuggestionUiState.Error -> {
                        ErrorSuggestionsView(
                            errorMessage = currentState.message,
                            fallbackSuggestion = currentState.fallbackSuggestion,
                            onRetry = onFetchSuggestions,
                            onOpenUrl = { url ->
                                openBrowserUrl(context, url)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleSuggestionsView(
    onFetch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TechDarkSurface)
            .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Obtén recomendaciones de taller en tiempo real",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Consulta foros de microsoldadura, diagramas esquemáticos y soluciones comprobadas de la comunidad indexadas por Google para este código de pánico.",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 16.sp
        )

        Button(
            onClick = onFetch,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_fetch_repair_suggestions")
        ) {
            Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar Soluciones y Prácticas en Vivo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingSuggestionsView() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TechDarkSurface)
            .border(BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.15f * alpha))
                .border(BorderStroke(1.5.dp, ElectricCyanLight.copy(alpha = alpha)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = ElectricCyanLight,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Consultando Google Search Grounding...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Indexando casos de microsoldadura, líneas I2C y esquemáticos",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ElectricCyanLight,
            trackColor = TechDarkBorder
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessSuggestionsView(
    suggestion: GroundedRepairSuggestion,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onAppendToNotes: ((String) -> Unit)?,
    onOpenUrl: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Grounding status indicator banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (suggestion.isRealTimeGrounded) ElectricBlue.copy(alpha = 0.12f)
                    else Color(0xFFEAB308).copy(alpha = 0.12f)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (suggestion.isRealTimeGrounded) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (suggestion.isRealTimeGrounded) ElectricCyanLight else Color(0xFFFDE047),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (suggestion.isRealTimeGrounded) "Resultados fundamentados con Google Search en vivo"
                           else "Guía de reparación de hardware asistida (Offline)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (suggestion.isRealTimeGrounded) Color.White else Color(0xFFFEF08A)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar Búsqueda",
                    tint = ElectricCyanLight,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Search Queries Chips (if available)
        if (suggestion.searchQueries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CONSULTAS REALIZADAS EN GOOGLE:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestion.searchQueries.take(2).forEach { query ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TechDarkSurface,
                            border = BorderStroke(0.5.dp, TechDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ElectricCyanLight,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = query,
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailed Repair Steps
        if (suggestion.detailedSteps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TechDarkSurface)
                    .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PROCEDIMIENTO DE TALLER PASO A PASO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyanLight,
                    letterSpacing = 0.5.sp
                )

                suggestion.detailedSteps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight
                            )
                        }
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Suspected Components & Schematic Lines
        if (suggestion.suspectedComponents.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TechDarkSurface)
                    .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LÍNEAS Y COMPONENTES A INSPECCIONAR (ESQUEMÁTICO)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestion.suspectedComponents.forEach { comp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = comp,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyanLight
                            )
                        }
                    }
                }
            }
        }

        // Diode Mode Reference Tips
        if (suggestion.diodeModeReferenceTips.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1B4B).copy(alpha = 0.4f))
                    .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "MEDICIONES EN MODO DIODO Y VOLTAJES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC),
                        letterSpacing = 0.5.sp
                    )
                }

                suggestion.diodeModeReferenceTips.forEach { tip ->
                    Text(
                        text = "• $tip",
                        fontSize = 11.sp,
                        color = Color(0xFFE0E7FF),
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Safety Cautions
        if (suggestion.cautions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF450A0A).copy(alpha = 0.3f))
                    .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "PRECAUCIONES CRÍTICAS DE SEGURIDAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }
                suggestion.cautions.forEach { caution ->
                    Text(
                        text = "⚠ $caution",
                        fontSize = 11.sp,
                        color = Color(0xFFFECACA),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Grounding Web Sources & Citations
        if (suggestion.searchSources.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FUENTES TÉCNICAS Y CITAS WEB (GOOGLE GROUNDING):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )

                suggestion.searchSources.forEach { source ->
                    Surface(
                        onClick = { onOpenUrl(source.url) },
                        shape = RoundedCornerShape(10.dp),
                        color = TechDarkSurface,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier.fillMaxWidth().testTag("grounding_source_item")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = ElectricCyanLight,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = source.url,
                                    fontSize = 10.sp,
                                    color = ElectricCyanLight.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val fullText = buildString {
                        append("--- SUGERENCIAS DE REPARACIÓN (PANICLAB) ---\n")
                        append(suggestion.summary)
                        if (suggestion.searchSources.isNotEmpty()) {
                            append("\n\nFuentes consultadas:\n")
                            suggestion.searchSources.forEach { append("- ${it.title}: ${it.url}\n") }
                        }
                    }
                    onCopy(fullText)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TechDarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_copy_repair_suggestions")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricCyanLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar Guía", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (onAppendToNotes != null) {
                Button(
                    onClick = {
                        val textToAppend = buildString {
                            append("\n\n[Sugerencias de Reparación en Vivo]:\n")
                            suggestion.detailedSteps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
                            if (suggestion.diodeModeReferenceTips.isNotEmpty()) {
                                append("Mediciones: ${suggestion.diodeModeReferenceTips.joinToString("; ")}\n")
                            }
                        }
                        onAppendToNotes(textToAppend)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueContainer),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp)
                        .testTag("btn_append_repair_suggestions_to_notes")
                ) {
                    Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricCyanLight)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir a Notas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyanLight)
                }
            }
        }
    }
}

@Composable
private fun ErrorSuggestionsView(
    errorMessage: String,
    fallbackSuggestion: GroundedRepairSuggestion?,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TechDarkSurface)
            .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Text(
                text = "No se pudieron obtener resultados en vivo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFCA5A5)
            )
        }
        Text(
            text = errorMessage,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reintentar Consulta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el enlace: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
