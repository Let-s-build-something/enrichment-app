package base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import base.theme.AppThemeShapes
import base.theme.AppThemeStyle
import base.theme.AppThemeIconsDark
import base.theme.AppThemeIconsLight
import base.theme.DarkAppColors
import base.theme.LightAppColors
import module.theme.LocalAppColors
import module.theme.LocalAppIcons
import module.theme.LocalThemeShapes
import module.theme.LocalThemeStyle
import module.theme.SharedColors

/** Theme with dynamic resources */
@Composable
fun ChatEnrichmentTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if(isDarkTheme) DarkAppColors else LightAppColors
    val icons = if(isDarkTheme) AppThemeIconsDark else AppThemeIconsLight

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppIcons provides icons,
        LocalThemeStyle provides AppThemeStyle(),
        LocalThemeShapes provides AppThemeShapes()
    ) {
        MaterialTheme(
            colorScheme = ColorScheme(
                primary = colors.brandMain,
                onPrimary = colors.tetrial,
                primaryContainer = colors.backgroundLight,
                onPrimaryContainer = colors.backgroundDark,
                inversePrimary = colors.tetrial,
                secondary = colors.brandMainDark,
                onSecondary = colors.tetrial,
                secondaryContainer = colors.brandMainDark,
                onSecondaryContainer = colors.primary,
                tertiary = Color.Transparent,
                onTertiary = Color.Transparent,
                tertiaryContainer = Color.Transparent,
                onTertiaryContainer = Color.Transparent,
                background = colors.backgroundLight,
                onBackground = colors.backgroundDark,
                surface = colors.backgroundLight,
                onSurface = colors.primary,
                surfaceVariant = Color.Transparent,
                onSurfaceVariant = Color.Transparent,
                surfaceTint = Color.Transparent,
                inverseSurface = Color.Transparent,
                inverseOnSurface = Color.Transparent,
                error = SharedColors.RED_ERROR,
                onError = Color.White,
                errorContainer = SharedColors.RED_ERROR_50,
                onErrorContainer = SharedColors.RED_ERROR,
                outline = Color.Transparent,
                outlineVariant = Color.Transparent,
                scrim = Color.Transparent
            ),
            typography = Typography(
                bodyLarge = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                )
            ),
            content = content
        )
    }
}