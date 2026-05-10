package com.example.wl.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val ExpressiveTypography = Typography(
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
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

val ExpressiveShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val LightExpressiveScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceTint = Primary,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

private val DarkExpressiveScheme = darkColorScheme(
    primary = PrimaryFixedDim,
    onPrimary = OnPrimaryFixedVariant,
    primaryContainer = OnPrimaryContainer,
    onPrimaryContainer = PrimaryContainer,
    secondary = SecondaryFixedDim,
    onSecondary = OnSecondaryFixedVariant,
    secondaryContainer = OnSecondaryContainer,
    onSecondaryContainer = SecondaryContainer,
    tertiary = TertiaryFixedDim,
    onTertiary = OnTertiaryFixedVariant,
    tertiaryContainer = OnTertiaryContainer,
    onTertiaryContainer = TertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Color(0xFF0F0D13),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF0F0D13),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    surfaceTint = PrimaryFixedDim,
    surfaceBright = Color(0xFF141218),
    surfaceDim = Color(0xFF0F0D13),
    surfaceContainerLowest = Color(0xFF0A080E),
    surfaceContainerLow = Color(0xFF1D1B22),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)


// --- Extra Colors Extension ---
data class WlExtraColors(
    val glowPrimary: Color = GlowPrimary,
    val glowSecondary: Color = GlowSecondary,
    val glowTertiary: Color = GlowTertiary,
    val gradientStart: Color = GradientStart,
    val gradientMid: Color = GradientMid,
    val gradientEnd: Color = GradientEnd,
    val statusConnected: Color = StatusConnected,
    val statusConnecting: Color = StatusConnecting,
    val statusDisconnected: Color = StatusDisconnected,
    val latencyFast: Color = LatencyFast,
    val latencyMedium: Color = LatencyMedium,
    val latencySlow: Color = LatencySlow,
    val latencyTimeout: Color = LatencyTimeout
)

val LocalWlColors = staticCompositionLocalOf { WlExtraColors() }

object WlTheme {
    val colors: WlExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWlColors.current
}

@Composable
fun WlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    animateTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkExpressiveScheme
        else -> LightExpressiveScheme
    }

    val colorScheme = if (animateTheme) {
        ColorScheme(
            primary = animateColorAsState(targetScheme.primary, tween(600), label = "primary").value,
            onPrimary = animateColorAsState(targetScheme.onPrimary, tween(600), label = "onPrimary").value,
            primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(600), label = "primaryContainer").value,
            onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(600), label = "onPrimaryContainer").value,
            inversePrimary = animateColorAsState(targetScheme.inversePrimary, tween(600), label = "inversePrimary").value,
            secondary = animateColorAsState(targetScheme.secondary, tween(600), label = "secondary").value,
            onSecondary = animateColorAsState(targetScheme.onSecondary, tween(600), label = "onSecondary").value,
            secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(600), label = "secondaryContainer").value,
            onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(600), label = "onSecondaryContainer").value,
            tertiary = animateColorAsState(targetScheme.tertiary, tween(600), label = "tertiary").value,
            onTertiary = animateColorAsState(targetScheme.onTertiary, tween(600), label = "onTertiary").value,
            tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, tween(600), label = "tertiaryContainer").value,
            onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, tween(600), label = "onTertiaryContainer").value,
            background = animateColorAsState(targetScheme.background, tween(600), label = "background").value,
            onBackground = animateColorAsState(targetScheme.onBackground, tween(600), label = "onBackground").value,
            surface = animateColorAsState(targetScheme.surface, tween(600), label = "surface").value,
            onSurface = animateColorAsState(targetScheme.onSurface, tween(600), label = "onSurface").value,
            surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, tween(600), label = "surfaceVariant").value,
            onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(600), label = "onSurfaceVariant").value,
            surfaceTint = animateColorAsState(targetScheme.surfaceTint, tween(600), label = "surfaceTint").value,
            inverseSurface = animateColorAsState(targetScheme.inverseSurface, tween(600), label = "inverseSurface").value,
            inverseOnSurface = animateColorAsState(targetScheme.inverseOnSurface, tween(600), label = "inverseOnSurface").value,
            error = animateColorAsState(targetScheme.error, tween(600), label = "error").value,
            onError = animateColorAsState(targetScheme.onError, tween(600), label = "onError").value,
            errorContainer = animateColorAsState(targetScheme.errorContainer, tween(600), label = "errorContainer").value,
            onErrorContainer = animateColorAsState(targetScheme.onErrorContainer, tween(600), label = "onErrorContainer").value,
            outline = animateColorAsState(targetScheme.outline, tween(600), label = "outline").value,
            outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(600), label = "outlineVariant").value,
            scrim = animateColorAsState(targetScheme.scrim, tween(600), label = "scrim").value,
            surfaceBright = animateColorAsState(targetScheme.surfaceBright, tween(600), label = "surfaceBright").value,
            surfaceDim = animateColorAsState(targetScheme.surfaceDim, tween(600), label = "surfaceDim").value,
            surfaceContainerLowest = animateColorAsState(targetScheme.surfaceContainerLowest, tween(600), label = "surfaceContainerLowest").value,
            surfaceContainerLow = animateColorAsState(targetScheme.surfaceContainerLow, tween(600), label = "surfaceContainerLow").value,
            surfaceContainer = animateColorAsState(targetScheme.surfaceContainer, tween(600), label = "surfaceContainer").value,
            surfaceContainerHigh = animateColorAsState(targetScheme.surfaceContainerHigh, tween(600), label = "surfaceContainerHigh").value,
            surfaceContainerHighest = animateColorAsState(targetScheme.surfaceContainerHighest, tween(600), label = "surfaceContainerHighest").value
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

    CompositionLocalProvider(LocalWlColors provides WlExtraColors()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ExpressiveTypography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
