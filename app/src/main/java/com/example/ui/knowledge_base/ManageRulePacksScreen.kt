package com.example.ui.knowledge_base

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.RulePackEntity
import com.example.domain.model.RulePackOrigin
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.knowledge_base.components.RulePackPreviewDialog
import com.example.ui.knowledge_base.components.RulePackValidationErrorsDialog
import com.example.ui.theme.*
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRulePacksScreen(
    viewModel: KnowledgeBaseViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentVersion by viewModel.currentVersion.collectAsState()
    val activeRulePack by viewModel.activeRulePack.collectAsState()
    val allRulePacks by viewModel.allRulePacks.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val previewState by viewModel.previewState.collectAsState()

    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var versionToRestore by remember { mutableStateOf<String?>(null) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "paquete.json"
                viewModel.validateAndPreviewRulePack(
                    jsonContent = content,
                    filename = fileName,
                    origin = RulePackOrigin.USER_IMPORTED
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer archivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Active Preview / Validation Error Dialogs
    when (val state = previewState) {
        is RulePackPreviewUiState.ValidatedDiff -> {
            RulePackPreviewDialog(
                validationResult = state.validationResult,
                parsedPack = state.parsedPack,
                diffSummary = state.diffSummary,
                filename = state.filename,
                isInstalling = false,
                onConfirmInstall = {
                    viewModel.installValidatedPack(
                        parsedPack = state.parsedPack,
                        filename = state.filename,
                        onSuccess = { installedVer ->
                            Toast.makeText(context, "¡Paquete v$installedVer instalado con éxito!", Toast.LENGTH_LONG).show()
                        },
                        onError = { errMsg ->
                            Toast.makeText(context, "Error al instalar: $errMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onDismiss = { viewModel.clearPreview() }
            )
        }
        is RulePackPreviewUiState.Installing -> {
            // Visual spinner handled in dialog
        }
        is RulePackPreviewUiState.ValidationError -> {
            RulePackValidationErrorsDialog(
                validationResult = state.validationResult,
                filename = state.filename,
                onDismiss = { viewModel.clearPreview() }
            )
        }
        RulePackPreviewUiState.Validating -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Validando Paquete", color = Color.White) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricCyanLight)
                        Text("Verificando integridad referencial, SemVer y esquema...", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    }
                },
                containerColor = TechDarkCard
            )
        }
        RulePackPreviewUiState.Idle -> { /* do nothing */ }
    }

    // Direct JSON Paste Dialog
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Text(
                    text = "Pegar JSON de Reglas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pega aquí el contenido de un Rule Pack JSON para validarlo e importarlo.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = pastedJsonText,
                        onValueChange = { pastedJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\n  \"schemaVersion\": 1,\n  ...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = TechDarkBorder,
                            focusedContainerColor = TechDarkSurface,
                            unfocusedContainerColor = TechDarkSurface
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                pastedJsonText = clipText
                            }
                        }) {
                            Text("Pegar del Portapapeles", color = ElectricCyanLight, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = pastedJsonText.trim()
                        if (text.isNotBlank()) {
                            showPasteDialog = false
                            viewModel.validateAndPreviewRulePack(
                                jsonContent = text,
                                filename = "clipboard.json",
                                origin = RulePackOrigin.USER_IMPORTED
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Validar y Previsualizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = TechDarkBg,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Restore Default confirmation dialog
    if (showRestoreDefaultConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultConfirm = false },
            title = { Text("Restaurar Reglas de Fábrica", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta acción reactivará el paquete determinista oficial de fábrica v1.0.0 incluido en la APK. ¿Deseas continuar?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDefaultConfirm = false
                        viewModel.restoreBundledDefault(
                            onSuccess = { Toast.makeText(context, "Reglas de fábrica v1.0.0 restauradas", Toast.LENGTH_LONG).show() },
                            onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restaurar v1.0.0", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaultConfirm = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = TechDarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Restore specific historical version dialog
    versionToRestore?.let { ver ->
        AlertDialog(
            onDismissRequest = { versionToRestore = null },
            title = { Text("Restaurar Versión $ver", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se reactivará la base de conocimiento con todas las reglas y modelos guardados en la versión $ver. ¿Deseas proceder?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        versionToRestore = null
                        viewModel.restoreVersion(
                            version = ver,
                            onSuccess = { Toast.makeText(context, "Versión $ver activada", Toast.LENGTH_SHORT).show() },
                            onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Activar Versión", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { versionToRestore = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = TechDarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = TechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gestión de Rule Packs",
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
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HexagonMicroscopeEmblem(size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = activeRulePack?.title ?: "PanicLab Diagnostic Rules",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricCyanLight.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "v$currentVersion",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyanLight,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "• ${activeRulePack?.origin ?: "BUNDLED"}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "ACTIVO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (!activeRulePack?.checksum.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TechDarkSurface,
                                border = BorderStroke(1.dp, TechDarkBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "SHA-256: ${activeRulePack?.checksum?.take(16)}...",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = TechDarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(label = "Reglas Activas", value = "${rules.size}")
                            StatBox(label = "Modelos", value = "${devices.size}")
                            StatBox(label = "Historial", value = "${allRulePacks.size} packs")
                        }
                    }
                }
            }

            // Import & Update Options Card
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
                                text = "Actualizar / Importar Reglas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Importa un nuevo Rule Pack JSON. Antes de instalar, PanicLab valida la integridad referencial y te muestra una vista previa interactiva con las diferencias de diagnóstico.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("import_rule_pack_file_button")
                            ) {
                                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Archivo JSON", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showPasteDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("paste_rule_pack_json_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pegar JSON", fontSize = 13.sp, color = ElectricCyanLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Restore defaults action
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRestoreDefaultConfirm = true },
                    shape = RoundedCornerShape(18.dp),
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TechDarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Restaurar Reglas de Fábrica (v1.0.0)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Reinstala el paquete oficial determinista incluido en la APK.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            }

            // Version History Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORIAL DE VERSIONES INSTALADAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${allRulePacks.size} paquetes",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (allRulePacks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkCard,
                        border = BorderStroke(1.dp, TechDarkBorder)
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No hay paquetes previos en el historial.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(allRulePacks, key = { it.version }) { pack ->
                    RulePackHistoryCard(
                        pack = pack,
                        isActive = pack.isActive,
                        onRestore = {
                            versionToRestore = pack.version
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RulePackHistoryCard(
    pack: RulePackEntity,
    isActive: Boolean,
    onRestore: () -> Unit
) {
    val dateStr = remember(pack.importedAt) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(pack.importedAt))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) TechDarkCard else TechDarkSurface,
        border = BorderStroke(1.dp, if (isActive) ElectricCyanLight.copy(alpha = 0.5f) else TechDarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${pack.version}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) ElectricCyanLight else Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TechDarkBorder
                    ) {
                        Text(
                            text = pack.origin,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTIVO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = pack.title,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )

                Text(
                    text = "${pack.rulesCount} reglas • ${pack.modelsCount} modelos • $dateStr",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (!isActive) {
                OutlinedButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Restaurar", fontSize = 11.sp, color = ElectricCyanLight, fontWeight = FontWeight.Bold)
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
