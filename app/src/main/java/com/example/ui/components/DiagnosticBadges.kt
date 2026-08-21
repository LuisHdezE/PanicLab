package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.VerificationStatus
import com.example.ui.theme.*

@Composable
fun ConfidenceBadge(
    level: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (bg, border, fg, label) = when (level) {
        ConfidenceLevel.HIGH -> Quadruple(ConfidenceHighBg, ConfidenceHighBorder, ConfidenceHigh, "ALTA CONFIANZA")
        ConfidenceLevel.MEDIUM -> Quadruple(ConfidenceMediumBg, ConfidenceMediumBorder, ConfidenceMedium, "CONF. MEDIA")
        ConfidenceLevel.LOW -> Quadruple(ConfidenceLowBg, ConfidenceLowBorder, ConfidenceLow, "BAJA CONF.")
        ConfidenceLevel.UNKNOWN -> Quadruple(ConfidenceUnknownBg, ConfidenceUnknownBorder, ConfidenceUnknown, "SIN CONFIRMAR")
    }

    Box(
        modifier = modifier
            .testTag("confidence_badge_${level.name}")
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun VerificationBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val (bg, border, fg, label) = when (status) {
        VerificationStatus.VERIFIED -> Quadruple(Color(0xFF064E3B).copy(alpha = 0.45f), StatusVerified.copy(alpha = 0.6f), StatusVerified, "VERIFICADO")
        VerificationStatus.WELL_DOCUMENTED -> Quadruple(Color(0xFF1E3A8A).copy(alpha = 0.45f), StatusDocumented.copy(alpha = 0.6f), StatusDocumented, "DOCUMENTADO")
        VerificationStatus.COMMUNITY_SUPPORTED -> Quadruple(Color(0xFF4C1D95).copy(alpha = 0.45f), StatusCommunity.copy(alpha = 0.6f), StatusCommunity, "COMUNIDAD")
        VerificationStatus.CONFLICTING_SOURCE -> Quadruple(Color(0xFF7F1D1D).copy(alpha = 0.45f), StatusCaution.copy(alpha = 0.6f), StatusCaution, "EN CONFLICTO")
        VerificationStatus.EXPERIMENTAL -> Quadruple(ConfidenceMediumBg, ConfidenceMediumBorder, ConfidenceMedium, "EXPERIMENTAL")
        VerificationStatus.UNKNOWN -> Quadruple(ConfidenceUnknownBg, ConfidenceUnknownBorder, ConfidenceUnknown, "DESCONOCIDO")
    }

    Box(
        modifier = modifier
            .testTag("verification_badge_${status.name}")
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
