package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val DeepObsidian = Color(0xFF030712)
private val DarkNavyCenter = Color(0xFF061022)
private val NeonBlue = Color(0xFF007AFF)
private val NeonCyan = Color(0xFF00C6FF)
private val BrightElectricBlue = Color(0xFF0088FF)
private val CircuitDimBlue = Color(0xFF0A305A)
private val CircuitBrightCyan = Color(0xFF00E5FF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMutedGrey = Color(0xFF6E7E94)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Splash duration timer (2.8 seconds)
    LaunchedEffect(Unit) {
        delay(2800)
        onSplashFinished()
    }

    // Infinite rotation animation for the glowing minimal loader
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loader_rotation"
    )

    // Subtle pulsing glow for the emblem and separator
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF02050D),
                        Color(0xFF050E1F),
                        Color(0xFF02060E)
                    )
                )
            )
            .testTag("splash_screen_container")
    ) {
        // 1. PCB Circuit Traces Background (Canvas with glowing lines and nodes)
        CircuitBackground(
            modifier = Modifier.fillMaxSize(),
            glowAlpha = pulseAlpha
        )

        // 2. Central Content Column (Centered & perfectly distributed)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer to push branding to the upper-middle area
            Spacer(modifier = Modifier.weight(1.0f))

            // Upper-Middle Branding Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hexagonal Microscope Emblema
                MicroscopeHexagonEmblem(
                    modifier = Modifier.size(132.dp),
                    glowAlpha = pulseAlpha
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Brand Title: "Panic" (White) + "Lab" (Electric Blue)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Panic",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Lab",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightElectricBlue,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle: "by FixMyCellLab" with "CellLab" highlighted in blue
                val subtitleString = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = TextWhite.copy(alpha = 0.9f),
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp
                        )
                    ) {
                        append("by FixMy")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = BrightElectricBlue,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    ) {
                        append("CellLab")
                    }
                }
                Text(text = subtitleString)

                Spacer(modifier = Modifier.height(18.dp))

                // Glowing Horizontal Divider Beam
                GlowingHorizontalBeam(
                    modifier = Modifier
                        .width(160.dp)
                        .height(3.dp),
                    glowAlpha = pulseAlpha
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Claim / Tagline: "Analiza. Diagnostica. Repara."
                val claimString = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = TextWhite.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    ) {
                        append("Analiza. Diagnostica. ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = BrightElectricBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    ) {
                        append("Repara.")
                    }
                }
                Text(text = claimString)
            }

            // Spacer between branding and lower loader
            Spacer(modifier = Modifier.weight(1.3f))

            // Lower Section: Glowing Minimalist Spinner and Version Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Minimalist Glowing Circular Loader
                GlowingCircularLoader(
                    rotation = rotation,
                    modifier = Modifier.size(42.dp)
                )

                // Version text: "v1.0"
                Text(
                    text = "v1.0",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextMutedGrey,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Renders the futuristic glowing printed circuit board (PCB) traces in top-right and bottom-left.
 */
@Composable
private fun CircuitBackground(
    modifier: Modifier = Modifier,
    glowAlpha: Float = 1.0f
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Top-Right Radial Glow Spot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0056B3).copy(alpha = 0.22f * glowAlpha),
                    Color(0xFF031633).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(width * 0.88f, height * 0.15f),
                radius = width * 0.55f
            )
        )

        // Bottom-Left Radial Glow Spot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0056B3).copy(alpha = 0.20f * glowAlpha),
                    Color(0xFF031633).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(width * 0.12f, height * 0.85f),
                radius = width * 0.55f
            )
        )

        // Draw Top-Right Circuit Lines
        drawTopRightCircuits(width, height, glowAlpha)

        // Draw Bottom-Left Circuit Lines
        drawBottomLeftCircuits(width, height, glowAlpha)
    }
}

