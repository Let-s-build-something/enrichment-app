package lets.build.chatenrichment.ui.theme

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
import com.squadris.squadris.compose.theme.LocalAppColors
import com.squadris.squadris.compose.theme.LocalAppIcons
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.compose.theme.LocalThemeShapes
import com.squadris.squadris.compose.theme.LocalThemeStyle
import com.squadris.squadris.compose.theme.SharedColors

/** Theme with dynamic resources */
@Composable
fun ChatEnrichmentTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    //dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    /*dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }*/

    val colors = if(isDarkTheme) DarkAppColors else LightAppColors
    val icons = if(isDarkTheme) AppThemeIconsDark else AppThemeIconsLight

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppIcons provides icons,
        LocalThemeStyle provides AppThemeStyle,
        LocalThemeShapes provides AppThemeShapes
    ) {
        MaterialTheme(
            colorScheme = ColorScheme(
                primary = colors.brandMain,
                onPrimary = colors.tetrial,
                primaryContainer = colors.backgroundLight,
                onPrimaryContainer = colors.onBackgroundComponent,
                inversePrimary = colors.tetrial,
                secondary = colors.brandMainDark,
                onSecondary = colors.tetrial,
                secondaryContainer = colors.brandMainDark,
                onSecondaryContainer = colors.primary,
                tertiary = Color.Transparent,
                onTertiary = Color.Transparent,
                tertiaryContainer = Color.Transparent,
                onTertiaryContainer = Color.Transparent,
                background = colors.onBackgroundComponent,
                onBackground = colors.onBackgroundComponentContrast,
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
                /* Other default text styles to override
                titleLarge = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 0.sp
                ),
                labelSmall = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.5.sp
                )
                */
            ),
            content = content
        )
    }
}