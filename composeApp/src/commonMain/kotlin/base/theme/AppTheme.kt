package base.theme

import androidx.compose.runtime.Composable
import module.theme.BaseColors
import module.theme.BaseTheme
import module.theme.LocalAppColors
import module.theme.LocalAppIcons
import module.theme.ThemeIcons
import module.theme.ThemeStyle

/** Main theme with current colors and styles */
class AppTheme: BaseTheme {

    /** base set of colors based on configurations */
    override val colors: BaseColors
        @Composable
        get() = LocalAppColors.current

    /** base icons for the main app theme [BaseTheme.current] */
    override val icons: ThemeIcons
        @Composable
        get() = LocalAppIcons.current

    /** base styles for the main app theme [BaseTheme.current] */
    override val styles: ThemeStyle
        @Composable
        get() = AppThemeStyle()

    /** base shapes for the main app theme [BaseTheme.current] */
    override val shapes: AppThemeShapes
        @Composable
        get() = AppThemeShapes()
}