private fun DrawScope.drawTopRightCircuits(w: Float, h: Float, alpha: Float) {
    val traceColor = CircuitDimBlue.copy(alpha = 0.65f)
    val activeTraceColor = NeonBlue.copy(alpha = 0.75f * alpha)
    val strokeWidth = 1.4.dp.toPx()

    // Trace 1 (Far right vertical with angled bends)
    val path1 = Path().apply {
        moveTo(w * 0.98f, 0f)
        lineTo(w * 0.98f, h * 0.12f)
        lineTo(w * 0.88f, h * 0.19f)
        lineTo(w * 0.88f, h * 0.32f)
        lineTo(w * 0.82f, h * 0.36f)
        lineTo(w * 0.82f, h * 0.42f)
    }
    drawPath(path1, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.82f, h * 0.42f), isGlow = false)

    // Trace 2 (Middle descending right trace with cyan glow nodes)
    val path2 = Path().apply {
        moveTo(w * 0.86f, 0f)
        lineTo(w * 0.86f, h * 0.08f)
        lineTo(w * 0.78f, h * 0.14f)
        lineTo(w * 0.78f, h * 0.22f)
        lineTo(w * 0.72f, h * 0.26f)
        lineTo(w * 0.72f, h * 0.34f)
    }
    drawPath(path2, color = activeTraceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.78f, h * 0.14f), isGlow = true)
    drawNode(Offset(w * 0.72f, h * 0.34f), isGlow = true)

    // Trace 3 (Inner branch)
    val path3 = Path().apply {
        moveTo(w * 0.74f, 0f)
        lineTo(w * 0.74f, h * 0.05f)
        lineTo(w * 0.67f, h * 0.10f)
        lineTo(w * 0.67f, h * 0.17f)
    }
    drawPath(path3, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.67f, h * 0.17f), isGlow = false)

    // Trace 4 (Long angled branch towards center-right)
    val path4 = Path().apply {
        moveTo(w, h * 0.10f)
        lineTo(w * 0.92f, h * 0.10f)
        lineTo(w * 0.84f, h * 0.16f)
        lineTo(w * 0.84f, h * 0.25f)
        lineTo(w * 0.89f, h * 0.29f)
        lineTo(w * 0.89f, h * 0.38f)
    }
    drawPath(path4, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.89f, h * 0.38f), isGlow = true)

    // Trace 5 (Deep right side connector)
    val path5 = Path().apply {
        moveTo(w, h * 0.22f)
        lineTo(w * 0.94f, h * 0.22f)
        lineTo(w * 0.94f, h * 0.35f)
        lineTo(w * 0.90f, h * 0.38f)
    }
    drawPath(path5, color = activeTraceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.90f, h * 0.38f), isGlow = false)

    // Decorative tiny glowing cyan chip pins / dots
    drawGlowDot(Offset(w * 0.84f, h * 0.28f))
    drawGlowDot(Offset(w * 0.76f, h * 0.20f))
    drawGlowDot(Offset(w * 0.92f, h * 0.16f))
}

private fun DrawScope.drawBottomLeftCircuits(w: Float, h: Float, alpha: Float) {
    val traceColor = CircuitDimBlue.copy(alpha = 0.65f)
    val activeTraceColor = NeonBlue.copy(alpha = 0.75f * alpha)
    val strokeWidth = 1.4.dp.toPx()

    // Trace 1 (Bottom left edge ascending trace)
    val path1 = Path().apply {
        moveTo(w * 0.02f, h)
        lineTo(w * 0.02f, h * 0.88f)
        lineTo(w * 0.12f, h * 0.81f)
        lineTo(w * 0.12f, h * 0.68f)
        lineTo(w * 0.18f, h * 0.64f)
        lineTo(w * 0.18f, h * 0.58f)
    }
    drawPath(path1, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.18f, h * 0.58f), isGlow = false)

    // Trace 2 (Active prominent trace with cyan glow nodes)
    val path2 = Path().apply {
        moveTo(w * 0.14f, h)
        lineTo(w * 0.14f, h * 0.92f)
        lineTo(w * 0.22f, h * 0.86f)
        lineTo(w * 0.22f, h * 0.78f)
        lineTo(w * 0.28f, h * 0.74f)
        lineTo(w * 0.28f, h * 0.66f)
    }
    drawPath(path2, color = activeTraceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.22f, h * 0.86f), isGlow = true)
    drawNode(Offset(w * 0.28f, h * 0.66f), isGlow = true)

    // Trace 3 (Inner branch)
    val path3 = Path().apply {
        moveTo(w * 0.26f, h)
        lineTo(w * 0.26f, h * 0.95f)
        lineTo(w * 0.33f, h * 0.90f)
        lineTo(w * 0.33f, h * 0.83f)
    }
    drawPath(path3, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.33f, h * 0.83f), isGlow = false)

    // Trace 4 (Left side horizontal branch entering upward)
    val path4 = Path().apply {
        moveTo(0f, h * 0.90f)
        lineTo(w * 0.08f, h * 0.90f)
        lineTo(w * 0.16f, h * 0.84f)
        lineTo(w * 0.16f, h * 0.75f)
        lineTo(w * 0.11f, h * 0.71f)
        lineTo(w * 0.11f, h * 0.62f)
    }
    drawPath(path4, color = traceColor, style = Stroke(width = strokeWidth))
    drawNode(Offset(w * 0.11f, h * 0.62f), isGlow = true)

    // Decorative tiny glowing cyan nodes
    drawGlowDot(Offset(w * 0.16f, h * 0.72f))
    drawGlowDot(Offset(w * 0.24f, h * 0.80f))
    drawGlowDot(Offset(w * 0.08f, h * 0.84f))
}

