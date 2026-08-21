package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// PanicLab Tech-Lab Sleek Dark Palette (Primary Interface)
val TechDarkBg = Color(0xFF080D18)
val TechDarkSurface = Color(0xFF0F172A)
val TechDarkCard = Color(0xFF131E35)
val TechDarkCardElevated = Color(0xFF182642)
val TechDarkBorder = Color(0xFF1E2E4E)
val TechDarkBorderGlow = Color(0xFF2E4B82)

// Vibrant Electric Blue & Cyan Accents
val ElectricBlue = Color(0xFF3B82F6)
val ElectricBlueDark = Color(0xFF1D4ED8)
val ElectricBlueLight = Color(0xFF60A5FA)
val ElectricBlueContainer = Color(0xFF1E3A8A)
val ElectricCyan = Color(0xFF06B6D4)
val ElectricCyanLight = Color(0xFF38BDF8)

// Sleek Light Palette (Fallback)
val PrimaryLight = Color(0xFF1B6EF3)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE8F0FF)
val OnPrimaryContainerLight = Color(0xFF1B6EF3)

val SecondaryLight = Color(0xFF2563EB)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD1E1FF)
val OnSecondaryContainerLight = Color(0xFF1E40AF)

val BackgroundLight = Color(0xFFF7F9FF)
val OnBackgroundLight = Color(0xFF1B1B1F)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1B1B1F)
val SurfaceVariantLight = Color(0xFFF0F4FF)
val OnSurfaceVariantLight = Color(0xFF64748B)
val OutlineLight = Color(0xFFDEE2F1)
val OutlineVariantLight = Color(0xFFEEF2FC)

// Sleek Interface Dark Palette (M3 Theme mapping)
val PrimaryDark = ElectricBlue
val OnPrimaryDark = Color(0xFFFFFFFF)
val PrimaryContainerDark = Color(0xFF142B59)
val OnPrimaryContainerDark = Color(0xFFD8E6FF)

val SecondaryDark = ElectricCyanLight
val OnSecondaryDark = Color(0xFF042F2E)
val SecondaryContainerDark = Color(0xFF134E4A)
val OnSecondaryContainerDark = Color(0xFFCCFBF1)

val BackgroundDark = TechDarkBg
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = TechDarkSurface
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = TechDarkCard
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = TechDarkBorder
val OutlineVariantDark = Color(0xFF152238)

// Diagnostic Confidence Colors
val ConfidenceHigh = Color(0xFF10B981)
val ConfidenceHighBg = Color(0xFF064E3B).copy(alpha = 0.45f)
val ConfidenceHighBorder = Color(0xFF10B981).copy(alpha = 0.6f)

val ConfidenceMedium = Color(0xFFF59E0B)
val ConfidenceMediumBg = Color(0xFF78350F).copy(alpha = 0.45f)
val ConfidenceMediumBorder = Color(0xFFF59E0B).copy(alpha = 0.6f)

val ConfidenceLow = Color(0xFFEA580C)
val ConfidenceLowBg = Color(0xFF7C2D12).copy(alpha = 0.45f)
val ConfidenceLowBorder = Color(0xFFEA580C).copy(alpha = 0.6f)

val ConfidenceUnknown = Color(0xFF94A3B8)
val ConfidenceUnknownBg = Color(0xFF334155).copy(alpha = 0.45f)
val ConfidenceUnknownBorder = Color(0xFF64748B).copy(alpha = 0.6f)

val StatusVerified = Color(0xFF10B981)
val StatusDocumented = Color(0xFF3B82F6)
val StatusCommunity = Color(0xFFA855F7)
val StatusCaution = Color(0xFFEF4444)
val StatusConflict = Color(0xFFF97316)
