package com.example.ui.knowledge_base

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.theme.*
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRulePacksScreen(
    viewModel: KnowledgeBaseViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentVersion by viewModel.currentVersion.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val devices by viewModel.devices.collectAsState()

    var isImporting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                viewModel.importRulePack(content) { result ->
                    isImporting = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Paquete v${result.getOrNull()} importado con éxito a Room", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error al importar JSON: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                isImporting = false
                Toast.makeText(context, "Error al leer archivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paquetes de Reglas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("manage_packs_back_button")
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // Active Rule Pack Header Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorderGlow)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HexagonMicroscopeEmblem(size = 42.dp)
                            Column {
                                Text(
                                    text = "Paquete Activo Oficial",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Versión: v$currentVersion • Determinista",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricCyanLight
                                )
                            }
                        }

                        HorizontalDivider(color = TechDarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(label = "Reglas Activas", value = "${rules.size}")
                            StatBox(label = "Modelos Mapeados", value = "${devices.size}")
                            StatBox(label = "Schema", value = "v1.0.0")
                        }
                    }
                }
            }

            // Import JSON rule pack
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
                            Text(
                                text = "Importar Paquete Comunitario",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Importa un archivo JSON con nuevas definiciones de sensores o modelos de iPhone. La validación y persistencia se ejecutan localmente en Room.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            enabled = !isImporting,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_rule_pack_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importando a Room...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar Archivo JSON", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Restore defaults card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TechDarkCard,
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TechDarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Restaurar Reglas de Fábrica", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Reinstala el paquete v1.0.0 incluido en la APK.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElectricCyanLight)
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}
