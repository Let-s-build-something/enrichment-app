package ui.conversation.components.message

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

enum class Direction {
    Top, Bottom, Both
}

@Composable
fun WaveLine(
    modifier: Modifier = Modifier,
    waveHeights: List<Float>,
    waveColor: Color = Color.LightGray,
    animationDuration: Int = 500,
    animationSpec: AnimationSpec<Float> = tween(
        durationMillis = animationDuration,
        easing = FastOutSlowInEasing
    ),
    direction: Direction = Direction.Bottom
) {
    val waveAnimations = remember {
        mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }

    val animationProgresses = remember {
        waveHeights.map {
            mutableStateOf((0..999).random().div(1000.0).toFloat())
        }
    }

    LaunchedEffect(waveHeights) {
        waveHeights.forEachIndexed { index, newHeight ->
            waveAnimations[index]?.animateTo(
                targetValue = newHeight,
                animationSpec = animationSpec
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            animationProgresses.forEach { state ->
                state.value = (state.value + 0.01f) % 1f
            }
            delay((animationDuration / 100).toLong())
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = 0f
        val waveSegmentWidth = width / (waveHeights.lastIndex).coerceAtLeast(1)

        val path = Path().apply {
            moveTo(0f, centerY)

            for (i in waveHeights.indices) {
                val animationProgress = animationProgresses[i].value

                val animatedWaveHeight = waveAnimations[i]?.value ?: waveHeights[i]
                val startX = i * waveSegmentWidth
                val endX = (i + 1) * waveSegmentWidth

                val sineValue = sin(animationProgress * 2 * PI).toFloat()
                val startY = when (direction) {
                    Direction.Top -> centerY - animatedWaveHeight * height * (0.5f + 0.5f * sineValue)
                    Direction.Bottom -> centerY + animatedWaveHeight * height * (0.5f + 0.5f * sineValue)
                    Direction.Both -> centerY - animatedWaveHeight * (height / 2) * sineValue
                }

                val endY = if (i < waveHeights.lastIndex) {
                    val nextSineValue = sin(animationProgresses[i + 1].value * 2 * PI).toFloat()
                    when (direction) {
                        Direction.Top -> centerY - waveHeights[i + 1] * height * (0.5f + 0.5f * nextSineValue)
                        Direction.Bottom -> centerY + waveHeights[i + 1] * height * (0.5f + 0.5f * nextSineValue)
                        Direction.Both -> centerY - waveHeights[i + 1] * (height / 2) * nextSineValue
                    }
                } else startY

                val control1X = startX + waveSegmentWidth * 0.5f
                val control2X = endX - waveSegmentWidth * 0.5f

                cubicTo(
                    control1X, startY,
                    control2X, endY,
                    endX, endY
                )
            }
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

