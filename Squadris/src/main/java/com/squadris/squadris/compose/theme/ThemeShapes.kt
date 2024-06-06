package com.squadris.squadris.compose.theme

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/** Variables with default shapes */
interface ThemeShapes {

    /** shape for circular action */
    val circularActionShape: Shape

    /** shape for rectangular action */
    val rectangularActionShape: Shape

    /** shape for a component */
    val componentShape: Shape

    /** base component corner radius */
    val componentCornerRadius: Dp

    /** default size of a small icon */
    val iconSizeSmall: Dp

    /** default size of a medium icon */
    val iconSizeMedium: Dp

    /** Spacing between two items in a list */
    val betweenItemsSpace: Dp

    /** default size of a large icon */
    val iconSizeLarge: Dp

    /** shape of a chip component */
    val chipShape: Shape
}