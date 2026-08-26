package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.model.ConfidenceLevel
import com.example.ocr.OcrLogExtractor
import com.example.ocr.OcrScanResult
import com.example.ocr.PanicLogImageAnalyzer
import com.example.ui.analysis.AnalysisUiState
import com.example.ui.analysis.AnalysisViewModel
import com.example.ui.analysis.AnalyzingScreen
import com.example.ui.components.ConfidenceBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraLogScannerScreen(
    viewModel: AnalysisViewModel,
    onNavigateBack: () -> Unit,
    onAnalysisSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Se requiere permiso de cámara para el escáner OCR", Toast.LENGTH_LONG).show()
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var currentScanResult by remember { mutableStateOf<OcrScanResult?>(null) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var editableLogText by remember { mutableStateOf("") }
    var isProcessingGalleryImage by remember { mutableStateOf(false) }

    // Gallery Image Picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingGalleryImage = true
            coroutineScope.launch {
                try {
                    val result = PanicLogImageAnalyzer.processUri(context, uri)
                    currentScanResult = result
                    editableLogText = result.cleanedText
                    showReviewSheet = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Error procesando imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingGalleryImage = false
                }
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
        AnalyzingScreen(currentStepText = "Analizando texto extraído por OCR con motor determinista...")
        return
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Escáner OCR de Panic",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Apunta a pantalla o papel impreso",
                            fontSize = 11.sp,
                            color = ElectricCyanLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("scanner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Flashlight toggle
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                val newTorchState = !isTorchEnabled
                                activeCamera?.cameraControl?.enableTorch(newTorchState)
                                isTorchEnabled = newTorchState
                            },
                            modifier = Modifier.testTag("scanner_torch_toggle")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Linterna",
                                tint = if (isTorchEnabled) Color(0xFFFBBF24) else Color.White
                            )
                        }
                    }
                    // Gallery pick button
                    IconButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        modifier = Modifier.testTag("scanner_gallery_pick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Cargar foto",
                            tint = ElectricCyanLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Permission Request Fallback View
                CameraPermissionRequestView(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSelectFromGallery = { galleryPickerLauncher.launch("image/*") }
                )
            } else {
                // Live Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(
                                            cameraExecutor,
                                            PanicLogImageAnalyzer(
                                                onScanResult = { result ->
                                                    currentScanResult = result
                                                    if (result.cleanedText.isNotBlank()) {
                                                        editableLogText = result.cleanedText
                                                    }
                                                }
                                            )
                                        )
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                activeCamera = camera
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Scanner Reticle Overlay & Laser Animation
                ScannerReticleOverlay()

                // Live Detection Status Banner (Top floating)
                currentScanResult?.let { result ->
                    LiveDetectionHud(
                        result = result,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }

                // Bottom Action Controls Bar
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, TechDarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Capture and Review Button
                        Button(
                            onClick = {
                                val result = currentScanResult
                                if (result != null && result.cleanedText.isNotBlank()) {
                                    editableLogText = result.cleanedText
                                    showReviewSheet = true
                                } else {
                                    Toast.makeText(context, "Apunta la cámara a las líneas de texto del Panic Log", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentScanResult?.hasValidPanicSignatures == true) ElectricCyanLight else ElectricBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("scanner_capture_review_btn")
                        ) {
                            Icon(
                                imageVector = if (currentScanResult?.hasValidPanicSignatures == true) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (currentScanResult?.hasValidPanicSignatures == true) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentScanResult?.hasValidPanicSignatures == true)
                                    "Captura lista • Analizar Log >"
                                else
                                    "Capturar y Revisar Texto",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentScanResult?.hasValidPanicSignatures == true) Color.Black else Color.White
                            )
                        }

                        // Instructions tip
                        Text(
                            text = "💡 Enfoca líneas como: \"SMC PANIC\", \"sensor array\", \"0x400000\" o \"Product: iPhone\"",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (isProcessingGalleryImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyanLight)
                }
            }
        }
    }

    // Bottom Sheet for Reviewing and Refining Scanned OCR Text
    if (showReviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewSheet = false },
            containerColor = TechDarkSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Revisar Registro Escaneado",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Verifica los datos extraídos antes del diagnóstico",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = { showReviewSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                // Detected badges summary
                currentScanResult?.let { res ->
                    if (res.detectedKeywords.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            res.detectedKeywords.forEach { kw ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricBlue.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ElectricCyanLight.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = kw,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyanLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Text Editor for OCR Scanned content
                OutlinedTextField(
                    value = editableLogText,
                    onValueChange = { editableLogText = it },
                    label = { Text("Texto del Panic Log Extraído") },
                    minLines = 8,
                    maxLines = 14,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyanLight,
                        unfocusedBorderColor = TechDarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = TechDarkBg,
                        unfocusedContainerColor = TechDarkBg
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("scanner_review_text_input")
                )

                // Actions: Re-scan or Run Analysis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReviewSheet = false },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TechDarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scanner_rescan_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-escanear", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (editableLogText.isNotBlank()) {
                                showReviewSheet = false
                                viewModel.analyzeRawLog(editableLogText, "scan_ocr.ips")
                            } else {
                                Toast.makeText(context, "El texto del log no puede estar vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("scanner_run_diagnostic_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnosticar >", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDetectionHud(
    result: OcrScanResult,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TechDarkSurface.copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            if (result.hasValidPanicSignatures) ElectricCyanLight else TechDarkBorderGlow
        ),
        modifier = modifier.fillMaxWidth().testTag("scanner_live_hud")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (result.hasValidPanicSignatures) ConfidenceHigh else ElectricCyanLight)
                    )
                    Text(
                        text = if (result.hasValidPanicSignatures) "PATRONES DETECTADOS" else "ESCANEANDO EN VIVO...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.hasValidPanicSignatures) ConfidenceHigh else ElectricCyanLight,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${result.lineCount} líneas",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Text(
                text = result.confidenceHint,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            if (result.detectedKeywords.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.detectedKeywords.take(4).forEach { kw ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechDarkBg)
                                .border(BorderStroke(1.dp, TechDarkBorder), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = kw,
                                fontSize = 10.sp,
                                color = ElectricCyanLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerReticleOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 90.dp)
    ) {
        val width = size.width
        val height = size.height
        val cornerLength = 40.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val cornerColor = ElectricCyanLight

        // Top-left corner
        drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
        drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

        // Top-right corner
        drawLine(cornerColor, Offset(width, 0f), Offset(width - cornerLength, 0f), strokeWidth)
        drawLine(cornerColor, Offset(width, 0f), Offset(width, cornerLength), strokeWidth)

        // Bottom-left corner
        drawLine(cornerColor, Offset(0f, height), Offset(cornerLength, height), strokeWidth)
        drawLine(cornerColor, Offset(0f, height), Offset(0f, height - cornerLength), strokeWidth)

        // Bottom-right corner
        drawLine(cornerColor, Offset(width, height), Offset(width - cornerLength, height), strokeWidth)
        drawLine(cornerColor, Offset(width, height), Offset(width, height - cornerLength), strokeWidth)

        // Animated horizontal laser scanning line
        val currentLaserY = height * laserPosition
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    ElectricCyanLight,
                    Color.White,
                    ElectricCyanLight,
                    Color.Transparent
                )
            ),
            start = Offset(10.dp.toPx(), currentLaserY),
            end = Offset(width - 10.dp.toPx(), currentLaserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

@Composable
private fun CameraPermissionRequestView(
    onRequestPermission: () -> Unit,
    onSelectFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TechDarkCard,
            border = BorderStroke(1.dp, TechDarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.15f))
                        .border(1.dp, ElectricCyanLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ElectricCyanLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Permiso de Cámara Requerido",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "PanicLab necesita acceso a la cámara para escanear registros de pánico directamente desde pantallas o documentos impresos.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scanner_request_permission_btn")
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Habilitar Cámara", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSelectFromGallery,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TechDarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scanner_select_gallery_fallback_btn")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar Imagen de Galería", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
