package com.example.ui.import_log

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.analysis.AnalysisUiState
import com.example.ui.analysis.AnalysisViewModel
import com.example.ui.analysis.AnalyzingScreen
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.theme.*
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFileScreen(
    viewModel: AnalysisViewModel,
    onNavigateBack: () -> Unit,
    onAnalysisSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileSizeKb by remember { mutableStateOf<Long?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                var name = uri.lastPathSegment ?: "panic_log.ips"
                if (name.contains("/")) name = name.substringAfterLast("/")
                selectedFileName = name
                fileContent = text
                fileSizeKb = (text.toByteArray().size / 1024).toLong()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer archivo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AnalysisUiState.Success) {
            val report = (uiState as AnalysisUiState.Success).report
            onAnalysisSuccess(report.id)
            viewModel.resetState()
        }
    }

    if (uiState is AnalysisUiState.Analyzing) {
        AnalyzingScreen(currentStepText = "Escaneando archivo .ips y sensor array...")
        return
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analizar Archivo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("import_file_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val content = fileContent
                            if (!content.isNullOrBlank()) {
                                viewModel.analyzeRawLog(content, selectedFileName)
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*", "text/*", "application/json"))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_file_analysis_button")
                    ) {
                        Icon(
                            imageVector = if (fileContent != null) Icons.Default.Troubleshoot else Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (fileContent != null) "Analizar ahora >" else "Seleccionar Archivo de Registro",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // Glowing Upload Dropzone Box
            item {
                Surface(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("*/*", "text/*", "application/json"))
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.5.dp, if (selectedFileName != null) ElectricCyanLight else ElectricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_upload_dropzone")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.15f))
                                .border(BorderStroke(1.5.dp, ElectricCyanLight), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedFileName != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (selectedFileName != null) ConfidenceHigh else ElectricCyanLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (selectedFileName != null) selectedFileName!! else "Toca aquí para seleccionar archivo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (selectedFileName != null) "${fileSizeKb ?: 0} KB • Listo para procesar" else "Formatos compatibles: .ips, .txt, .log, .json",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Format tags
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormatPill(".ips")
                            FormatPill(".txt")
                            FormatPill(".log")
                            FormatPill(".json")
                        }
                    }
                }
            }

            // Import Channels Card
            item {
                Text(
                    text = "FUENTES COMPATIBLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceChannelRow(
                        title = "Logs de Análisis de iOS (Configuración > Privacidad)",
                        subtitle = "Archivos Panic-full-*.ips compartidos por AirDrop o guardados en archivos.",
                        icon = Icons.Default.PhonelinkSetup
                    )
                    SourceChannelRow(
                        title = "Exportación 3uTools / iMazing / iTunes",
                        subtitle = "Logs de diagnóstico extraídos en PC o Mac vía cable USB.",
                        icon = Icons.Default.Computer
                    )
                    SourceChannelRow(
                        title = "Registros de Laboratorio en Texto Plano",
                        subtitle = "Archivos .txt con el volcado completo del sensor SMC.",
                        icon = Icons.Default.DataObject
                    )
                }
            }

            // Security Badge
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ElectricCyanLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "El archivo se analiza íntegramente en la memoria de la app. Los datos del usuario no salen del dispositivo.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TechDarkSurface)
            .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyanLight
        )
    }
}

@Composable
private fun SourceChannelRow(title: String, subtitle: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)
            }
        }
    }
}
