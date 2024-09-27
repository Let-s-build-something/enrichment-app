package components.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import chat.enrichment.shared.ui.theme.Colors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
private fun Preview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .width(190.dp)
                .height(70.dp)
                .background(
                    color = Colors.Asparagus,
                    shape = BackgroundEmotionShape(
                        sides = 50,
                        depth = 0.09,
                        iterations = 360
                    )
                )
                .padding(16.dp)
        )
    }
}

/**
 * Wavy rounded-all-around background which can be used to create a more emotionally charged background.
 * Known issues:
 *  - Straight lines can get longer than the rounded ones as the [depth] increases, especially when nearing .4.
 *  - It may not be ideal performance-wise due to 1) it is not composed of a single path, but of multiple ones,
 *  that are drawn over each other and 2) the waves can be made with Bézier Curve instead of straight lines, see more here https://pomax.github.io/bezierinfo.
 *
 * @param sides number of sides.
 * @param depth a double value between 0.0 - 1.0 for modifying curve depth - ideal maximum value is around .4.
 * @param iterations a value between 0 - 360 that determines the quality of the waves shape - number of polygons.
 */
class BackgroundEmotionShape(
    private val sides: Int,
    private val depth: Double = 0.09,
    iterations: Int = 360
) : Shape {

    private companion object {
        private const val TWO_PI = 2 * PI
    }

    private val steps = (TWO_PI) / min(iterations, 360)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(Path().apply {
        val theLeast = min(size.height, size.width) * .5f
        val theMost = max(size.height, size.width)

        val padding = with(density) { theLeast * .083.dp.toPx() }
        val roundedCircumference = theLeast * PI * 2
        val straightCircumference = theMost * 2 - theLeast * 4
        val totalCircumference = roundedCircumference + straightCircumference

        val blockPath = Path().apply {
            addRect(
                Rect(
                    offset = Offset(theLeast * 1.2f, 0f),
                    size = Size(theMost - theLeast * 2.4f, theLeast * 2)
                )
            )
        }

        val cornerPath = Path().apply {
            // left rounded start
            drawOnSide(
                size,
                center = Offset(
                    theLeast,
                    theLeast
                ),
                sides = ceil(sides * roundedCircumference / totalCircumference * .85f).toInt()
            )

            if(size.height != size.width) {
                // right rounded end
                drawOnSide(
                    size,
                    center = Offset(
                        max(size.height, size.width) - theLeast,
                        theLeast
                    ),
                    sides = ceil(sides * roundedCircumference / totalCircumference * .85f).toInt()
                )
            }
            op(
                blockPath,
                this,
                PathOperation.ReverseDifference
            )
        }

        val straightPath = if(size.height != size.width) {
            Path().apply {
                // top line
                drawLine(
                    size,
                    start = Offset(
                        theLeast,
                        padding
                    ),
                    end = Offset(
                        size.width - theLeast,
                        theLeast
                    ),
                    sides = ceil(sides * straightCircumference / totalCircumference / 2).toInt()
                )
                // bottom line
                drawLine(
                    size,
                    start = Offset(
                        theLeast,
                        size.height - padding
                    ),
                    end = Offset(
                        size.width - theLeast,
                        theLeast
                    ),
                    sides = ceil(sides * straightCircumference / totalCircumference / 2).toInt()
                )
            }
        }else null

        if(straightPath != null) {
            // draw everything over each-other
            op(
                cornerPath,
                straightPath,
                PathOperation.Union
            )
        }else addPath(cornerPath)
    })

    private fun Path.drawLine(
        size: Size,
        start: Offset,
        end: Offset,
        sides: Int
    ) = apply {
        moveTo(start.x, start.y)

        val amplitude = depth * min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, depth)
        val length = (end.x - start.x)
        val steps = (length / (2 * PI / sides)).toInt()
        val stepSize = length / steps

        for (i in 0..steps) {
            val t = i * stepSize
            val waveOffset = amplitude * sin((t / length) * 2 * PI * sides - PI / 2).toFloat()
            val x = start.x + t
            val y = start.y + waveOffset.times(
                // we make sure the wave starts and ends at the relative "bottom"
                if(start.y > end.y) 1 else -1
            )
            lineTo(x, y.toFloat())
        }

        lineTo(end.x, end.y)
        lineTo(start.x, end.y)
    }

    private fun Path.drawOnSide(
        size: Size,
        center: Offset,
        sides: Int
    ) = apply {
        val r = min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, depth)

        moveTo(x = center.x, y = center.y)

        var t = 0.0

        while (t <= TWO_PI) {
            val x = r * (cos(t) * (1 + depth * cos(sides * t)))
            val y = r * (sin(t) * (1 + depth * cos(sides * t)))
            lineTo((x + center.x).toFloat(), (y + center.y).toFloat())

            t += steps
        }

        val x = r * (cos(t) * (1 + depth * cos(sides * t)))
        val y = r * (sin(t) * (1 + depth * cos(sides * t)))
        lineTo((x + center.x).toFloat(), (y + center.y).toFloat())
    }

    private fun mapRange(a: Double, b: Double, c: Double, d: Double, x: Double): Double {
        return (x - a) / (b - a) * (d - c) + c
    }
}