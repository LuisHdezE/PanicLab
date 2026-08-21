package com.example.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    report: DiagnosticReport?,
    highlightLine: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val logContent = report?.rawLog ?: report?.panicStringSummary ?: "Log no disponible en almacenamiento local."
    val lines = remember(logContent) { logContent.lines() }

    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    val matchingLinesCount = remember(searchQuery, lines) {
        if (searchQuery.isBlank()) 0
        else lines.count { it.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(highlightLine) {
        if (highlightLine in 1..lines.size) {
            listState.animateScrollToItem(maxOf(0, highlightLine - 1))
        }
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Visor de Log Panic Full",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${lines.size} líneas • ${report?.productCode ?: ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("log_viewer_back_button")
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
                        onClick = {
                            clipboardManager.setText(AnnotatedString(logContent))
                            Toast.makeText(context, "Log completo copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .testTag("log_viewer_copy_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Todo",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search filter bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = TechDarkCard,
                border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) ElectricBlue else TechDarkBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("log_search_field"),
                        placeholder = {
                            Text(
                                "Buscar en el log (ej. SMC, 0x1000, caller)...",
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
                                IconButton(onClick = { searchQuery = "" }) {
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

                    if (searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlueContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$matchingLinesCount coincidencias",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight
                            )
                        }
                    }
                }
            }

            // Terminal Code Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TechDarkSurface)
                    .border(1.dp, TechDarkBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val lineNum = index + 1
                    val isTarget = lineNum == highlightLine
                    val matchesSearch = searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)

                    val bgColor = when {
                        isTarget -> ElectricBlue.copy(alpha = 0.35f)
                        matchesSearch -> Color(0xFF854D0E).copy(alpha = 0.45f)
                        index % 2 == 0 -> Color.Transparent
                        else -> Color(0xFF1E293B).copy(alpha = 0.3f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(vertical = 2.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = lineNum.toString().padStart(4, ' '),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTarget) ElectricCyanLight else Color(0xFF64748B),
                            modifier = Modifier.width(36.dp)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .width(1.dp)
                                .height(16.dp)
                                .background(TechDarkBorder)
                        )
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = when {
                                isTarget -> Color.White
                                matchesSearch -> Color(0xFFFDE047)
                                else -> Color(0xFFCBD5E1)
                            },
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
