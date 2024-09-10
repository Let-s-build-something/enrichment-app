package base.theme

import androidx.compose.runtime.Composable
import chat.enrichment.shared.ui.theme.BaseColors
import chat.enrichment.shared.ui.theme.BaseTheme
import chat.enrichment.shared.ui.theme.LocalAppColors
import chat.enrichment.shared.ui.theme.LocalAppIcons
import chat.enrichment.shared.ui.theme.ThemeIcons
import chat.enrichment.shared.ui.theme.ThemeStyle

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