private fun DrawScope.drawNode(center: Offset, isGlow: Boolean) {
    val radius = 3.2.dp.toPx()
    // Outer via ring
    drawCircle(
        color = if (isGlow) CircuitBrightCyan else CircuitDimBlue,
        radius = radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    // Inner hole
    drawCircle(
        color = DeepObsidian,
        radius = radius * 0.5f,
        center = center
    )
}

private fun DrawScope.drawGlowDot(center: Offset) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                CircuitBrightCyan,
                NeonBlue.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = 6.dp.toPx()
        ),
        radius = 6.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 1.2.dp.toPx(),
        center = center
    )
}

/**
 * Renders the glowing hexagon container and scientific laboratory microscope vector.
 */
@Composable
private fun MicroscopeHexagonEmblem(
    modifier: Modifier = Modifier,
    glowAlpha: Float = 1.0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.46f

        // 1. Soft Outer Cyan Glow Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonBlue.copy(alpha = 0.35f * glowAlpha),
                    NeonCyan.copy(alpha = 0.12f * glowAlpha),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius * 1.5f
            )
        )

        // Calculate Hexagon Vertices (pointy-top orientation)
        val hexagonPath = Path()
        for (i in 0..5) {
            val angleDeg = 60f * i - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = (cx + radius * cos(angleRad)).toFloat()
            val y = (cy + radius * sin(angleRad)).toFloat()
            if (i == 0) hexagonPath.moveTo(x, y) else hexagonPath.lineTo(x, y)
        }
        hexagonPath.close()

        // 2. Hexagon Inner Gradient Fill
        drawPath(
            path = hexagonPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B2144),
                    Color(0xFF040E1E),
                    Color(0xFF020712)
                )
            )
        )

        // 3. Hexagon Outer Glowing Neon Border
        drawPath(
            path = hexagonPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan,
                    NeonBlue,
                    Color(0xFF004499)
                )
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Subtle Inner Hexagon Accent Line
        val innerHexagonPath = Path()
        val innerRadius = radius * 0.92f
        for (i in 0..5) {
            val angleDeg = 60f * i - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = (cx + innerRadius * cos(angleRad)).toFloat()
            val y = (cy + innerRadius * sin(angleRad)).toFloat()
            if (i == 0) innerHexagonPath.moveTo(x, y) else innerHexagonPath.lineTo(x, y)
        }
        innerHexagonPath.close()

        drawPath(
            path = innerHexagonPath,
            color = NeonCyan.copy(alpha = 0.25f),
            style = Stroke(width = 1.dp.toPx())
        )

        // 5. Draw Microscope Vector inside the hexagon
        drawMicroscopeIcon(cx, cy, radius * 0.72f)
    }
}

/**
 * Draws the high-contrast scientific microscope icon with cyan accents.
 */
