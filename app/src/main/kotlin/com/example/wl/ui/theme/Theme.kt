package com.example.wl.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================================
// Material 3 Expressive Typography
// ============================================================================
val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        fontStyle = FontStyle.Normal
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================================================
// Expressive Shapes — soft, pillowy, dynamic
// ============================================================================
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ============================================================================
// Color Schemes
// ============================================================================
private val LightExpressiveScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surface = Surface,
    onSurface = OnSurface,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

private val DarkExpressiveScheme = darkColorScheme(
    primary = PrimaryFixedDim,
    onPrimary = OnPrimaryFixedVariant,
    primaryContainer = OnPrimaryContainer,
    onPrimaryContainer = PrimaryContainer,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondary = SecondaryFixedDim,
    onSecondary = OnSecondaryFixedVariant,
    secondaryContainer = OnSecondaryContainer,
    onSecondaryContainer = SecondaryContainer,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiary = TertiaryFixedDim,
    onTertiary = OnTertiaryFixedVariant,
    tertiaryContainer = OnTertiaryContainer,
    onTertiaryContainer = TertiaryContainer,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surface = SurfaceContainerLowest,
    onSurface = Color(0xFFE6E0E9),
    surfaceDim = Color(0xFF141218),
    surfaceBright = Color(0xFF3B383E),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ============================================================================
// WlTheme — Main entry point
// ============================================================================
@Composable
fun WlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    animateTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Animated color scheme switching
    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkExpressiveScheme
        else -> LightExpressiveScheme
    }

    val colorScheme = if (animateTheme) {
        // Animated color scheme transition
        androidx.compose.material3.ColorScheme(
            primary = animateColorAsState(targetScheme.primary, tween(600), label = "primary").value,
            onPrimary = animateColorAsState(targetScheme.onPrimary, tween(600), label = "onPrimary").value,
            primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(600), label = "primaryContainer").value,
            onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(600), label = "onPrimaryContainer").value,
            primaryFixed = animateColorAsState(targetScheme.primaryFixed, tween(600), label = "primaryFixed").value,
            primaryFixedDim = animateColorAsState(targetScheme.primaryFixedDim, tween(600), label = "primaryFixedDim").value,
            onPrimaryFixed = animateColorAsState(targetScheme.onPrimaryFixed, tween(600), label = "onPrimaryFixed").value,
            onPrimaryFixedVariant = animateColorAsState(targetScheme.onPrimaryFixedVariant, tween(600), label = "onPrimaryFixedVariant").value,
            secondary = animateColorAsState(targetScheme.secondary, tween(600), label = "secondary").value,
            onSecondary = animateColorAsState(targetScheme.onSecondary, tween(600), label = "onSecondary").value,
            secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(600), label = "secondaryContainer").value,
            onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(600), label = "onSecondaryContainer").value,
            secondaryFixed = animateColorAsState(targetScheme.secondaryFixed, tween(600), label = "secondaryFixed").value,
            secondaryFixedDim = animateColorAsState(targetScheme.secondaryFixedDim, tween(600), label = "secondaryFixedDim").value,
            onSecondaryFixed = animateColorAsState(targetScheme.onSecondaryFixed, tween(600), label = "onSecondaryFixed").value,
            onSecondaryFixedVariant = animateColorAsState(targetScheme.onSecondaryFixedVariant, tween(600), label = "onSecondaryFixedVariant").value,
            tertiary = animateColorAsState(targetScheme.tertiary, tween(600), label = "tertiary").value,
            onTertiary = animateColorAsState(targetScheme.onTertiary, tween(600), label = "onTertiary").value,
            tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, tween(600), label = "tertiaryContainer").value,
            onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, tween(600), label = "onTertiaryContainer").value,
            tertiaryFixed = animateColorAsState(targetScheme.tertiaryFixed, tween(600), label = "tertiaryFixed").value,
            tertiaryFixedDim = animateColorAsState(targetScheme.tertiaryFixedDim, tween(600), label = "tertiaryFixedDim").value,
            onTertiaryFixed = animateColorAsState(targetScheme.onTertiaryFixed, tween(600), label = "onTertiaryFixed").value,
            onTertiaryFixedVariant = animateColorAsState(targetScheme.onTertiaryFixedVariant, tween(600), label = "onTertiaryFixedVariant").value,
            error = animateColorAsState(targetScheme.error, tween(600), label = "error").value,
            onError = animateColorAsState(targetScheme.onError, tween(600), label = "onError").value,
            errorContainer = animateColorAsState(targetScheme.errorContainer, tween(600), label = "errorContainer").value,
            onErrorContainer = animateColorAsState(targetScheme.onErrorContainer, tween(600), label = "onErrorContainer").value,
            surface = animateColorAsState(targetScheme.surface, tween(600), label = "surface").value,
            onSurface = animateColorAsState(targetScheme.onSurface, tween(600), label = "onSurface").value,
            surfaceDim = animateColorAsState(targetScheme.surfaceDim, tween(600), label = "surfaceDim").value,
            surfaceBright = animateColorAsState(targetScheme.surfaceBright, tween(600), label = "surfaceBright").value,
            surfaceContainerLowest = animateColorAsState(targetScheme.surfaceContainerLowest, tween(600), label = "surfaceContainerLowest").value,
            surfaceContainerLow = animateColorAsState(targetScheme.surfaceContainerLow, tween(600), label = "surfaceContainerLow").value,
            surfaceContainer = animateColorAsState(targetScheme.surfaceContainer, tween(600), label = "surfaceContainer").value,
            surfaceContainerHigh = animateColorAsState(targetScheme.surfaceContainerHigh, tween(600), label = "surfaceContainerHigh").value,
            surfaceContainerHighest = animateColorAsState(targetScheme.surfaceContainerHighest, tween(600), label = "surfaceContainerHighest").value,
            onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(600), label = "onSurfaceVariant").value,
            outline = animateColorAsState(targetScheme.outline, tween(600), label = "outline").value,
            outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(600), label = "outlineVariant").value,
            inverseSurface = animateColorAsState(targetScheme.inverseSurface, tween(600), label = "inverseSurface").value,
            inverseOnSurface = animateColorAsState(targetScheme.inverseOnSurface, tween(600), label = "inverseOnSurface").value,
            inversePrimary = animateColorAsState(targetScheme.inversePrimary, tween(600), label = "inversePrimary").value,
            scrim = animateColorAsState(targetScheme.scrim, tween(600), label = "scrim").value
        )
    } else {
        targetScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}