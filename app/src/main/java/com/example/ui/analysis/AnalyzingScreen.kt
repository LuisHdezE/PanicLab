package com.example.ui.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HexagonMicroscopeEmblem
import com.example.ui.theme.*

@Composable
fun AnalyzingScreen(
    currentStepText: String = "Escaneando sensor array y códigos SMC...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val progressValue by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Center Animation Circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("analyzing_progress_circle"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Track
                    drawArc(
                        color = TechDarkBorder,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Rotating glowing arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                ElectricBlue.copy(alpha = 0.1f),
                                ElectricBlue,
                                ElectricCyanLight,
                                ElectricBlue
                            )
                        ),
                        startAngle = rotation,
                        sweepAngle = progressValue * 300f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HexagonMicroscopeEmblem(size = 46.dp)
                    Text(
                        text = "${(progressValue * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Analizando log",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElectricCyanLight
                    )
                }
            }

            // Steps Progress List
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = TechDarkCard,
                border = BorderStroke(1.dp, TechDarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalysisStepItem(
                        text = "Extrayendo encabezado y modelo de placa",
                        isCompleted = true,
                        isActive = false
                    )
                    AnalysisStepItem(
                        text = currentStepText,
                        isCompleted = false,
                        isActive = true
                    )
                    AnalysisStepItem(
                        text = "Emparejando con base determinista v1.0",
                        isCompleted = false,
                        isActive = false
                    )
                    AnalysisStepItem(
                        text = "Generando flujo de reparación técnico",
                        isCompleted = false,
                        isActive = false
                    )
                }
            }
        }

        // Bottom Privacy Disclaimer
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
                    text = "Procesamiento 100% offline • Sin servidores ni telemetría",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun AnalysisStepItem(
    text: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ConfidenceHigh,
                modifier = Modifier.size(16.dp)
            )
        } else if (isActive) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(BorderStroke(1.5.dp, ElectricCyanLight), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.dp, Color(0xFF334155)), CircleShape)
            )
        }

        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isCompleted -> Color(0xFF94A3B8)
                isActive -> Color.White
                else -> Color(0xFF64748B)
            }
        )
    }
}