private fun DrawScope.drawMicroscopeIcon(cx: Float, cy: Float, size: Float) {
    val scale = size / 50f // normalize to a 50x50 coordinate system

    // Offset center
    val ox = cx - 25f * scale
    val oy = cy - 25f * scale

    val whiteBrush = SolidColor(Color.White)
    val cyanBrush = SolidColor(BrightElectricBlue)

    // Base of microscope (Solid curved stand)
    val basePath = Path().apply {
        moveTo(ox + 12f * scale, oy + 42f * scale)
        lineTo(ox + 38f * scale, oy + 42f * scale)
        lineTo(ox + 38f * scale, oy + 39f * scale)
        quadraticBezierTo(ox + 25f * scale, oy + 37f * scale, ox + 12f * scale, oy + 39f * scale)
        close()
    }
    drawPath(basePath, brush = whiteBrush)

    // Stage Platform (Horizontal rectangle with cyan slide detail)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(ox + 14f * scale, oy + 31f * scale),
        size = Size(18f * scale, 3.2f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * scale)
    )
    drawRoundRect(
        color = NeonCyan,
        topLeft = Offset(ox + 16f * scale, oy + 32f * scale),
        size = Size(14f * scale, 1.2f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f * scale)
    )

    // Curved Arm & Pillar (C-shaped back arm)
    val armPath = Path().apply {
        moveTo(ox + 36f * scale, oy + 39f * scale)
        lineTo(ox + 31f * scale, oy + 39f * scale)
        // Inner curve
        cubicTo(
            ox + 32f * scale, oy + 28f * scale,
            ox + 28f * scale, oy + 20f * scale,
            ox + 27f * scale, oy + 17f * scale
        )
        lineTo(ox + 31f * scale, oy + 14f * scale)
        // Outer curve
        cubicTo(
            ox + 37f * scale, oy + 18f * scale,
            ox + 42f * scale, oy + 26f * scale,
            ox + 36f * scale, oy + 39f * scale
        )
        close()
    }
    drawPath(armPath, brush = whiteBrush)

    // Eyepiece / Optical Tube (Angled cylinder)
    val tubePath = Path().apply {
        moveTo(ox + 28f * scale, oy + 9f * scale)
        lineTo(ox + 32f * scale, oy + 13f * scale)
        lineTo(ox + 22f * scale, oy + 23f * scale)
        lineTo(ox + 18f * scale, oy + 19f * scale)
        close()
    }
    drawPath(tubePath, brush = whiteBrush)

    // Eyepiece Top Ring
    val topRingPath = Path().apply {
        moveTo(ox + 27f * scale, oy + 8f * scale)
        lineTo(ox + 33f * scale, oy + 14f * scale)
        lineTo(ox + 34.5f * scale, oy + 12.5f * scale)
        lineTo(ox + 28.5f * scale, oy + 6.5f * scale)
        close()
    }
    drawPath(topRingPath, brush = whiteBrush)

    // Objective Lens Revolving Turret / Nosepiece (with cyan objective pins)
    val turretPath = Path().apply {
        moveTo(ox + 17.5f * scale, oy + 19.5f * scale)
        lineTo(ox + 22.5f * scale, oy + 24.5f * scale)
        lineTo(ox + 19f * scale, oy + 28f * scale)
        lineTo(ox + 14f * scale, oy + 23f * scale)
        close()
    }
    drawPath(turretPath, brush = whiteBrush)

    // Cyan Objective Lenses (3 angled prongs pointing toward stage)
    val lensWidth = 1.4f * scale
    val lensLength = 3.5f * scale
    drawRoundRect(
        color = NeonCyan,
        topLeft = Offset(ox + 15f * scale, oy + 24.5f * scale),
        size = Size(lensWidth, lensLength),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.5f * scale)
    )
    drawRoundRect(
        color = NeonCyan,
        topLeft = Offset(ox + 17.5f * scale, oy + 26f * scale),
        size = Size(lensWidth, lensLength),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.5f * scale)
    )
    drawRoundRect(
        color = NeonCyan,
        topLeft = Offset(ox + 20f * scale, oy + 27.5f * scale),
        size = Size(lensWidth, lensLength),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.5f * scale)
    )
}

/**
 * Renders a glowing horizontal light beam divider.
 */
@Composable
private fun GlowingHorizontalBeam(
    modifier: Modifier = Modifier,
    glowAlpha: Float = 1.0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Diffuse Cyan Glow Halo
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.55f * glowAlpha),
                    NeonBlue.copy(alpha = 0.2f * glowAlpha),
                    Color.Transparent
                ),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.45f
            ),
            topLeft = Offset(0f, -h * 2f),
            size = Size(w, h * 5f)
        )

        // Crisp Center Laser Beam
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonBlue.copy(alpha = 0.6f),
                    Color.White,
                    NeonCyan,
                    NeonBlue.copy(alpha = 0.6f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist glowing neon circular loading indicator ring.
 */
@Composable
private fun GlowingCircularLoader(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer(rotationZ = rotation)
    ) {
        val strokeWidth = 2.8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Soft outer cyan glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.5f
            )
        )

        // Arc with sweeping gradient trail
        drawArc(
            brush = Brush.sweepGradient(
                0.0f to Color.Transparent,
                0.4f to Color.Transparent,
                0.7f to NeonBlue,
                1.0f to NeonCyan
            ),
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
