package com.example.wl.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ============================================================================
// Material 3 Expressive Color Palette
// ============================================================================
// Rich, saturated colors with multiple tonal roles for expressive UI.

// --- Primary: Deep Violet/Indigo ---
val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFEADDFF)
val OnPrimaryContainer = Color(0xFF21005D)
val PrimaryFixed = Color(0xFFEADDFF)
val OnPrimaryFixed = Color(0xFF21005D)
val PrimaryFixedDim = Color(0xFFD0BCFF)
val OnPrimaryFixedVariant = Color(0xFF4F378B)

// --- Secondary: Teal/Mint ---
val Secondary = Color(0xFF006D59)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFF80F6D5)
val OnSecondaryContainer = Color(0xFF002119)
val SecondaryFixed = Color(0xFF80F6D5)
val OnSecondaryFixed = Color(0xFF002119)
val SecondaryFixedDim = Color(0xFF61D9BA)
val OnSecondaryFixedVariant = Color(0xFF005243)

// --- Tertiary: Warm Amber/Coral ---
val Tertiary = Color(0xFFB2473A)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDAD4)
val OnTertiaryContainer = Color(0xFF410002)
val TertiaryFixed = Color(0xFFFFDAD4)
val OnTertiaryFixed = Color(0xFF410002)
val TertiaryFixedDim = Color(0xFFFFB4A8)
val OnTertiaryFixedVariant = Color(0xFF8C2D22)

// --- Neutral ---
val Neutral = Color(0xFF49454F)
val NeutralVariant = Color(0xFF49454F)
val OnNeutral = Color(0xFFFFFFFF)
val SurfaceDim = Color(0xFFDED8E1)
val Surface = Color(0xFFFEF7FF)
val SurfaceBright = Color(0xFFFEF7FF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F2FA)
val SurfaceContainer = Color(0xFFF2ECF5)
val SurfaceContainerHigh = Color(0xFFECE6F0)
val SurfaceContainerHighest = Color(0xFFE6E0E9)
val OnSurface = Color(0xFF1D1B20)
val OnSurfaceVariant = Color(0xFF49454F)
val Outline = Color(0xFF7A757F)
val OutlineVariant = Color(0xFFCAC4D0)

// --- Error ---
val Error = Color(0xFFB3261E)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

// ============================================================================
// Expressive / Accent Colors (for gradients, glows, special effects)
// ============================================================================
val GlowPrimary = Color(0x406750A4) // 25% alpha for glow overlays
val GlowSecondary = Color(0x40006D59)
val GlowTertiary = Color(0x40B2473A)

val GradientStart = Color(0xFF4158D0)  // Blue
val GradientMid = Color(0xFFC850C0)    // Pink-Purple
val GradientEnd = Color(0xFFFFCC70)    // Warm Gold

// Status colors for VPN
val StatusConnected = Color(0xFF4CAF50)
val StatusConnectedDim = Color(0x404CAF50)
val StatusConnecting = Color(0xFFFFC107)
val StatusConnectingDim = Color(0x40FFC107)
val StatusDisconnected = Color(0xFF9E9E9E)
val StatusDisconnectedDim = Color(0x409E9E9E)

// Latency colors
val LatencyFast = Color(0xFF4CAF50)       // < 150ms
val LatencyMedium = Color(0xFFFFC107)     // 150-400ms
val LatencySlow = Color(0xFFFF5722)       // 400-1000ms
val LatencyTimeout = Color(0xFFE53935)    // > 1000ms or error

// ============================================================================
// Utility for blending / interpolation (for animated gradients)
// ============================================================================
fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    return lerp(start, end, fraction)
}