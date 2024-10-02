package base.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.theme.LocalTheme

/** Styles specific to main app theme in dark theme */
class DarkThemeStyles: DefaultThemeStyles() {
    override val heading: TextStyle
        @Composable
        get() = TextStyle(
            color = LocalTheme.current.colors.primary,
            fontSize = 28.sp,
            fontFamily = FontFamily(fontQuicksandMedium)
        )

    override val subheading: TextStyle
        @Composable
        get() = TextStyle(
            color = LocalTheme.current.colors.primary,
            fontSize = 22.sp,
            fontFamily = FontFamily(fontQuicksandMedium)
        )

    override val category: TextStyle
        @Composable
        get() = TextStyle(
            color = LocalTheme.current.colors.secondary,
            fontSize = 16.sp,
            fontFamily = FontFamily(fontQuicksandMedium)
        )

    override val title: TextStyle
        @Composable
        get() = TextStyle(
            color = LocalTheme.current.colors.primary,
            fontSize = 16.sp,
            fontFamily = FontFamily(fontQuicksandRegular)
        )

    override val regular: TextStyle
        @Composable
        get() = TextStyle(
            color = LocalTheme.current.colors.secondary,
            fontSize = 12.sp,
            fontFamily = FontFamily(fontQuicksandRegular)
        )
}