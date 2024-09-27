package components.draw

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize


fun Modifier.drawEmotionalBackground(
    salience: Float,
    frequency: Float
) = composed {
    val modifierSize = remember {
        mutableStateOf(IntSize.Zero)
    }

    onGloballyPositioned {
        modifierSize.value = it.size
    }.drawWithCache {
        val height = size.height
        val waveLength = height / wavesCount
        val nextPointOffset = waveLength / 2f
        val controlPointOffset = nextPointOffset / 2f
        val amplitude = amplitudeProvider(size)
        val wavePath = Path()

        onDrawWithContent {
            // We'll construct the wave path next.
            ...

            drawPath(
                path = wavePath,
                brush = Gradient,
                blendMode = BlendMode.SrcAtop
            )
        }
    }

}