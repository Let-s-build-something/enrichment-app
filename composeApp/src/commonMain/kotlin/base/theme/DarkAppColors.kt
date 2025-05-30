package base.theme

import androidx.compose.ui.graphics.Color
import augmy.interactive.shared.ui.theme.BaseColors

object DarkAppColors: BaseColors {

    override val primary: Color = Color.White
    override val secondary: Color = Colors.GrayLight
    override val tetrial: Color = Colors.DutchWhite
    override val brandMain: Color = Colors.Asparagus
    override val brandMainDark: Color = Colors.HunterGreen

    override val backgroundLight: Color = Colors.Night
    override val backgroundDark: Color = Colors.EerieBlack
    override val backgroundContrast: Color = Colors.EerieBlackLight
    override val component: Color = Colors.Onyx

    override val appbarBackground: Color = backgroundContrast
    override val appbarContent: Color = secondary
    override val disabled: Color = secondary.copy(alpha = .6f)
    override val disabledComponent: Color = Colors.White7

    override val shimmer: Color = secondary.copy(.16f)
    override val overShimmer: Color = secondary.copy(.42f)
}