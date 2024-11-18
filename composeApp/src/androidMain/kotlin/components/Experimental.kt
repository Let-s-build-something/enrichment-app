package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun Experiment(modifier: Modifier = Modifier) {
    /*val density = LocalDensity.current
    val size = remember {
        mutableStateOf(IntSize.Zero)
    }

    val items = (0..500).map { it }
    val paddingPx = with(density) { 2.dp.toPx() }

    val radius = size.value.width.toFloat() / 2
    val mappings = if(radius != 0f) {
        getMappings(radius = radius, minRadius = radius / 4, paddingPx, items = items)
    }else 0f to hashMapOf()
    val circleSize = mappings.first
    val mappedItems = mappings.second

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier
            .size(with(density) { radius.div(2).toDp() })
            .background(color = Color.Cyan, shape = CircleShape)
            .zIndex(1f)
        )
        Layout(
            modifier = Modifier
                .background(color = Color.White, shape = CircleShape)
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged {
                    size.value = it
                },
            content = {
                for(i in 0 until mappedItems.map { it.value.size }.sum()) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Blue,
                                shape = CircleShape
                            ).requiredSize(
                                with(density) { circleSize.toFloat().toDp() }
                            )
                            .zIndex(2f)
                    )
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            layout(constraints.maxWidth, constraints.minHeight) {
                var placeableIndex = 0

                mappedItems.map { it.value }.forEachIndexed { indexLayer, items ->
                    var verticalDistance = (0..mappedItems.size * 2).random() * circleSize
                    val radiusOverride = radius - (indexLayer * circleSize) - circleSize / 2

                    for(index in items.indices) {
                        val x =  (radiusOverride * cos(verticalDistance / radiusOverride)).toFloat()
                        val y = (radiusOverride * kotlin.math.sin(verticalDistance / radiusOverride)).toFloat()

                        placeables[placeableIndex].placeRelative(
                            x = x.toInt(),
                            y = y.toInt()
                        )
                        placeableIndex++
                        verticalDistance += circleSize + paddingPx
                    }
                }
            }
        }
    }*/
}
