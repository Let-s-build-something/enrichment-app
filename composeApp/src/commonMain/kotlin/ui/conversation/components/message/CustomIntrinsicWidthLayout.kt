package ui.conversation.components.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable

@Composable
fun CustomIntrinsicWidthLayout(
    modifier: Modifier = Modifier,
    hasAttachment: Boolean,
    alignment: Alignment.Horizontal,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = mutableListOf<Placeable>()
        val layoutWidth: Int

        if (hasAttachment && measurables.isNotEmpty()) {
            // When hasAttachment, use the first child's intrinsic width
            val firstChildMeasurable = measurables[0]
            val firstChildPlaceable = firstChildMeasurable.measure(
                constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
            )
            val minWidth = firstChildPlaceable.width.coerceAtMost(constraints.maxWidth)
            placeables.add(firstChildPlaceable)

            // Measure remaining children with the first child's width as maxWidth
            for (i in 1 until measurables.size) {
                val placeable = measurables[i].measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = minWidth
                    )
                )
                placeables.add(placeable)
            }

            // Layout width is the first child's width, respecting constraints
            layoutWidth = minWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        } else {
            // When no attachment, wrap content (use max of children's measured widths)
            var maxChildWidth = 0
            for (measurable in measurables) {
                val placeable = measurable.measure(
                    constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
                )
                placeables.add(placeable)
                maxChildWidth = maxOf(maxChildWidth, placeable.width)
            }
            layoutWidth = maxChildWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

        layout(layoutWidth, placeables.sumOf { it.height }) {
            var yOffset = 0
            for (placeable in placeables) {
                val xOffset = when (alignment) {
                    Alignment.End -> layoutWidth - placeable.width
                    else -> 0
                }
                placeable.placeRelative(x = xOffset, y = yOffset)
                yOffset += placeable.height
            }
        }
    }
}