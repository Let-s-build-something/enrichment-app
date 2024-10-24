package augmy.interactive.shared.ui.theme

import androidx.compose.ui.graphics.Color

interface BaseColors {

    /** Brand color */
    val brandMain: Color

    /** Darker version of brand color */
    val brandMainDark: Color

    /** contrasting color to the brand color */
    val tetrial: Color

    /** Contrast to background color, used for darker text */
    val primary: Color

    /** Contrast to background color, used for lighter or regular text */
    val secondary: Color

    /** disabled color with any background */
    val disabled: Color

    /** disabled color contrasted to background */
    val disabledComponent: Color

    /** color of shimmer background */
    val shimmer: Color

    /** color of shimmering stripe of background */
    val overShimmer: Color

    /** lighter background */
    val backgroundLight: Color

    /** darker background */
    val backgroundDark: Color
}