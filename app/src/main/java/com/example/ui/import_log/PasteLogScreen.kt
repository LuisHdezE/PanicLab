package com.example.ui.import_log

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.analysis.AnalysisUiState
import com.example.ui.analysis.AnalysisViewModel
import com.example.ui.analysis.AnalyzingScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteLogScreen(
    viewModel: AnalysisViewModel,
    onNavigateBack: () -> Unit,
    onAnalysisSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()

    var logText by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AnalysisUiState.Success) {
            val report = (uiState as AnalysisUiState.Success).report
            onAnalysisSuccess(report.id)
            viewModel.resetState()
        }
    }

    if (uiState is AnalysisUiState.Analyzing) {
        AnalyzingScreen(currentStepText = "Procesando texto y emparejando reglas deterministas...")
        return
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pegar Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("paste_log_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (logText.isNotEmpty()) {
                        IconButton(
                            onClick = { logText = "" },
                            modifier = Modifier.testTag("paste_clear_button")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (logText.isNotBlank()) {
                                viewModel.analyzeRawLog(logText, "pasted_panic.ips")
                            } else {
                                Toast.makeText(context, "Por favor pega o ingresa el texto del log primero.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = logText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("analyze_pasted_log_button")
                    ) {
                        Icon(imageVector = Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analizar log pegado >",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Quick Paste Action Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                logText = clipText
                                Toast.makeText(context, "Texto pegado desde el portapapeles", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("paste_clipboard_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pegar Portapapeles", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Surface(
                        onClick = {
                            logText = sampleSMC13MiniLog
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("paste_sample_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargar Ejemplo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Dark Code Input Surface
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.5.dp, if (logText.isNotEmpty()) ElectricBlue.copy(alpha = 0.6f) else TechDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        TextField(
                            value = logText,
                            onValueChange = { logText = it },
                            placeholder = {
                                Text(
                                    text = "Pega aquí el contenido de un log Panic Full...\n\n" +
                                            "Ejemplo:\n" +
                                            "{\"product\":\"iPhone14,4\",\"panicString\":\"panic(cpu 1 caller ...): SMC PANIC - BSC failure at address 0x1000...\", ...}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp, max = 400.dp)
                                .testTag("paste_log_text_field"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = ElectricCyanLight,
                                lineHeight = 16.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        if (logText.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${logText.lines().size} líneas • ${logText.length} caracteres",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Formato detectado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyanLight
                                )
                            }
                        }
                    }
                }
            }

            // Sample Log Preset Chips
            item {
                Text(
                    text = "PLANTILLAS RÁPIDAS DE PRUEBA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SamplePresetChip(
                        label = "iPhone 13 Mini (SMC 0x1000 Dock)",
                        onClick = { logText = sampleSMC13MiniLog }
                    )
                    SamplePresetChip(
                        label = "iPhone 14 Pro (SMC 0x80000 Micrófono)",
                        onClick = { logText = sampleSMC14ProLog }
                    )
                    SamplePresetChip(
                        label = "iPhone 11 (Térmico Prs0 Barómetro)",
                        onClick = { logText = sampleThermalLog }
                    )
                }
            }
        }
    }
}

@Composable
private fun SamplePresetChip(label: String, onClick: () -> Unit) {
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
            Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(14.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

private val sampleSMC13MiniLog = """
{"bug_type":"210","timestamp":"2024-03-15 14:22:01.00 +0000","os_version":"iPhone OS 17.4 (21E236)","incident_id":"A1B2C3D4-E5F6-7890-ABCD-EF1234567890"}
{
  "build" : "iPhone OS 17.4 (21E236)",
  "product" : "iPhone14,4",
  "socId" : "0x8110",
  "socRevision" : "0x11",
  "incident" : "A1B2C3D4-E5F6-7890-ABCD-EF1234567890",
  "crashReporterKey" : "abcdef0123456789abcdef0123456789abcdef01",
  "kernel" : "Darwin Kernel Version 23.4.0: Wed Feb 21 21:05:07 PST 2024; root:xnu-10063.101.17~2\/RELEASE_ARM64_T8110",
  "date" : "2024-03-15 14:22:01.00 +0000",
  "panicString" : "panic(cpu 1 caller 0xfffffff01e5239a0): SMC PANIC - BSC failure at address 0x1000 - [Sensor] SMC BSC failure! sensor array: 0x0 0x1000 0x0 0x0\nPanicked task 0xffffffe1981e4b80: 0 pages, 234 threads: pid 0: kernel_task\n"
}
""".trimIndent()

private val sampleSMC14ProLog = """
{"bug_type":"210","os_version":"iPhone OS 17.2","product":"iPhone15,2"}
{
  "product" : "iPhone15,2",
  "build" : "21C62",
  "panicString" : "panic(cpu 0 caller 0xfffffff011223344): SMC PANIC - BSC failure 0x80000 - mic3 thermal failure\n"
}
""".trimIndent()

private val sampleThermalLog = """
{"bug_type":"210","os_version":"iPhone OS 16.5","product":"iPhone12,1"}
{
  "product" : "iPhone12,1",
  "build" : "20F66",
  "panicString" : "panic(cpu 0 caller 0xfffffff011112222): Thermal mitigation failure: sensor [Prs0] timeout on i2c bus 3\n"
}
""".trimIndent()
