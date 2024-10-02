package base.theme

import androidx.compose.runtime.Composable
import augmy.interactive.shared.ui.theme.BaseColors
import augmy.interactive.shared.ui.theme.BaseTheme
import augmy.interactive.shared.ui.theme.LocalThemeColors
import augmy.interactive.shared.ui.theme.LocalThemeIcons
import augmy.interactive.shared.ui.theme.LocalThemeStyles
import augmy.interactive.shared.ui.theme.ThemeIcons
import augmy.interactive.shared.ui.theme.ThemeStyle

/** Main theme with current colors and styles */
class AppTheme: BaseTheme {

    /** base set of colors based on configurations */
    override val colors: BaseColors
        @Composable
        get() = LocalThemeColors.current

    /** base icons for the main app theme [BaseTheme.current] */
    override val icons: ThemeIcons
        @Composable
        get() = LocalThemeIcons.current

    /** base styles for the main app theme [BaseTheme.current] */
    override val styles: ThemeStyle
        @Composable
        get() = LocalThemeStyles.current

    /** base shapes for the main app theme [BaseTheme.current] */
    override val shapes: AppThemeShapes
        @Composable
        get() = AppThemeShapes()
}