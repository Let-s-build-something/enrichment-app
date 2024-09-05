package base.theme

import androidx.compose.runtime.Composable
import future_shared_module.theme.BaseColors
import future_shared_module.theme.BaseTheme
import future_shared_module.theme.LocalAppColors
import future_shared_module.theme.LocalAppIcons
import future_shared_module.theme.ThemeIcons
import future_shared_module.theme.ThemeStyle

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