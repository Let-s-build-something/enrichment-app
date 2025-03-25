package ui.conversation.components.experimental.robotalk

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import korlibs.math.squared

// so that not all amplitudes affect the visualization
private const val CUT_OFF_FRACTION = 0.25f

// the faction that is considered moving head part of the visualization
private const val HEAD_FRACTION = .4f

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
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
    }

    Canvas(modifier = modifier) {
        val startingY = size.height * HEAD_FRACTION
        val newY = -size.height * amplitude.value.coerceAtMost(1f)
        val headOffset = Offset(size.width * .1f, newY)
        val headSize = Size(
            width = size.width * .8f,
            height = startingY
        )

        drawRoundRect(
            color = color,
            topLeft = headOffset,
            size = headSize,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            cornerRadius = CornerRadius(x = 10.dp.toPx(), y = 10.dp.toPx())
        )

        drawPoints(
            points = listOf(
                Offset(
                    headOffset.x + headSize.width * .35f,
                    headOffset.y + headSize.height / 2f
                ),
                Offset(
                    headOffset.x + headSize.width * .65f,
                    headOffset.y + headSize.height / 2f
                ),
            ),
            pointMode = PointMode.Points,
            color = color,
            cap = StrokeCap.Round,
            strokeWidth = 4.dp.toPx()
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, startingY),
            size = Size(
                width = size.width,
                height = size.height / 2
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            cornerRadius = CornerRadius(x = 20.dp.toPx(), y = 20.dp.toPx())
        )
    }
}
