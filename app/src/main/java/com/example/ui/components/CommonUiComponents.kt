package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexagonMicroscopeEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    primaryColor: Color = ElectricBlue,
    cyanAccent: Color = ElectricCyanLight,
    surfaceBg: Color = TechDarkSurface
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f - 3f

            // Draw filled hexagon
            val hexPath = Path().apply {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 30.0)
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = hexPath,
                color = surfaceBg
            )

            // Draw glowing border
            drawPath(
                path = hexPath,
                color = primaryColor,
                style = Stroke(width = 2.5f, join = StrokeJoin.Round)
            )

            // Draw stylized microscope inside
            val scale = this.size.minDimension / 100f
            val ox = center.x
            val oy = center.y

            // Eyepiece/Arm
            drawLine(
                color = cyanAccent,
                start = Offset(ox - 10f * scale, oy - 22f * scale),
                end = Offset(ox + 6f * scale, oy - 6f * scale),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )

            // Objective turret
            drawLine(
                color = primaryColor,
                start = Offset(ox + 4f * scale, oy - 4f * scale),
                end = Offset(ox - 2f * scale, oy + 8f * scale),
                strokeWidth = 3.5f * scale,
                cap = StrokeCap.Round
            )

            // Base stage
            drawLine(
                color = primaryColor,
                start = Offset(ox - 16f * scale, oy + 18f * scale),
                end = Offset(ox + 16f * scale, oy + 18f * scale),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )

            // Specimen point
            drawCircle(
                color = cyanAccent,
                radius = 2.5f * scale,
                center = Offset(ox, oy + 12f * scale)
            )
        }
    }
}

@Composable
fun PanicLabTopHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HexagonMicroscopeEmblem(size = 42.dp)

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PanicLab",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyanLight,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = "by FixMyCellLab • Deterministic v1.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(12.dp),
                color = TechDarkCard,
                border = BorderStroke(1.dp, TechDarkBorder),
                modifier = Modifier.size(40.dp).testTag("header_search_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar reglas",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                onClick = onSettingsClick,
                shape = RoundedCornerShape(12.dp),
                color = TechDarkCard,
                border = BorderStroke(1.dp, TechDarkBorder),
                modifier = Modifier.size(40.dp).testTag("header_settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PanicLabBottomNavBar(
    selectedTab: Int,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToKnowledgeBase: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = TechDarkSurface,
        border = BorderStroke(1.dp, TechDarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                title = "Centro",
                icon = Icons.Default.Dashboard,
                isSelected = selectedTab == 0,
                onClick = onNavigateToHome,
                tag = "bottom_nav_home"
            )

            NavTabItem(
                title = "Historial",
                icon = Icons.Default.History,
                isSelected = selectedTab == 1,
                onClick = onNavigateToHistory,
                tag = "bottom_nav_history"
            )

            NavTabItem(
                title = "Base",
                icon = Icons.Default.MenuBook,
                isSelected = selectedTab == 2,
                onClick = onNavigateToKnowledgeBase,
                tag = "bottom_nav_rules"
            )

            NavTabItem(
                title = "Analizar",
                icon = Icons.Default.FileUpload,
                isSelected = selectedTab == 3,
                onClick = onNavigateToImport,
                tag = "bottom_nav_report"
            )
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag(tag)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ElectricCyanLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ElectricCyanLight else Color(0xFF64748B)
        )
    }
}

@Composable
fun PanicCodeBadge(
    code: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElectricBlueContainer.copy(alpha = 0.5f))
            .border(BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = code,
            color = ElectricCyanLight,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
