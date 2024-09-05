package future_shared_module.theme

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

    /** default size of a large icon */
    val iconSizeLarge: Dp

    /** Spacing between two items in a list */
    val betweenItemsSpace: Dp

    /** shape of a chip component */
    val chipShape: Shape
}