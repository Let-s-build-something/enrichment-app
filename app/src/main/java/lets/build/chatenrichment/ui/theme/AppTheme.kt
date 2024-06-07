package lets.build.chatenrichment.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.squadris.squadris.compose.theme.BaseColors
import com.squadris.squadris.compose.theme.BaseTheme
import com.squadris.squadris.compose.theme.LocalAppIcons
import com.squadris.squadris.compose.theme.ThemeIcons
import com.squadris.squadris.compose.theme.ThemeShapes
import com.squadris.squadris.compose.theme.ThemeStyle
import lets.build.chatenrichment.ui.theme.DefaultValues.defaultBaseColors
import lets.build.chatenrichment.ui.theme.DefaultValues.defaultThemeIcons

/** Base theme for this app */
object AppTheme: BaseTheme {

    override val colors: BaseColors
        @Composable
        get() = LocalAppColors.current

    override val styles: ThemeStyle = AppThemeStyle

    override val icons: ThemeIcons
        @Composable
        get() = LocalAppIcons.current

    override val shapes: ThemeShapes = AppThemeShapes
}

/** current set of colors */
val LocalAppColors = staticCompositionLocalOf {
    defaultBaseColors
}

/** current set of colors */
val LocalAppIcons = staticCompositionLocalOf {
    defaultThemeIcons
}

private object DefaultValues {
    val defaultThemeIcons = object: ThemeIcons {
        override val googleSignUp: Int = -1
    }
    val defaultBaseColors = object: BaseColors {
        override val primary: Color = Color(0x00000000)
        override val secondary: Color = Color(0x00000000)
        override val contrastAction: Color = Color(0x00000000)
        override val contrastActionLight: Color = Color(0x00000000)
        override val disabled: Color = Color(0x00000000)
        override val brandMain: Color = Color(0x00000000)
        override val brandMainDark: Color = Color(0x00000000)
        override val tetrial: Color = Color(0x00000000)
        override val backgroundLight: Color = Color(0x00000000)
        override val onBackGroundLight: Color = Color(0x00000000)
        override val onBackgroundComponentContrast: Color = Color(0x00000000)
        override val onBackgroundComponent: Color = Color(0x00000000)
        override val shimmer: Color = Color(0x00000000)
        override val overShimmer: Color = Color(0x00000000)
    }
}