package base.theme

import androidx.compose.ui.graphics.Color
import future_shared_module.theme.BaseColors
import future_shared_module.theme.Colors

object DarkAppColors: BaseColors {

    override val primary: Color = Color.White
    override val secondary: Color = Colors.GrayLight
    override val disabled: Color = Colors.White7

    override val brandMain: Color = Colors.Asparagus

    override val brandMainDark: Color = Colors.HunterGreen

    override val tetrial: Color = Colors.Flax

    override val backgroundLight: Color = Colors.Night

    override val backgroundDark: Color = Colors.EerieBlack

    override val shimmer: Color = Colors.HunterGreen16
    override val overShimmer: Color = Colors.HunterGreen42
}