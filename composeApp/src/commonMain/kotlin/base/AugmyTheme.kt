package base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import augmy.interactive.shared.ui.theme.LocalThemeColors
import augmy.interactive.shared.ui.theme.LocalThemeIcons
import augmy.interactive.shared.ui.theme.LocalThemeShapes
import augmy.interactive.shared.ui.theme.LocalThemeStyles
import augmy.interactive.shared.ui.theme.SharedColors
import base.theme.AppThemeIconsDark
import base.theme.AppThemeIconsLight
import base.theme.AppThemeShapes
import base.theme.DarkAppColors
import base.theme.DarkThemeStyles
import base.theme.DefaultThemeStyles
import base.theme.LightAppColors

/** Theme with dynamic resources */
@Composable
fun AugmyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if(isDarkTheme) DarkAppColors else LightAppColors
    val styles = if(isDarkTheme) DarkThemeStyles() else DefaultThemeStyles()
    val icons = if(isDarkTheme) AppThemeIconsDark else AppThemeIconsLight

    CompositionLocalProvider(
        LocalThemeColors provides colors,
        LocalThemeIcons provides icons,
        LocalThemeStyles provides styles,
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
                scrim = Color.Transparent,
                surfaceBright = colors.backgroundLight,
                surfaceDim = colors.backgroundDark,
                surfaceContainer = colors.backgroundLight,
                surfaceContainerHigh = colors.backgroundLight,
                surfaceContainerHighest = colors.backgroundDark,
                surfaceContainerLow = colors.backgroundLight,
                surfaceContainerLowest = colors.backgroundLight,
            ),
            typography = Typography(
                bodyLarge = styles.regular,
                bodySmall = styles.regular,
                bodyMedium = styles.regular,
                titleLarge = styles.regular,
                titleSmall = styles.title,
                titleMedium = styles.title,
                headlineMedium = styles.heading,
                headlineSmall = styles.heading,
                headlineLarge = styles.heading,
                labelLarge = styles.category,
                labelSmall = styles.category,
                labelMedium = styles.category
            ),
            content = content
        )
    }
}