package ui.conversation.components.experimental.robotalk

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import korlibs.math.squared
import kotlin.math.sqrt

// so that not all amplitudes affect the visualization
private const val CUT_OFF_FRACTION = 0.3f

// the faction that is considered moving head part of the visualization
private const val HEAD_FRACTION = .5f

@Composable
fun RobotalkVisualization(
    modifier: Modifier,
    color: Color = LocalTheme.current.colors.secondary,
    amplitudes: List<Pair<Double, String>>,
    median: Double
) {
    val amplitude = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(amplitudes) {
        amplitude.animateTo(
            targetValue = amplitudes.lastOrNull()?.first?.div(median)?.let { fraction ->
                fraction.takeIf { it > CUT_OFF_FRACTION }
                    ?.times(HEAD_FRACTION.squared())
            }?.toFloat() ?: 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Canvas(modifier = modifier) {
        val startingY = size.height * HEAD_FRACTION
        val newY = -size.height * amplitude.value.coerceAtMost(1f)
        val newX = sqrt(size.width.squared() - newY.squared())

        Path().apply {
            moveTo(0f, startingY)

            lineTo(
                x = newX,
                y = newY + startingY
            )
            arcTo(
                rect = Rect(
                    topLeft = Offset(0f, 5f),
                    bottomRight = Offset(size.width, newY + startingY.times(1.5f))
                ),
                startAngleDegrees = -180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
        }.let {
            drawPath(
                path = it,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
