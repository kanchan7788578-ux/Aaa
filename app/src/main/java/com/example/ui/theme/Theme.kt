package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BengalEmeraldLight,
    onPrimary = Color(0xFF002414),
    primaryContainer = BengalEmeraldDark,
    onPrimaryContainer = Color(0xFFB5F5D4),
    secondary = HeadlightAmber,
    onSecondary = Color(0xFF281800),
    secondaryContainer = Color(0xFF472C00),
    onSecondaryContainer = Color(0xFFFFDFA6),
    tertiary = ExpressCrimson,
    onTertiary = Color.White,
    background = AsphaltBlack,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BengalEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3F5DC),
    onPrimaryContainer = Color(0xFF002112),
    secondary = HeadlightAmber,
    onSecondary = Color(0xFF281800),
    tertiary = ExpressCrimson,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MymensinghBusTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
