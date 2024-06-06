package com.squadris.squadris.compose.theme

import androidx.compose.ui.graphics.Color

interface BaseColors {

    /** Main, generally more contrasting color of the app */
    val primary: Color

    /** Generally little less contrasting color than [primary] */
    val secondary: Color

    /** Constrasting color to background and appbar for actions */
    val contrastAction: Color
    val contrastActionLight: Color
    /** disabled color contrasted to background */
    val disabled: Color

    /** Brand color */
    val brandMain: Color

    /** Darker version of brand color */
    val brandMainDark: Color

    /** contrasting color to the brand color */
    val tetrial: Color

    val backgroundLight: Color
    val onBackGroundLight: Color
    val onBackgroundComponentContrast: Color
    val onBackgroundComponent: Color

    /** color of shimmer background */
    val shimmer: Color
    /** color of shimmering stripe of background */
    val overShimmer: Color
}